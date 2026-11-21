package com.vektra.web.controller;

import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Driver;
import com.vektra.model.DriverStatus;
import com.vektra.security.SessionUser;
import com.vektra.service.DriverService;
import com.vektra.service.RouteService;
import com.vektra.util.InputValidator;
import com.vektra.util.RouteFormatter;
import com.vektra.web.Controller;
import com.vektra.web.FlashMessages;
import com.vektra.web.TemplateRenderer;
import com.vektra.web.WebSession;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestión de conductores para el administrador. Reemplaza a DriversPanel,
 * AddDriverPanel y DeleteDriverPanel.
 *
 * Rutas:
 *   GET  /admin/drivers               lista de conductores
 *   GET  /admin/drivers/new           formulario de nuevo conductor
 *   POST /admin/drivers               guarda el conductor
 *   POST /admin/drivers/{id}/status   activa o desactiva
 *   POST /admin/drivers/{id}/delete   borra (pide doble confirmación en la página)
 */
public class DriverController implements Controller {

    private static final int UNPROCESSABLE = 422;
    private static final String LIST_PATH = "/admin/drivers";
    private static final String[] FORM_FIELDS = {"nationalId", "name", "lastName", "licenseNumber", "phone", "routeId"};

    private final DriverService driverService;
    private final RouteService routeService;
    private final TemplateRenderer renderer;

    /**
     * Fila de la tabla de conductores.
     *
     * @param driver     conductor
     * @param routeLabel texto de su ruta asignada
     */
    public record DriverRow(Driver driver, String routeLabel) { }

    /**
     * Opción del selector de rutas.
     *
     * @param id    id de la ruta
     * @param label texto que ve el usuario
     */
    public record RouteOption(int id, String label) { }

    /**
     * @param driverService reglas de los conductores
     * @param routeService  rutas para el selector y la tabla
     * @param renderer      motor de plantillas
     */
    public DriverController(DriverService driverService, RouteService routeService, TemplateRenderer renderer) {
        this.driverService = driverService;
        this.routeService = routeService;
        this.renderer = renderer;
    }

    @Override
    public void register(Javalin app) {
        app.get(LIST_PATH, this::list);
        app.get(LIST_PATH + "/new", ctx -> showForm(ctx, Map.of(), null));
        app.post(LIST_PATH, this::create);
        app.post(LIST_PATH + "/{id}/status", this::toggleStatus);
        app.post(LIST_PATH + "/{id}/delete", this::delete);
    }

    private void list(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        Map<Integer, String> routeLabels = routeOptions().stream()
                .collect(Collectors.toMap(RouteOption::id, RouteOption::label));
        List<DriverRow> rows = driverService.getDrivers(actor).stream()
                .map(driver -> new DriverRow(driver,
                        routeLabels.getOrDefault(driver.getAssignedRouteId(), "Ruta no encontrada")))
                .toList();
        renderer.render(ctx, "fleet/drivers", Map.of("drivers", rows));
    }

    /**
     * Muestra el formulario de nuevo conductor.
     *
     * @param ctx   petición actual
     * @param form  valores escritos antes (si hubo un error)
     * @param error mensaje de error, o null
     */
    private void showForm(Context ctx, Map<String, String> form, String error) {
        Map<String, Object> model = new HashMap<>();
        model.put("form", form);
        model.put("routes", routeOptions());
        if (error != null) {
            model.put("error", error);
        }
        renderer.render(ctx, "fleet/driver-form", model);
    }

    private void create(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        Map<String, String> form = readForm(ctx);
        try {
            int routeId = InputValidator.requireInt(form.get("routeId"), "Selecciona la ruta asignada.");
            driverService.addDriver(actor, form.get("nationalId"), form.get("name"), form.get("lastName"),
                    form.get("licenseNumber"), form.get("phone"), routeId);
            FlashMessages.success(ctx, "Conductor registrado con éxito.");
            ctx.redirect(LIST_PATH, HttpStatus.FOUND);
        } catch (ValidationException e) {
            ctx.status(UNPROCESSABLE);
            showForm(ctx, form, e.getMessage());
        }
    }

    private void toggleStatus(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        try {
            DriverStatus status = driverService.toggleStatus(actor, pathId(ctx));
            FlashMessages.success(ctx, "El conductor ahora está " + status.getDisplayName().toLowerCase() + ".");
        } catch (ValidationException | NotFoundException e) {
            FlashMessages.error(ctx, e.getMessage());
        }
        ctx.redirect(LIST_PATH, HttpStatus.FOUND);
    }

    private void delete(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        try {
            driverService.deleteDriver(actor, pathId(ctx));
            FlashMessages.success(ctx, "Conductor eliminado con éxito.");
        } catch (ValidationException | NotFoundException e) {
            FlashMessages.error(ctx, e.getMessage());
        }
        ctx.redirect(LIST_PATH, HttpStatus.FOUND);
    }

    /** Rutas para el selector, con el mismo texto que usaba AddDriverPanel. */
    private List<RouteOption> routeOptions() {
        return routeService.getRoutes().stream()
                .map(route -> new RouteOption(route.getId(), RouteFormatter.format(route)))
                .toList();
    }

    private static Map<String, String> readForm(Context ctx) {
        Map<String, String> form = new HashMap<>();
        for (String field : FORM_FIELDS) {
            String value = ctx.formParam(field);
            form.put(field, value == null ? "" : value);
        }
        return form;
    }

    private static int pathId(Context ctx) {
        return InputValidator.requireInt(ctx.pathParam("id"), "El conductor seleccionado no es válido.");
    }
}
