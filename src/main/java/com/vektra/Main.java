package com.vektra;

import com.vektra.dao.DriverDao;
import com.vektra.dao.PassengerDao;
import com.vektra.dao.RouteDao;
import com.vektra.dao.StationDao;
import com.vektra.dao.TicketDao;
import com.vektra.dao.UserDao;
import com.vektra.dao.VehicleDao;
import com.vektra.dao.DashboardDao;
import com.vektra.service.DashboardService;
import com.vektra.service.DriverService;
import com.vektra.service.EmailService;
import com.vektra.service.PassengerService;
import com.vektra.service.RouteService;
import com.vektra.service.TicketService;
import com.vektra.service.UserService;
import com.vektra.service.VehicleService;
import com.vektra.view.ActiveTicketsPanel;
import com.vektra.view.AddDriverPanel;
import com.vektra.view.AddVehiclePanel;
import com.vektra.view.BuyTicketPanel;
import com.vektra.view.CreateAccountFrame;
import com.vektra.view.DeleteDriverPanel;
import com.vektra.view.DriversPanel;
import com.vektra.view.LoginFrame;
import com.vektra.view.MainFrameView;
import com.vektra.view.MapPanel;
import com.vektra.view.DashboardPanel;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.SwingUtilities;

/**
 * Punto de entrada de Vektra. Crea los servicios con sus DAO, abre la
 * pantalla de inicio de sesión y conecta las ventanas entre sí. Los datos
 * de conexión se leen de config.properties (o de las variables de entorno)
 * la primera vez que se consulta la base de datos.
 */
public class Main {

    private static PassengerService passengerService;
    private static LoginFrame loginFrame;

    /** Constructor privado: la clase solo tiene main y no se instancia. */
    private Main() {
    }

    /**
     * Arranca la aplicación en el hilo de eventos de Swing.
     *
     * @param args argumentos de la línea de comandos (no se usan)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::openLoginScreen);
    }

    /** Abre la pantalla de inicio de sesión. */
    private static void openLoginScreen() {
        UserService userService = new UserService(new UserDao());
        passengerService = new PassengerService(new PassengerDao());
        loginFrame = new LoginFrame(userService, passengerService,
                Main::openMainWindow, Main::openCreateAccount);
        loginFrame.setVisible(true);
    }

    /** Abre la pantalla de crear cuenta y cierra el login. */
    private static void openCreateAccount() {
        loginFrame.dispose();
        new CreateAccountFrame(passengerService, Main::openLoginScreen).setVisible(true);
    }

    /** Abre la ventana principal con las secciones que el rol permite. */
    private static void openMainWindow() {
        RouteService routeService = new RouteService(new RouteDao(), new StationDao());
        DriverService driverService = new DriverService(new DriverDao(), routeService);
        VehicleService vehicleService = new VehicleService(new VehicleDao());
        TicketService ticketService = new TicketService(new TicketDao(), EmailService.fromConfig());

        DashboardService dashboardService = new DashboardService(new DashboardDao());
        DashboardPanel dashboardPanel = new DashboardPanel(dashboardService);

        DriversPanel driversPanel = new DriversPanel(driverService);
        AddDriverPanel addDriverPanel =
                new AddDriverPanel(driverService, routeService, driversPanel::refresh);
        DeleteDriverPanel deleteDriverPanel =
                new DeleteDriverPanel(driverService, driversPanel::refresh);
        AddVehiclePanel addVehiclePanel = new AddVehiclePanel(vehicleService, driverService);
        BuyTicketPanel buyTicketPanel = new BuyTicketPanel(ticketService, routeService);
        ActiveTicketsPanel activeTicketsPanel = new ActiveTicketsPanel(ticketService, routeService);

        Map<String, MainFrameView.Section> sections = new LinkedHashMap<>();
        sections.put("Mapa", new MainFrameView.Section(new MapPanel(routeService), () -> { }));
        sections.put("Comprar ticket",
                new MainFrameView.Section(buyTicketPanel, buyTicketPanel::refresh));
        sections.put("Mis tickets",
                new MainFrameView.Section(activeTicketsPanel, activeTicketsPanel::refresh));
        sections.put("Conductores",
                new MainFrameView.Section(driversPanel, driversPanel::refresh));
        sections.put("Añadir conductor",
                new MainFrameView.Section(addDriverPanel, addDriverPanel::refresh));
        sections.put("Eliminar conductor",
                new MainFrameView.Section(deleteDriverPanel, deleteDriverPanel::refresh));
        sections.put("Añadir vehículo",
                new MainFrameView.Section(addVehiclePanel, addVehiclePanel::refresh));

        sections.put("Dashboard", new MainFrameView.Section(dashboardPanel, dashboardPanel::refresh));

        new MainFrameView(sections, Main::openLoginScreen).setVisible(true);
    }
}
