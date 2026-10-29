package com.vektra.view;

import com.vektra.service.RouteService;
import java.awt.BorderLayout;
import java.net.URL;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JPanel;

/**
 * Mapa interactivo de las estaciones y rutas del metro.
 *
 * Es la única pantalla que usa JavaFX: muestra map.html dentro de un WebView.
 * No escribe SQL; pide los datos a RouteService en un hilo aparte para no
 * congelar el mapa, y se los pasa a la página como JSON. El mapa necesita
 * internet para descargar Leaflet y las imágenes de OpenStreetMap.
 */
public class MapView extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(MapView.class.getName());

    /** Ubicación de la página del mapa dentro de src/main/resources. */
    private static final String MAP_PAGE = "/map.html";

    /**
     * Identificación que exige OpenStreetMap para descargar el mapa,
     * con un contacto real del proyecto.
     */
    private static final String USER_AGENT = "Vektra/1.0 (+https://github.com/eymontenegro/Vektra)";

    /** Página que se muestra si map.html no está en el proyecto. */
    private static final String MISSING_PAGE = """
            <html><body style="font-family:sans-serif;color:#333333;padding:20px">
            <h3>No se encontró el mapa</h3>
            <p>Falta el archivo map.html en los recursos de la aplicación.</p>
            </body></html>
            """;

    private static final String DATABASE_ERROR =
            "No se pudieron cargar las estaciones y rutas. Revise la conexión con la base de datos.";

    private static final String DATA_ERROR =
            "Algunas estaciones o rutas tienen datos incorrectos y no se pudieron mostrar.";

    /** Servicio que entrega las estaciones y las rutas. */
    private final RouteService routeService;

    /** Puente entre Swing y JavaFX donde vive el WebView. */
    private final JFXPanel fxPanel = new JFXPanel();

    /**
     * Crea el mapa y empieza a cargarlo.
     *
     * @param routeService servicio que entrega las estaciones y las rutas
     */
    public MapView(RouteService routeService) {
        super(new BorderLayout());
        this.routeService = routeService;
        add(fxPanel, BorderLayout.CENTER);
        // Sin esto JavaFX se apaga al cambiar de pestaña y el mapa no vuelve a abrir.
        Platform.setImplicitExit(false);
        Platform.runLater(this::createWebView);
    }

    /** Crea el WebView y carga map.html. Corre en el hilo de JavaFX. */
    private void createWebView() {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setUserAgent(USER_AGENT);
        fxPanel.setScene(new Scene(webView));

        URL page = MapView.class.getResource(MAP_PAGE);
        if (page == null) {
            LOGGER.log(Level.WARNING, "No se encontró {0} en los recursos", MAP_PAGE);
            engine.loadContent(MISSING_PAGE);
            return;
        }
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                loadData(engine);
            }
        });
        engine.load(page.toExternalForm());
    }

    /**
     * Consulta las estaciones y rutas en un hilo aparte y, al terminar,
     * las dibuja en la página o muestra el error.
     *
     * @param engine motor de la página ya cargada
     */
    private void loadData(WebEngine engine) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws SQLException {
                return MapScriptBuilder.loadData(routeService.getStations(), routeService.getRoutes());
            }
        };
        task.setOnSucceeded(event -> engine.executeScript(task.getValue()));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            LOGGER.log(Level.WARNING, "No se pudieron cargar los datos del mapa", error);
            String message = error instanceof SQLException ? DATABASE_ERROR : DATA_ERROR;
            engine.executeScript(MapScriptBuilder.showError(message));
        });
        Thread loader = new Thread(task, "map-data-loader");
        loader.setDaemon(true);
        loader.start();
    }
}
