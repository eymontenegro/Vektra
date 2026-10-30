package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Consulta los totales que muestra el dashboard: conductores, vehículos,
 * estaciones y rutas.
 *
 * Si la consulta falla lanza una SQLException, de modo que un error de la
 * base de datos nunca se confunde con un total real de cero.
 */
public class DashboardDao {

    /**
     * Cuenta los conductores registrados.
     *
     * @return total de conductores
     * @throws SQLException si falla la consulta
     */
    public int countDrivers() throws SQLException {
        return count("SELECT COUNT(*) FROM drivers");
    }

    /**
     * Cuenta los vehículos registrados.
     *
     * @return total de vehículos
     * @throws SQLException si falla la consulta
     */
    public int countVehicles() throws SQLException {
        return count("SELECT COUNT(*) FROM vehicles");
    }

    /**
     * Cuenta las estaciones registradas.
     *
     * @return total de estaciones
     * @throws SQLException si falla la consulta
     */
    public int countStations() throws SQLException {
        return count("SELECT COUNT(*) FROM stations");
    }

    /**
     * Cuenta las rutas registradas.
     *
     * @return total de rutas
     * @throws SQLException si falla la consulta
     */
    public int countRoutes() throws SQLException {
        return count("SELECT COUNT(*) FROM routes");
    }

    /**
     * Ejecuta una consulta COUNT y devuelve su resultado. Es el único lugar
     * donde se abre la conexión para el dashboard.
     *
     * @param sql consulta que devuelve una sola columna con el total
     * @return total obtenido
     * @throws SQLException si falla la consulta o no devuelve resultado
     */
    private int count(String sql) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("La consulta de totales no devolvió resultado.");
            }
            return resultSet.getInt(1);
        }
    }
}