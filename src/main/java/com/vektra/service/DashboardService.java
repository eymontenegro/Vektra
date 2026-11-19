package com.vektra.service;
import com.vektra.dao.DashboardDao;
import com.vektra.exception.DataAccessException;
import com.vektra.model.DashboardTotals;
import com.vektra.model.StationSales;
import com.vektra.security.AccessControl;
import com.vektra.security.SessionUser;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Entrega los datos del dashboard. Solo lo puede ver el administrador.
 *
 * Si la base de datos falla lanza DataAccessException: la pantalla muestra
 * el error en lugar de un cero engañoso.
 */
public class DashboardService {
    /** Número de estaciones que aparecen en el ranking de ventas. */
    public static final int TOP_ORIGINS_LIMIT = 5;
    private static final Logger LOGGER = Logger.getLogger(DashboardService.class.getName());
    private static final String LOAD_ERROR =
            "No se pudieron cargar las métricas. Revisa la conexión con la base de datos.";
    private final DashboardDao dashboardDao;
    /**
     * @param dashboardDao DAO de las vistas del dashboard
     */
    public DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }
    /**
     * Totales del sistema.
     *
     * @param actor usuario de la sesión
     * @return totales actuales
     * @throws com.vektra.exception.AuthorizationException si no es administrador
     * @throws DataAccessException si falla la base de datos
     */
    public DashboardTotals getTotals(SessionUser actor) {
        AccessControl.requireDashboardViewer(actor);
        try {
            return dashboardDao.findTotals();
        } catch (SQLException e) {
            throw failure(e);
        }
    }
    /**
     * Estaciones de origen con más tickets vendidos.
     *
     * @param actor usuario de la sesión
     * @return hasta {@link #TOP_ORIGINS_LIMIT} estaciones
     * @throws com.vektra.exception.AuthorizationException si no es administrador
     * @throws DataAccessException si falla la base de datos
     */
    public List<StationSales> getTopOrigins(SessionUser actor) {
        AccessControl.requireDashboardViewer(actor);
        try {
            return dashboardDao.findTopOrigins(TOP_ORIGINS_LIMIT);
        } catch (SQLException e) {
            throw failure(e);
        }
    }
    private DataAccessException failure(SQLException e) {
        LOGGER.log(Level.SEVERE, "Fallo de base de datos en DashboardService", e);
        return new DataAccessException(LOAD_ERROR, e);
    }
}