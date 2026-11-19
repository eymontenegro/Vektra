package com.vektra;
import com.vektra.config.AppConfig;
import com.vektra.dao.DashboardDao;
import com.vektra.dao.PassengerDao;
import com.vektra.dao.RouteDao;
import com.vektra.dao.StationDao;
import com.vektra.dao.TicketDao;
import com.vektra.dao.UserDao;
import com.vektra.security.LoginAttemptLimiter;
import com.vektra.service.DashboardService;
import com.vektra.service.EmailService;
import com.vektra.service.PassengerService;
import com.vektra.service.RouteService;
import com.vektra.service.TicketService;
import com.vektra.service.UserService;
import com.vektra.web.TemplateRenderer;
import com.vektra.web.WebServer;
import com.vektra.web.controller.AuthController;
import com.vektra.web.controller.DashboardController;
import com.vektra.web.controller.MapController;
import com.vektra.web.controller.TicketController;
/**
 * Punto de entrada de Vektra.
 *
 * Crea los DAO, los servicios y los controladores (inyección de
 * dependencias por constructor, sin variables estáticas) y arranca el
 * servidor web. La aplicación queda en http://localhost:7070 (o el puerto
 * de server.port en config.properties).
 */
public final class Main {
    /** Puerto por defecto del servidor web. */
    private static final int DEFAULT_PORT = 7070;
    /** Constructor privado: la clase solo tiene main y no se instancia. */
    private Main() {
    }
    /**
     * Arranca la aplicación.
     *
     * @param args argumentos de la línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        AppConfig config = AppConfig.global();
        // Servicios que usan varias partes de la aplicación.
        RouteService routeService = new RouteService(new RouteDao(), new StationDao());
        PassengerDao passengerDao = new PassengerDao();
        // Las plantillas se guardan en caché salvo que server.dev=true.
        boolean devMode = config.get("server.dev").map(Boolean::parseBoolean).orElse(false);
        TemplateRenderer renderer = new TemplateRenderer(!devMode);
        WebServer server = new WebServer(renderer);
        // --- Líder: autenticación --
        UserService userService = new UserService(new UserDao());
        PassengerService passengerService = new PassengerService(passengerDao);
        LoginAttemptLimiter loginLimiter = new LoginAttemptLimiter();
        server.register(new AuthController(userService, passengerService, loginLimiter, renderer));
        // --- Desarrollador 1: dashboard --
        DashboardService dashboardService = new DashboardService(new DashboardDao());
        server.register(new DashboardController(dashboardService, renderer));
        // --- Desarrollador 2: mapa y tickets --
TicketService ticketService = new TicketService(new TicketDao(), passengerDao,
routeService, EmailService.fromConfig());
server.register(new MapController(routeService, renderer));
server.register(new TicketController(ticketService, routeService, renderer));
// --- Desarrollador 3: conductores y vehículos --
// (El desarrollador reemplaza este comentario con su código en su PR.)
server.start(config.getInt("server.port", DEFAULT_PORT));
}
}