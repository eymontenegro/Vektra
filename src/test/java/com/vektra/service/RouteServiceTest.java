package com.vektra.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vektra.dao.RouteDao;
import com.vektra.dao.StationDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.ValidationException;
import com.vektra.model.LineColor;
import com.vektra.model.Route;
import com.vektra.model.Station;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
/** Pruebas de RouteService con DAO falsos. */
class RouteServiceTest {
    private static final Station PLAZA = new Station(1, "Plaza", 10.47, -73.24, List.of(LineColor.RED));
    private static final Station TERMINAL = new Station(2, "Terminal", 10.44, -73.25, List.of(LineColor.RED));
    private final RouteService service = new RouteService(new FakeRouteDao(), new FakeStationDao(false));
    @Test
    void findRouteWorksInBothDirections() {
        assertTrue(service.findRoute(1, 2).isPresent());
        assertTrue(service.findRoute(2, 1).isPresent());
    }
    @Test
    void sameStationIsRejected() {
        assertThrows(ValidationException.class, () -> service.findRoute(1, 1));
    }
    @Test
    void routeExistsDelegatesToDao() {
        assertTrue(service.routeExists(1));
        assertEquals(false, service.routeExists(2));
    }
    @Test
    void databaseErrorBecomesDataAccessException() {
        RouteService broken = new RouteService(new FakeRouteDao(), new FakeStationDao(true));
        DataAccessException error = assertThrows(DataAccessException.class, broken::getStations);
        assertTrue(error.getMessage().startsWith("No se pudieron cargar"));
    }
    @Test
    void findStationReturnsMatchingStation() {
        assertEquals("Terminal", service.findStation(2).orElseThrow().getName());
        assertTrue(service.findStation(9).isEmpty());
    }
    private static final class FakeStationDao extends StationDao {
        private final boolean broken;
        FakeStationDao(boolean broken) {
            this.broken = broken;
        }
        @Override
        public List<Station> findAll() throws SQLException {
            if (broken) {
                throw new SQLException("sin conexión", "08001");
            }
            return List.of(PLAZA, TERMINAL);
        }
    }
    private static final class FakeRouteDao extends RouteDao {
        @Override
        public List<Route> findAll(Map<Integer, Station> stationsById) {
            return List.of(new Route(1, stationsById.get(1), stationsById.get(2), null, LineColor.RED, 10, 4, 0));
        }
        @Override
        public boolean existsById(int routeId) {
            return routeId == 1;
        }
    }
}