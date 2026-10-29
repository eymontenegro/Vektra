package com.vektra.service;

import com.vektra.dao.TicketDao;
import com.vektra.model.Passenger;
import com.vektra.model.Station;
import com.vektra.model.Ticket;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reglas de negocio del ciclo del ticket: compra (con precio calculado aquí),
 * guardado, envío por correo y consulta de los tickets de un pasajero.
 * <p>
 * Las operaciones son lentas (base de datos y correo): la vista debe llamarlas
 * desde un {@code SwingWorker}, no desde el hilo de la interfaz.
 */
public class TicketService {

    /** Tarifa única por trayecto, en pesos colombianos. */
    private static final BigDecimal BASE_FARE = new BigDecimal("2500");
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final Logger LOGGER = Logger.getLogger(TicketService.class.getName());

    private final TicketDao ticketDao;
    private final EmailService emailService;

    /**
     * Resultado de una compra: el ticket guardado y si el correo se pudo enviar.
     * Si el correo falla, el ticket sigue siendo válido.
     */
    public record PurchaseResult(Ticket ticket, boolean emailSent) { }

    /**
     * @param ticketDao    acceso a datos de tickets
     * @param emailService envío del ticket por correo
     */
    public TicketService(TicketDao ticketDao, EmailService emailService) {
        this.ticketDao = ticketDao;
        this.emailService = emailService;
    }

    /**
     * Compra un ticket para el pasajero indicado. Guarda primero y envía el
     * correo solo si el guardado fue exitoso.
     *
     * @throws IllegalArgumentException si falta el pasajero o una estación, o si son la misma
     * @throws IllegalStateException    si falla la base de datos
     */
    public PurchaseResult buyTicket(Passenger passenger, Station origin, Station destination) {
        validatePurchase(passenger, origin, destination);

        Ticket ticket = new Ticket(0, generateCode(), LocalDateTime.now(), calculatePrice(),
                passenger, origin, destination);
        try {
            if (!ticketDao.saveTicket(ticket)) {
                throw new IllegalStateException("No se pudo guardar el ticket. Intenta de nuevo.");
            }
        } catch (SQLException e) {
            if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
                throw new IllegalArgumentException("La estación o el pasajero seleccionado ya no existe.");
            }
            LOGGER.log(Level.SEVERE, "Fallo de base de datos al guardar el ticket", e);
            throw new IllegalStateException("No se pudo conectar con la base de datos. Intenta de nuevo.", e);
        }
        return new PurchaseResult(ticket, emailService.send(ticket));
    }

    /**
     * Devuelve los tickets del pasajero indicado, del más reciente al más antiguo.
     *
     * @throws IllegalStateException si falla la base de datos
     */
    public List<Ticket> getPassengerTickets(Passenger passenger) {
        if (passenger == null) {
            throw new IllegalArgumentException("Debes iniciar sesión para ver tus tickets.");
        }
        try {
            return ticketDao.findByPassenger(passenger);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de base de datos al consultar tickets", e);
            throw new IllegalStateException("No se pudieron cargar tus tickets. Intenta de nuevo.", e);
        }
    }

    private void validatePurchase(Passenger passenger, Station origin, Station destination) {
        if (passenger == null) {
            throw new IllegalArgumentException("Debes iniciar sesión como pasajero para comprar.");
        }
        if (origin == null || destination == null) {
            throw new IllegalArgumentException("Selecciona la estación de origen y la de destino.");
        }
        if (origin.getId() == destination.getId()) {
            throw new IllegalArgumentException("El origen y el destino deben ser estaciones distintas.");
        }
    }

    /** Hoy la tarifa es única; si cambia la regla, se cambia solo aquí. */
    private BigDecimal calculatePrice() {
        return BASE_FARE;
    }

    private String generateCode() {
        return "VK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}