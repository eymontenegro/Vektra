package com.vektra.web.controller;
import com.vektra.service.RouteService;
import com.vektra.web.Controller;
import com.vektra.web.MapDataBuilder;
import com.vektra.web.TemplateRenderer;
import io.javalin.Javalin;
import io.javalin.http.Context;
/**
* Mapa interactivo de estaciones y rutas. Reemplaza a MapPanel, MapView y
* map.html del WebView de JavaFX.
*
* Rutas:
*   GET /map      
página con el mapa (cualquier persona conectada)
*   GET /api/map  estaciones y rutas en JSON, que pide map.js
*/
public class MapController implements Controller {
private final RouteService routeService;
private final TemplateRenderer renderer;
/**
* @param routeService estaciones y rutas
* @param renderer     
motor de plantillas
*/
public MapController(RouteService routeService, TemplateRenderer renderer) {
this.routeService = routeService;
        this.renderer = renderer;
    }
    @Override
    public void register(Javalin app) {
        app.get("/map", ctx -> renderer.render(ctx, "map/map"));
        app.get("/api/map", this::mapData);
    }
    /** Si la base falla, RouteService lanza DataAccessException y WebServer responde 503. */
    private void mapData(Context ctx) {
        String json = MapDataBuilder.toJson(routeService.getStations(), routeService.getRoutes());
        ctx.contentType("application/json; charset=utf-8");
        ctx.result(json);
    }
}
