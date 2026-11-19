package com.vektra.model;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
/** Pruebas de las reglas de la clase Route. */
class RouteTest {
    private final Station plaza = new Station(1, "Plaza", 10.47, -73.24, List.of());
    private final Station terminal = new Station(2, "Terminal", 10.44, -73.25, List.of());
    private final Station estadio = new Station(3, "Estadio", 10.46, -73.25, List.of());
    @Test
    void stopsIncludeOriginIntermediatesAndDestination() {
        Route route = new Route(1, plaza, terminal, List.of(estadio), LineColor.RED, 10, 4, 0);
        assertEquals(List.of(plaza, estadio, terminal), route.getStops());
    }
    @Test
    void connectsWorksInBothDirections() {
        Route route = new Route(1, plaza, terminal, null, LineColor.RED, 10, 4, 0);
        assertTrue(route.connects(1, 2));
        assertTrue(route.connects(2, 1));
assertFalse(route.connects(1, 3));
}
@Test
void sameOriginAndDestinationIsRejected() {
assertThrows(IllegalArgumentException.class,
() -> new Route(1, plaza, plaza, null, LineColor.RED, 10, 4, 0));
}
@Test
void nonPositiveTimeOrDistanceIsRejected() {
assertThrows(IllegalArgumentException.class,
() -> new Route(1, plaza, terminal, null, LineColor.RED, 0, 4, 0));
assertThrows(IllegalArgumentException.class,
() -> new Route(1, plaza, terminal, null, LineColor.RED, 10, -1, 0));
}
}