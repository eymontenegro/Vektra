package com.vektra.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.DashboardTotals;
import com.vektra.model.Route;
import com.vektra.model.Station;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de integración contra una base PostgreSQL real con los datos de
 * prueba de DataBase/vektra.sql (versión 2).
 *
 * Solo se ejecutan con: mvn test -Pdatabase-tests
 * Usan la misma configuración que la aplicación (config.properties o variables
 * de entorno). Son de solo lectura: no cambian datos.
 */
@Tag("database")
class DatabaseIT {

    @Test
    void connectionOpens() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            assertTrue(connection.isValid(2));
        }
    }

    @Test
    void stationsAndRoutesMatchSeedData() throws Exception {
        List<Station> stations = new StationDao().findAll();
        assertEquals(8, stations.size());
        Map<Integer, Station> byId = stations.stream()
                .collect(Collectors.toMap(Station::getId, Function.identity()));
        List<Route> routes = new RouteDao().findAll(byId);
        assertEquals(4, routes.size());
        assertTrue(routes.stream().allMatch(route -> route.getStops().size() >= 2));
    }

    @Test
    void routeExistsById() throws Exception {
        Map<Integer, Station> byId = new StationDao().findAll().stream()
                .collect(Collectors.toMap(Station::getId, Function.identity()));
        int existingId = new RouteDao().findAll(byId).get(0).getId();
        assertTrue(new RouteDao().existsById(existingId));
        assertFalse(new RouteDao().existsById(-1));
    }

    @Test
    void dashboardViewReturnsTotals() throws Exception {
        DashboardTotals totals = new DashboardDao().findTotals();
        assertEquals(8, totals.totalStations());
        assertEquals(4, totals.totalRoutes());
        assertEquals(2, totals.totalVehicles());
    }
}
