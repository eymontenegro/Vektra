package com.vektra.web.controller;

import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Driver;
import com.vektra.model.LineColor;
import com.vektra.model.Vehicle;
import com.vektra.security.SessionUser;
import com.vektra.service.DriverService;
import com.vektra.service.VehicleService;
import com.vektra.util.InputValidator;
import com.vektra.web.Controller;
import com.vektra.web.FlashMessages;
import com.vektra.web.TemplateRenderer;
import com.vektra.web.WebSession;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestión de vehículos para el administrador. Reemplaza a AddVehiclePanel y
 * agrega la lista, la edición y el borrado, que VehicleService ya tenía
 * (updateVehicle y deleteVehicle) pero ninguna pantalla usaba.
 *
 * Rutas:
 *   GET  /admin/vehicles               lista de vehículos
 *   GET  /admin/vehicles/new           formulario de nuevo vehículo
 *   POST /admin/vehicles               guarda el vehículo
 *   GET  /admin/vehicles/{id}/edit     formulario de edición
 *   POST /admin/vehicles/{id}          guarda los cambios
 *   POST /admin/vehicles/{id}/delete   borra el vehículo
 */
public class VehicleController implements Controller {

    private static final int UNPROCESSABLE = 422;
    private static final String LIST_PATH = "/admin/vehicles";
    private static final String[] FORM_FIELDS = {"plate", "brand", "model", "year", "capacity", "line", "driverId"};

    /** Capacidad que propone el formulario nuevo, igual que AddVehiclePanel. */
    private static final String DEFAULT_CAPACITY = "50";

    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final TemplateRenderer renderer;

    /**
     * Opción del selector de conductores.
     *
     * @param id    id del conductor
     * @param label nombre y cédula, igual que en AddVehiclePanel
     */
    public record DriverOption(int id, String label) { }

    /**
     * @param vehicleService reglas de los vehículos
     * @param driverService  conductores para el selector
     * @param renderer       motor de plantillas
     */
    public VehicleController(VehicleService vehicleService, DriverService driverService, TemplateRenderer renderer) {
        this.vehicleService = vehicleService;
        this.driverService = driverService;
        this.renderer = renderer;
    }

    @Override
    public void register(Javalin app) {
        app.get(LIST_PATH, this::list);
        app.get(LIST_PATH + "/new", this::showCreateForm);
        app.post(LIST_PATH, this::create);
        app.get(LIST_PATH + "/{id}/edit", this::showEditForm);
        app.post(LIST_PATH + "/{id}", this::update);
        app.post(LIST_PATH + "/{id}/delete", this::delete);
    }

    private void list(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        renderer.render(ctx, "fleet/vehicles", Map.of("vehicles", vehicleService.getVehicles(actor)));
    }

    private void showCreateForm(Context ctx) {
        Map<String, String> form = new HashMap<>();
        form.put("year", String.valueOf(Year.now().getValue()));
        form.put("capacity", DEFAULT_CAPACITY);
        showForm(ctx, form, null, null);
    }

    private void showEditForm(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        int id = pathId(ctx);
        showForm(ctx, toForm(vehicleService.getVehicle(actor, id)), id, null);
    }

    /**
     * Muestra el formulario de vehículo.
     *
     * @param ctx       petición actual
     * @param form      valores de los campos
     * @param vehicleId id si se está editando, o null si es nuevo
     * @param error     mensaje de error, o null
     */
    private void showForm(Context ctx, Map<String, String> form, Integer vehicleId, String error) {
        SessionUser actor = WebSession.requireUser(ctx);
        Map<String, Object> model = new HashMap<>();
        model.put("form", form);
        model.put("pageTitle", vehicleId == null ? "Añadir vehículo" : "Editar vehículo");
        model.put("action", vehicleId == null ? LIST_PATH : LIST_PATH + "/" + vehicleId);
        model.put("lines", LineColor.values());
        model.put("drivers", driverOptions(actor));
        model.put("minYear", 1900);
        model.put("maxYear", Year.now().getValue() + 1);
        if (error != null) {
            model.put("error", error);
        }
        renderer.render(ctx, "fleet/vehicle-form", model);
    }

    private void create(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        Map<String, String> form = readForm(ctx);
        try {
            VehicleInput input = parse(actor, form);
            vehicleService.addVehicle(actor, form.get("plate"), form.get("brand"), form.get("model"),
                    input.year(), input.capacity(), input.line(), input.driver());
            FlashMessages.success(ctx, "Vehículo registrado con éxito.");
            ctx.redirect(LIST_PATH, HttpStatus.FOUND);
        } catch (ValidationException | NotFoundException e) {
            ctx.status(UNPROCESSABLE);
            showForm(ctx, form, null, e.getMessage());
        }
    }

    private void update(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        int id = pathId(ctx);
        Map<String, String> form = readForm(ctx);
        try {
            VehicleInput input = parse(actor, form);
            vehicleService.updateVehicle(actor, id, form.get("plate"), form.get("brand"), form.get("model"),
                    input.year(), input.capacity(), input.line(), input.driver());
            FlashMessages.success(ctx, "Vehículo actualizado con éxito.");
            ctx.redirect(LIST_PATH, HttpStatus.FOUND);
        } catch (ValidationException | NotFoundException e) {
            ctx.status(UNPROCESSABLE);
            showForm(ctx, form, id, e.getMessage());
        }
    }

    private void delete(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        try {
            vehicleService.deleteVehicle(actor, pathId(ctx));
            FlashMessages.success(ctx, "Vehículo eliminado con éxito.");
        } catch (ValidationException | NotFoundException e) {
            FlashMessages.error(ctx, e.getMessage());
        }
        ctx.redirect(LIST_PATH, HttpStatus.FOUND);
    }

    /** Datos del formulario ya convertidos a sus tipos. */
    private record VehicleInput(int year, int capacity, LineColor line, Driver driver) { }

    private VehicleInput parse(SessionUser actor, Map<String, String> form) {
        int year = InputValidator.requireInt(form.get("year"), "El año debe ser un número.");
        int capacity = InputValidator.requireInt(form.get("capacity"), "La capacidad debe ser un número.");
        LineColor line;
        try {
            line = LineColor.fromCode(form.get("line"));
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Selecciona la línea del vehículo.", e);
        }
        int driverId = InputValidator.requireInt(form.get("driverId"), "Selecciona el conductor.");
        Driver driver = driverService.getDriver(actor, driverId);
        return new VehicleInput(year, capacity, line, driver);
    }

    private List<DriverOption> driverOptions(SessionUser actor) {
        return driverService.getDrivers(actor).stream()
                .map(driver -> new DriverOption(driver.getId(),
                        driver.getFullName() + " - " + driver.getNationalId()))
                .toList();
    }

    private static Map<String, String> toForm(Vehicle vehicle) {
        Map<String, String> form = new HashMap<>();
        form.put("plate", vehicle.getPlate());
        form.put("brand", vehicle.getBrand() == null ? "" : vehicle.getBrand());
        form.put("model", vehicle.getModel() == null ? "" : vehicle.getModel());
        form.put("year", String.valueOf(vehicle.getYear()));
        form.put("capacity", String.valueOf(vehicle.getCapacity()));
        form.put("line", vehicle.getLine().name());
        form.put("driverId", String.valueOf(vehicle.getDriver().getId()));
        return form;
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
        return InputValidator.requireInt(ctx.pathParam("id"), "El vehículo seleccionado no es válido.");
    }
}
