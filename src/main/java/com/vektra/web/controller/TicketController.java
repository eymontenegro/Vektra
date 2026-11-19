package com.vektra.web.controller;
import com.google.gson.Gson;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.ValidationException;
import com.vektra.model.LineColor;
import com.vektra.model.Route;
import com.vektra.model.Ticket;
import com.vektra.security.SessionUser;
import com.vektra.service.RouteService;
import com.vektra.service.TicketService;
import com.vektra.service.TicketService.PassengerTicket;
import com.vektra.service.TicketService.PurchaseResult;
import com.vektra.util.InputValidator;
import com.vektra.util.RouteFormatter;
import com.vektra.util.TicketFormatter;
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
import java.util.Optional;
/**
 * Compra de tickets y "Mis tickets" del pasajero. Reemplaza a
 * BuyTicketPanel y ActiveTicketsPanel.
 *
 * Rutas (solo pasajeros, lo exige AccessFilter y lo vuelve a exigir TicketService):
 *   GET  /tickets            mis tickets
 *   GET  /tickets/buy        formulario de compra
 *   POST /tickets/buy        compra el ticket
 *   GET  /api/routes/find    ruta directa entre dos estaciones, en JSON (la usa buy-ticket.js)
 */
public class TicketController implements Controller {
    private static final int UNPROCESSABLE = 422;
    private static final Gson GSON = new Gson();
    private final TicketService ticketService;
    private final RouteService routeService;
    private final TemplateRenderer renderer;
    /**
     * Fila de la tabla "Mis tickets", con los textos ya formateados.
     *
     * @param code        código del ticket
     * @param origin      estación de origen
     * @param destination estación de destino
     * @param lineName    nombre de la línea, o "Sin ruta directa"
     * @param lineColor   color de la línea en hexadecimal, o null
     * @param date        fecha de compra dd/MM/yyyy HH:mm
     * @param price       precio, por ejemplo $2.500
     */
    public record TicketRow(String code, String origin, String destination,
                            String lineName, String lineColor, String date, String price) { }
    /**
     * Respuesta de /api/routes/find.
     *
     * @param found       true si hay una ruta directa
     * @param description texto que se muestra debajo de los selectores
     */
    private record RouteInfo(boolean found, String description) { }
    /**
     * @param ticketService compra y consulta de tickets
     * @param routeService  estaciones y rutas
     * @param renderer      motor de plantillas
     */
    public TicketController(TicketService ticketService, RouteService routeService,
                            TemplateRenderer renderer) {
        this.ticketService = ticketService;
        this.routeService = routeService;
        this.renderer = renderer;
    }
    @Override
    public void register(Javalin app) {
        app.get("/tickets", this::listTickets);
        app.get("/tickets/buy", ctx -> showBuyForm(ctx, Map.of()));
        app.post("/tickets/buy", this::buyTicket);
        app.get("/api/routes/find", this::findRoute);
    }
    private void listTickets(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        List<TicketRow> rows = ticketService.getPassengerTickets(actor).stream()
                .map(TicketController::toRow)
                .toList();
        renderer.render(ctx, "tickets/my-tickets", Map.of("tickets", rows));
    }
    /**
     * Muestra el formulario de compra.
     *
     * @param ctx   petición actual
     * @param extra datos adicionales (error y selección previa si la compra falló)
     */
    private void showBuyForm(Context ctx, Map<String, ?> extra) {
        Map<String, Object> model = new HashMap<>(extra);
        model.put("stations", routeService.getStations());
        model.put("fare", TicketFormatter.formatPrice(ticketService.getFare()));
        model.putIfAbsent("originId", "");
        model.putIfAbsent("destinationId", "");
        renderer.render(ctx, "tickets/buy-ticket", model);
    }
    private void buyTicket(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        String originParam = ctx.formParam("originId");
        String destinationParam = ctx.formParam("destinationId");
        try {
            int originId = InputValidator.requireInt(originParam, "Selecciona la estación de origen.");
            int destinationId = InputValidator.requireInt(destinationParam, "Selecciona la estación de destino.");
            PurchaseResult result = ticketService.buyTicket(actor, originId, destinationId);
            FlashMessages.success(ctx, purchaseMessage(result));
            ctx.redirect("/tickets", HttpStatus.FOUND);
        } catch (ValidationException e) {
            ctx.status(UNPROCESSABLE);
            showBuyForm(ctx, Map.of(
                    "error", e.getMessage(),
                    "originId", originParam == null ? "" : originParam,
                    "destinationId", destinationParam == null ? "" : destinationParam));
        }
    }
    /** Responde siempre JSON: el JavaScript no debe recibir una página HTML de error. */
    private void findRoute(Context ctx) {
        RouteInfo info;
        try {
            int originId = InputValidator.requireInt(ctx.queryParam("originId"), "Elige la estación de origen.");
            int destinationId = InputValidator.requireInt(ctx.queryParam("destinationId"),
                    "Elige la estación de destino.");
            Optional<Route> route = routeService.findRoute(originId, destinationId);
            info = route.map(found -> new RouteInfo(true, RouteFormatter.format(found)))
                    .orElse(new RouteInfo(false, "No hay una ruta directa entre estas estaciones."));
        } catch (ValidationException e) {
            info = new RouteInfo(false, e.getMessage());
        } catch (DataAccessException e) {
            ctx.status(503);
            info = new RouteInfo(false, "No se pudo consultar la ruta.");
        }
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(GSON.toJson(info));
    }
    /** Mismo mensaje que mostraba ConfirmationView tras la compra. */
    private static String purchaseMessage(PurchaseResult result) {
        Ticket ticket = result.ticket();
        String message = "Compraste el ticket " + ticket.getCode()
                + " por " + TicketFormatter.formatPrice(ticket.getPrice()) + ".";
        if (result.emailSent()) {
            return message + " Te lo enviamos a " + ticket.getPassengerEmail() + ".";
        }
        return message + " No pudimos enviarlo por correo: guarda el código, también lo verás en Mis tickets.";
    }
    private static TicketRow toRow(PassengerTicket passengerTicket) {
        Ticket ticket = passengerTicket.ticket();
        LineColor line = passengerTicket.line();
        return new TicketRow(
                ticket.getCode(),
                ticket.getOriginName(),
                ticket.getDestinationName(),
                line == null ? "Sin ruta directa" : line.getDisplayName(),
                line == null ? null : line.getHexCode(),
                TicketFormatter.formatDate(ticket.getPurchaseDate()),
                TicketFormatter.formatPrice(ticket.getPrice()));
    }
}
