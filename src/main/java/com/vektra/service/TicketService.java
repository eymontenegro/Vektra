package com.vektra.service;
import com.vektra.dao.PassengerDao;
import com.vektra.dao.TicketDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.LineColor;
import com.vektra.model.Passenger;
import com.vektra.model.Route;
import com.vektra.model.Station;
import com.vektra.model.Ticket;
import com.vektra.security.AccessControl;
import com.vektra.security.SessionUser;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
/**
 * Reglas de negocio del ciclo del ticket: compra (con precio calculado aquí),
 * guardado, envío por correo y consulta de los tickets de un pasajero.
 * <p>
 * Cada operación recibe quién la pide (SessionUser) y comprueba que sea un
 * pasajero. Ya no depende de la sesión estática UserSession.
 */
public class TicketService {
    /** Tarifa única por trayecto, en pesos colombianos. */
    private static final BigDecimal BASE_FARE = new BigDecimal("2500");
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final Logger LOGGER = Logger.getLogger(TicketService.class.getName());
    private final TicketDao ticketDao;
    private final PassengerDao passengerDao;
    private final RouteService routeService;
    private final EmailService emailService;
    /**
     * Resultado de una compra: el ticket guardado y si el correo se pudo enviar.
     * Si el correo falla, el ticket sigue siendo válido.
     *
     * @param ticket    ticket guardado
     * @param emailSent true si el correo salió
     */
    public record PurchaseResult(Ticket ticket, boolean emailSent) { }
    /**
     * Ticket con la línea de la ruta que une sus estaciones, para "Mis tickets".
     *
     * @param ticket ticket comprado
     * @param line   línea de la ruta directa, o null si no hay (tickets comprados
     *               antes de la regla de ruta directa, o una ruta que ya no existe)
     */
    public record PassengerTicket(Ticket ticket, LineColor line) { }
    /**
     * @param ticketDao    acceso a datos de tickets
     * @param passengerDao acceso a datos de pasajeros, para cargar al comprador
     * @param routeService estaciones y rutas
     * @param emailService envío del ticket por correo
     */
    public TicketService(TicketDao ticketDao, PassengerDao passengerDao,
                         RouteService routeService, EmailService emailService) {
        this.ticketDao = ticketDao;
        this.passengerDao = passengerDao;
        this.routeService = routeService;
        this.emailService = emailService;
    }
    /** @return precio de un trayecto, para mostrarlo antes de comprar */
    public BigDecimal getFare() {
        return calculatePrice();
    }
    /**
     * Compra un ticket para el pasajero de la sesión. Guarda primero y envía
     * el correo solo si el guardado fue exitoso.
     *
     * @param actor         usuario de la sesión, debe ser pasajero
     * @param originId      id de la estación de origen
     * @param destinationId id de la estación de destino
     * @return el ticket guardado y si el correo se envió
     * @throws com.vektra.exception.AuthorizationException si no es pasajero
     * @throws ValidationException si las estaciones no son válidas o no hay ruta directa entre ellas
     * @throws DataAccessException si falla la base de datos
     */
    public PurchaseResult buyTicket(SessionUser actor, int originId, int destinationId) {
        AccessControl.requirePassenger(actor);
        if (originId == destinationId) {
            throw new ValidationException("El origen y el destino deben ser estaciones distintas.");
        }
        Map<Integer, Station> stations = routeService.getStations().stream()
                .collect(Collectors.toMap(Station::getId, Function.identity()));
        Station origin = stations.get(originId);
        Station destination = stations.get(destinationId);
        if (origin == null || destination == null) {
            throw new ValidationException("Selecciona la estación de origen y la de destino.");
        }
        // Regla decidida por el equipo: solo se vende un ticket si existe una
        // ruta directa entre las dos estaciones (en cualquier sentido).
        if (RouteService.findRoute(routeService.getRoutes(), originId, destinationId).isEmpty()) {
            throw new ValidationException(
                    "No hay una ruta directa entre estas estaciones. Elige otro origen o destino.");
        }
        Passenger passenger = loadPassenger(actor.id());
        Ticket ticket = new Ticket(0, generateCode(), LocalDateTime.now(), calculatePrice(),
                passenger, origin, destination);
        try {
            if (!ticketDao.saveTicket(ticket)) {
                throw new DataAccessException("No se pudo guardar el ticket. Intenta de nuevo.", null);
            }
        } catch (SQLException e) {
            if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
                throw new ValidationException("La estación o el pasajero seleccionado ya no existe.", e);
            }
            LOGGER.log(Level.SEVERE, "Fallo de base de datos al guardar el ticket", e);
            throw new DataAccessException("No se pudo conectar con la base de datos. Intenta de nuevo.", e);
        }
        return new PurchaseResult(ticket, emailService.send(ticket));
    }
    /**
     * Devuelve los tickets del pasajero de la sesión, del más reciente al
     * más antiguo, cada uno con la línea de su ruta directa (si existe).
     *
     * @param actor usuario de la sesión, debe ser pasajero
     * @return tickets del pasajero
     * @throws com.vektra.exception.AuthorizationException si no es pasajero
     * @throws DataAccessException si falla la base de datos
     */
    public List<PassengerTicket> getPassengerTickets(SessionUser actor) {
        AccessControl.requirePassenger(actor);
        Passenger passenger = loadPassenger(actor.id());
        List<Ticket> tickets;
        try {
            tickets = ticketDao.findByPassenger(passenger);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de base de datos al consultar tickets", e);
            throw new DataAccessException("No se pudieron cargar tus tickets. Intenta de nuevo.", e);
        }
        // Las rutas se cargan una sola vez para todos los tickets.
        List<Route> routes = routeService.getRoutes();
        List<PassengerTicket> result = new ArrayList<>();
        for (Ticket ticket : tickets) {
            LineColor line = RouteService.findRoute(routes, ticket.getOriginId(), ticket.getDestinationId())
                    .map(Route::getLineColor)
                    .orElse(null);
            result.add(new PassengerTicket(ticket, line));
        }
        return result;
    }
    /** Carga los datos completos del pasajero que tiene la sesión. */
    private Passenger loadPassenger(int passengerId) {
        try {
            Passenger passenger = passengerDao.findById(passengerId);
            if (passenger == null) {
                throw new NotFoundException("Tu cuenta ya no existe. Inicia sesión de nuevo.");
            }
            return passenger;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de base de datos al cargar el pasajero", e);
            throw new DataAccessException("No se pudo conectar con la base de datos. Intenta de nuevo.", e);
        }
    }
    /** Hoy la tarifa es única; si cambia la regla, se cambia solo aquí. */
    private BigDecimal calculatePrice() {
        return BASE_FARE;
    }
    /** Código corto y difícil de adivinar, por ejemplo VK-3F9A1C2B7D. */
    private String generateCode() {
        return "VK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}