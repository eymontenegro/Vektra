package com.vektra.dao;

import com.vektra.model.Driver;
import com.vektra.model.DriverStatus;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Convierte una fila de la tabla drivers en un Driver.
 *
 * Antes DriverDao y VehicleDao armaban el conductor cada uno con su propio
 * código repetido; ahora los dos usan esta clase.
 */
final class DriverRowMapper {

    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private DriverRowMapper() {
    }

    /**
     * Lee el conductor de la fila actual.
     *
     * @param resultSet resultado posicionado en una fila con las columnas de drivers
     * @param idColumn  nombre de la columna con el id del conductor
     *                  ("id" en DriverDao, "driver_id" en VehicleDao)
     * @return conductor leído
     * @throws SQLException si falta alguna columna o el estado no es válido
     */
    static Driver map(ResultSet resultSet, String idColumn) throws SQLException {
        DriverStatus status;
        try {
            status = DriverStatus.fromCode(resultSet.getString("status"));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Un conductor tiene un estado no válido en la base de datos.", e);
        }
        return new Driver(
                resultSet.getInt(idColumn),
                resultSet.getString("national_id"),
                resultSet.getString("name"),
                resultSet.getString("last_name"),
                resultSet.getString("license_number"),
                resultSet.getString("phone"),
                status,
                resultSet.getInt("route_id"));
    }
}
