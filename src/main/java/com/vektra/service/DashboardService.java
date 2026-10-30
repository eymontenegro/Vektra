package com.vektra.service;

import com.vektra.dao.DashboardDao;
import java.sql.SQLException;

/**
 * Entrega al DashboardPanel los totales del sistema.
 *
 * El panel solo habla con esta clase y nunca con el DAO. Si la base de datos
 * falla se propaga la SQLException, así el panel puede mostrar un error en
 * lugar de un cero engañoso.
 */
public class DashboardService {

    /** Acceso a los totales en la base de datos. */
    private final DashboardDao dashboardDao;

    /**
     * Crea el servicio con el DAO que usará.
     *
     * @param dashboardDao DAO de los totales del dashboard
     */
    public DashboardService(DashboardDao dashboardDao) {
        this.dashboardDao = dashboardDao;
    }

    /**
     * Total de conductores registrados.
     *
     * @return número de conductores
     * @throws SQLException si falla la consulta
     */
    public int getTotalDrivers() throws SQLException {
        return dashboardDao.countDrivers();
    }

    /**
     * Total de vehículos registrados.
     *
     * @return número de vehículos
     * @throws SQLException si falla la consulta
     */
    public int getTotalVehicles() throws SQLException {
        return dashboardDao.countVehicles();
    }

    /**
     * Total de estaciones registradas.
     *
     * @return número de estaciones
     * @throws SQLException si falla la consulta
     */
    public int getTotalStations() throws SQLException {
        return dashboardDao.countStations();
    }

    /**
     * Total de rutas registradas.
     *
     * @return número de rutas
     * @throws SQLException si falla la consulta
     */
    public int getTotalRoutes() throws SQLException {
        return dashboardDao.countRoutes();
    }
}