package com.vektra.web;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vektra.model.LineColor;
import com.vektra.model.Route;
import com.vektra.model.Station;
import java.util.List;
import org.junit.jupiter.api.Test;
/** Pruebas del JSON que recibe map.js. */
class MapDataBuilderTest {
    @Test
    void jsonContainsStationsRoutesAndLineColor() {
        Station plaza = new Station(1, "Plaza \"Alfonso\" López", 10.47, -73.24, List.of(LineColor.RED));
        Station terminal = new Station(2, "Terminal", 10.44, -73.25, List.of(LineColor.RED));
        Route route = new Route(1, plaza, terminal, null, LineColor.RED, 10, 4, 0);
        String json = MapDataBuilder.toJson(List.of(plaza, terminal), List.of(route));
        assertTrue(json.contains("\"stations\""));
        assertTrue(json.contains("#E50822"));
assertTrue(json.contains("\"lines\":[\"Roja\"]"));
// Las comillas del nombre deben quedar escapadas para no romper el JSON.
assertTrue(json.contains("Plaza \\\"Alfonso\\\" López"));
}
}
