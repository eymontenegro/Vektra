package com.vektra.view;

import com.google.gson.Gson;
import com.vektra.model.LineColor;
import com.vektra.model.Route;
import com.vektra.model.Station;
import java.util.List;

/**
 * Arma las llamadas de JavaScript que MapView envía a map.html.
 *
 * Los datos se convierten a JSON con Gson, que escapa comillas, saltos de
 * línea y símbolos especiales (antes el JSON se armaba a mano, solo escapaba
 * las comillas y fallaba con valores nulos).
 */
final class MapScriptBuilder {

    /** Gson es seguro entre hilos, así que se comparte una sola instancia. */
    private static final Gson GSON = new Gson();

    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private MapScriptBuilder() {
    }

    /**
     * Llamada que dibuja las estaciones y las rutas en el mapa.
     *
     * @param stations estaciones a marcar
     * @param routes   rutas a dibujar
     * @return código JavaScript listo para executeScript
     */
    static String loadData(List<Station> stations, List<Route> routes) {
        MapData data = new MapData(
                stations.stream().map(MapScriptBuilder::toMarker).toList(),
                routes.stream().map(MapScriptBuilder::toLine).toList());
        return "loadData(" + GSON.toJson(data) + ");";
    }

    /**
     * Llamada que muestra un mensaje de error encima del mapa.
     *
     * @param message texto que verá la persona, en español
     * @return código JavaScript listo para executeScript
     */
    static String showError(String message) {
        return "showError(" + GSON.toJson(message) + ");";
    }

    private static StationMarker toMarker(Station station) {
        List<String> lineNames = station.getLines().stream().map(LineColor::getDisplayName).toList();
        return new StationMarker(station.getName(), station.getLatitude(), station.getLongitude(), lineNames);
    }

    private static RouteLine toLine(Route route) {
        List<double[]> path = route.getStops().stream()
                .map(stop -> new double[] {stop.getLatitude(), stop.getLongitude()})
                .toList();
        return new RouteLine(RouteFormatter.format(route), route.getLineColor().getHexCode(), path);
    }

    /** Todo lo que recibe la función loadData de map.html. */
    private record MapData(List<StationMarker> stations, List<RouteLine> routes) {
    }

    /** Estación tal como la dibuja el mapa. */
    private record StationMarker(String name, double lat, double lng, List<String> lines) {
    }

    /** Ruta tal como la dibuja el mapa: texto, color y recorrido completo. */
    private record RouteLine(String label, String color, List<double[]> path) {
    }
}
