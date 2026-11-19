package com.vektra.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vektra.dao.PassengerDao;
import com.vektra.dao.TicketDao;
import com.vektra.exception.AuthorizationException;
import com.vektra.exception.ValidationException;
import com.vektra.model.LineColor;
import com.vektra.model.Passenger;
import com.vektra.model.Role;
import com.vektra.model.Route;
import com.vektra.model.Station;
import com.vektra.model.Ticket;
import com.vektra.security.SessionUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/** Pruebas de la compra y la consulta de tickets, con DAO y servicios falsos. */
class TicketServiceTest {
private static final Station PLAZA = new Station(1, "Plaza", 10.47, -73.24, List.of());
private static final Station TERMINAL = new Station(2, "Terminal", 10.44, -73.25, List.of());
private static final Station ESTADIO = new Station(3, "Estadio", 10.46, -73.25, List.of());
private static final SessionUser PASSENGER =
new SessionUser(7, "Ana Ruiz", "ana@correo.com", Role.PASSENGER);
private static final SessionUser ADMIN =
new SessionUser(1, "Administrador", "admin@vektra.com", Role.ADMIN);
    private FakeTicketDao ticketDao;
    private FakeEmailService emailService;
    private TicketService service;
    @BeforeEach
    void setUp() {
        ticketDao = new FakeTicketDao();
        emailService = new FakeEmailService();
        service = new TicketService(ticketDao, new FakePassengerDao(), new FakeRouteService(), emailService);
    }
    @Test
    void buyTicketSavesWithFareAndSendsEmail() {
        TicketService.PurchaseResult result = service.buyTicket(PASSENGER, 1, 2);
        assertEquals(1, ticketDao.saved.size());
        assertEquals(new BigDecimal("2500"), result.ticket().getPrice());
        assertTrue(result.ticket().getCode().startsWith("VK-"));
        assertTrue(result.emailSent());
    }
    @Test
    void ticketIsKeptWhenEmailFails() {
        emailService.works = false;
        TicketService.PurchaseResult result = service.buyTicket(PASSENGER, 1, 2);
        assertFalse(result.emailSent());
        assertEquals(1, ticketDao.saved.size());
    }
    @Test
    void sameStationIsRejected() {
        assertThrows(ValidationException.class, () -> service.buyTicket(PASSENGER, 1, 1));
    }
    @Test
    void unknownStationIsRejected() {
        assertThrows(ValidationException.class, () -> service.buyTicket(PASSENGER, 1, 99));
    }
    @Test
    void adminCannotBuyTickets() {
        assertThrows(AuthorizationException.class, () -> service.buyTicket(ADMIN, 1, 2));
    }
    @Test
    void ticketWithoutDirectRouteIsRejected() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.buyTicket(PASSENGER, 1, 3));
        assertTrue(error.getMessage().startsWith("No hay una ruta directa"));
        assertEquals(0, ticketDao.saved.size());
    }
    @Test
    void passengerTicketsIncludeLineOfDirectRoute() {
        service.buyTicket(PASSENGER, 2, 1);
        // Ticket antiguo, comprado antes de la regla de ruta directa.
        Passenger ana = new FakePassengerDao().findById(7);
        ticketDao.saveTicket(new Ticket(0, "VK-ANTIGUO", java.time.LocalDateTime.now(),
                new BigDecimal("2500"), ana, PLAZA, ESTADIO));
        List<TicketService.PassengerTicket> tickets = service.getPassengerTickets(PASSENGER);
        assertEquals(LineColor.RED, tickets.get(0).line());
        assertNull(tickets.get(1).line());
    }
    /** Guarda los tickets en una lista. */
    private static final class FakeTicketDao extends TicketDao {
        private final List<Ticket> saved = new ArrayList<>();
        @Override
        public boolean saveTicket(Ticket ticket) {
            ticket.setId(saved.size() + 1);
            saved.add(ticket);
            return true;
        }
        @Override
        public List<Ticket> findByPassenger(Passenger passenger) {
            return saved.stream().filter(t -> t.getPassengerId() == passenger.getId()).toList();
        }
    }
    /** Siempre devuelve a la pasajera Ana con id 7. */
    private static final class FakePassengerDao extends PassengerDao {
        @Override
        public Passenger findById(int id) {
            return id == 7 ? new Passenger(7, "1020304050", "Ana", "Ruiz", "ana@correo.com", "hash",
                    LocalDate.of(2000, 1, 1), null) : null;
        }
    }
    /** Tres estaciones y una ruta Roja entre Plaza y Terminal. */
    private static final class FakeRouteService extends RouteService {
        FakeRouteService() {
            super(null, null);
        }
        @Override
        public List<Station> getStations() {
            return List.of(PLAZA, TERMINAL, ESTADIO);
        }
        @Override
        public List<Route> getRoutes() {
            return List.of(new Route(1, PLAZA, TERMINAL, null, LineColor.RED, 10, 4, 0));
        }
    }
    /** No envía correos: responde según el campo works. */
    private static final class FakeEmailService extends EmailService {
        private boolean works = true;
        FakeEmailService() {
            super("", "");
        }
        @Override
        public boolean send(Ticket ticket) {
            return works;
        }
    }
}
