package com.vektra.web.controller;

import com.vektra.model.DashboardTotals;
import com.vektra.model.StationSales;
import com.vektra.security.SessionUser;
import com.vektra.service.DashboardService;
import com.vektra.util.TicketFormatter;
import com.vektra.web.Controller;
import com.vektra.web.TemplateRenderer;
import com.vektra.web.WebSession;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.List;
import java.util.Map;

/**
 * * Métricas del sistema para el administrador. Reemplaza a DashboardPanel. *
 * * Rutas: * GET /admin/dashboard totales y estaciones con más ventas
 */
public class DashboardController implements Controller {

    private final DashboardService dashboardService;
    private final TemplateRenderer renderer;

    /**
     * * Fila del ranking de ventas, con el precio ya formateado, el ancho de *
     * la barra (respecto a la estación con más ventas) y la parte del total
     *
     * * * @param stationName nombre de la estación * @param ticketsSold
     * tickets vendidos * @param revenue recaudo formateado, por ejemplo $5.000
     * * @param barPercent ancho de la barra, de 0 a 100 * @param sharePercent
     * porcentaje del total de tickets vendidos, de 0 a 100
     */
    public record SalesRow(String stationName, int ticketsSold, String revenue, int barPercent, int sharePercent) {

    }

    /**
     * * @param dashboardService datos del dashboard * @param renderer motor de
     * plantillas
     */
    public DashboardController(DashboardService dashboardService, TemplateRenderer renderer) {
        this.dashboardService = dashboardService;
        this.renderer = renderer;
    }

    @Override
    public void register(Javalin app) {
        app.get("/admin/dashboard", this::show);
    }

    private void show(Context ctx) {
        SessionUser actor = WebSession.requireUser(ctx);
        DashboardTotals totals = dashboardService.getTotals(actor);
        List<StationSales> sales = dashboardService.getTopOrigins(actor);
        int max = sales.stream().mapToInt(StationSales::ticketsSold).max().orElse(0);        // El total sale de la vista v_dashboard_totals, no de sumar esta lista,
    // porque la lista solo trae las estaciones con más ventas.        
    int total = totals.totalTickets();        
        List<SalesRow> rows = sales.stream().map(sale -> new SalesRow(sale.stationName(), sale.ticketsSold(), TicketFormatter.formatPrice(sale.revenue()), percent(sale.ticketsSold(), max), percent(sale.ticketsSold(), total))).toList();
        renderer.render(ctx, "admin/dashboard", Map.of( "totals", totals, "revenue",TicketFormatter.formatPrice(totals.totalRevenue()), "sales", rows)); }
    /** Porcentaje redondeado de parte sobre total; 0 si el total es 0. */ static int percent(int part, int total) { return total == 0 ? 0 : Math.round(part * 100f / total); } }
    
    