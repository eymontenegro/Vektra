package com.vektra.web;
import com.google.gson.Gson;
import com.vektra.model.LineColor;
import com.vektra.model.Route;
import com.vektra.model.Station;
import com.vektra.util.RouteFormatter;
import java.util.List;
/**
 * Convierte las estaciones y rutas en el JSON que dibuja map.js.
 *
 * Reemplaza a MapScriptBuilder: antes Java armaba una llamada de JavaScript
 * para el WebView de JavaFX; ahora el navegador pide /api/map y recibe JSON.
 * Gson escapa comillas, saltos de línea y símbolos especiales.
 */
public final class MapDataBuilder {
    /** Gson es seguro entre hilos, así que se comparte una sola instancia. */
    private static final Gson GSON = new Gson();
    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private MapDataBuilder() {
    }
    /**
     * JSON con todo lo que dibuja el mapa.
     *
     * @param stations estaciones a marcar
     * @param routes   rutas a dibujar
     * @return texto JSON {"stations":[...],"routes":[...]}
     */
    public static String toJson(List<Station> stations, List<Route> routes) {
        MapData data = new MapData(
                stations.stream().map(MapDataBuilder::toMarker).toList(),
                routes.stream().map(MapDataBuilder::toLine).toList());
        return GSON.toJson(data);
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
/** Todo lo que recibe map.js. */
private record MapData(List<StationMarker> stations, List<RouteLine> routes) {
}
/** Estación tal como la dibuja el mapa. */
private record StationMarker(String name, double lat, double lng, List<String> lines) {
}
/** Ruta tal como la dibuja el mapa: texto, color y recorrido completo. */
private record RouteLine(String label, String color, List<double[]> path) {
}
}