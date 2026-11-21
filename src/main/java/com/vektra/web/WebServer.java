package com.vektra.web;

import com.vektra.exception.AuthorizationException;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor web de Vektra (Javalin con Jetty embebido).
 *
 * Configura los archivos estáticos (CSS, JavaScript, imágenes), los filtros
 * de seguridad que corren antes de cada petición y las páginas de error.
 * Cada controlador registra sus rutas con {@link #register(Controller)}.
 *
 * Las excepciones solo fijan el código HTTP y el mensaje; la página de error
 * la dibuja el manejador del código (app.error), que Javalin ejecuta
 * después del manejador de la excepción.
 */
public final class WebServer {

    private static final Logger LOGGER = Logger.getLogger(WebServer.class.getName());

    /** Atributo de la petición con el mensaje que mostrará la página de error. */
    private static final String ERROR_MESSAGE = "vektra.errorMessage";

    /**
     * Política de seguridad de contenido: el navegador solo ejecuta scripts
     * del propio servidor y de unpkg (Leaflet), y solo carga imágenes propias
     * y los mosaicos de OpenStreetMap. Un script inyectado no se ejecutaría.
     * style-src permite 'unsafe-inline' porque las plantillas usan atributos
     * style para los colores de las líneas y las barras del dashboard.
     */
    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' https://unpkg.com",
            "style-src 'self' https://unpkg.com 'unsafe-inline'",
            "img-src 'self' data: https://tile.openstreetmap.org https://unpkg.com",
            "font-src 'self'",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    private final Javalin app;
    private final TemplateRenderer renderer;

    /**
     * @param renderer motor de plantillas compartido por todos los controladores
     */
    public WebServer(TemplateRenderer renderer) {
        this.renderer = renderer;
        this.app = Javalin.create(config -> config.staticFiles.add("/static", Location.CLASSPATH));
        registerFilters();
        registerExceptionHandlers();
        registerErrorPages();
    }

    /**
     * Registra las rutas de un controlador.
     *
     * @param controller controlador a registrar
     * @return este servidor, para encadenar llamadas
     */
    public WebServer register(Controller controller) {
        controller.register(app);
        return this;
    }

    /**
     * Arranca el servidor.
     *
     * @param port puerto, por ejemplo 7070
     */
    public void start(int port) {
        app.start(port);
        LOGGER.log(Level.INFO, "Vektra está disponible en http://localhost:{0}", String.valueOf(port));
    }

    /** Detiene el servidor (lo usan las pruebas). */
    public void stop() {
        app.stop();
    }

    /** Filtros que corren antes de cada petición, en este orden. */
    private void registerFilters() {
        app.before(ctx -> {
            // Cabeceras básicas de seguridad para todas las respuestas.
            ctx.header("X-Content-Type-Options", "nosniff");
            ctx.header("X-Frame-Options", "DENY");
            ctx.header("Referrer-Policy", "same-origin");
            ctx.header("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        });
        app.before(AccessFilter::check);
        app.before(CsrfProtection::verify);
    }

    /** Cada tipo de excepción se convierte en un código HTTP con su mensaje. */
    private void registerExceptionHandlers() {
        app.exception(LoginRequiredException.class, (e, ctx) -> ctx.redirect("/login", HttpStatus.FOUND));
        app.exception(ValidationException.class, (e, ctx) -> fail(ctx, 400, e.getMessage()));
        app.exception(AuthorizationException.class, (e, ctx) -> fail(ctx, 403, e.getMessage()));
        app.exception(NotFoundException.class, (e, ctx) -> fail(ctx, 404, e.getMessage()));
        app.exception(DataAccessException.class, (e, ctx) -> fail(ctx, 503, e.getMessage()));
        app.exception(Exception.class, (e, ctx) -> {
            LOGGER.log(Level.SEVERE, "Error inesperado en " + ctx.method() + " " + ctx.path(), e);
            fail(ctx, 500, "Ocurrió un error inesperado. Intenta de nuevo.");
        });
    }

    /** Páginas de error, con un mensaje por defecto para cada código. */
    private void registerErrorPages() {
        errorPage(400, "Los datos enviados no son válidos.");
        errorPage(403, "No tienes permiso para ver esta página.");
        errorPage(404, "La página que buscas no existe.");
        errorPage(500, "Ocurrió un error inesperado. Intenta de nuevo.");
        errorPage(503, "El servicio no está disponible en este momento. Intenta de nuevo.");
    }

    private void errorPage(int status, String defaultMessage) {
        app.error(status, ctx -> {
            Object message = ctx.attribute(ERROR_MESSAGE);
            renderer.render(ctx, "error", Map.of(
                    "status", status,
                    "message", message == null ? defaultMessage : message.toString()));
        });
    }

    private static void fail(Context ctx, int status, String message) {
        ctx.attribute(ERROR_MESSAGE, message);
        ctx.status(status);
    }
}
