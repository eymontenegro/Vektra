package com.vektra.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.vektra.dao.DashboardDao;
import com.vektra.exception.AuthorizationException;
import com.vektra.exception.DataAccessException;
import com.vektra.model.DashboardTotals;
import com.vektra.model.Role;
import com.vektra.model.StationSales;
import com.vektra.security.SessionUser;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
/** Pruebas de permisos y errores del dashboard. */
class DashboardServiceTest {
    private static final SessionUser ADMIN = new SessionUser(1, "Administrador", "admin@vektra.com", Role.ADMIN);
    private static final SessionUser PASSENGER = new SessionUser(2, "Ana", "ana@correo.com", Role.PASSENGER);
    @Test
    void adminSeesTotals() {
        DashboardService service = new DashboardService(new FakeDashboardDao(false));
        assertEquals(8, service.getTotals(ADMIN).totalStations());
        assertEquals(DashboardService.TOP_ORIGINS_LIMIT, service.getTopOrigins(ADMIN).size());
    }
    @Test
    void passengerCannotSeeDashboard() {
        DashboardService service = new DashboardService(new FakeDashboardDao(false));
        assertThrows(AuthorizationException.class, () -> service.getTotals(PASSENGER));
    }
    @Test
    void databaseErrorIsNeverShownAsZero() {
        DashboardService service = new DashboardService(new FakeDashboardDao(true));
        assertThrows(DataAccessException.class, () -> service.getTotals(ADMIN));
    }
    private static final class FakeDashboardDao extends DashboardDao {
        private final boolean broken;
        FakeDashboardDao(boolean broken) {
            this.broken = broken;
        }
        @Override
        public DashboardTotals findTotals() throws SQLException {
            if (broken) {
                throw new SQLException("la vista no existe", "42P01");
            }
            return new DashboardTotals(2, 2, 2, 8, 4, 0, 0, BigDecimal.ZERO);
        }
        @Override
        public List<StationSales> findTopOrigins(int limit) {
            return java.util.stream.IntStream.range(0, limit)
                    .mapToObj(i -> new StationSales("Estación " + i, i, BigDecimal.ZERO))
                    .toList();
        }
    }
}