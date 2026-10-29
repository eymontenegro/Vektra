package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.Driver;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los conductores (tabla drivers).
 *
 * Cada método abre y cierra su propia conexión. Ya no existe asignarARuta:
 * la ruta se guarda en la columna route_id al crear el conductor.
 */
public class DriverDao {

    private static final String SELECT_ALL = """
            SELECT id, national_id, name, last_name, license_number, phone, status, route_id
            FROM drivers
            ORDER BY last_name, name
            """;

    private static final String INSERT = """
            INSERT INTO drivers (national_id, name, last_name, license_number, phone, status, route_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    /**
     * Devuelve todos los conductores ordenados por apellido y nombre.
     *
     * @return conductores registrados; vacía si no hay ninguno
     * @throws SQLException si falla la consulta
     */
    public List<Driver> findAll() throws SQLException {
        List<Driver> drivers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                drivers.add(mapRow(resultSet));
            }
        }
        return drivers;
    }

    /**
     * Guarda un conductor nuevo y le asigna el id generado por la base.
     *
     * @param driver conductor a guardar
     * @return true si se insertó
     * @throws SQLException si falla la consulta o se rompe una restricción
     */
    public boolean save(Driver driver) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, new String[]{"id"})) {
            statement.setString(1, driver.getNationalId());
            statement.setString(2, driver.getName());
            statement.setString(3, driver.getLastName());
            statement.setString(4, driver.getLicenseNumber());
            statement.setString(5, driver.getPhone());
            statement.setString(6, driver.getStatus());
            statement.setInt(7, driver.getAssignedRouteId());
            if (statement.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    driver.setId(keys.getInt(1));
                }
            }
            return true;
        }
    }

    /**
     * Borra un conductor por su id.
     *
     * @param id id del conductor
     * @return true si existía y se borró
     * @throws SQLException si falla la consulta
     */
    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM drivers WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Indica si ya hay un conductor con esa cédula.
     *
     * @param nationalId cédula, como texto
     * @return true si ya existe
     * @throws SQLException si falla la consulta
     */
    public boolean existsByNationalId(String nationalId) throws SQLException {
        return exists("SELECT 1 FROM drivers WHERE national_id = ?", nationalId);
    }

    /**
     * Indica si ya hay un conductor con ese número de licencia.
     *
     * @param licenseNumber número de licencia
     * @return true si ya existe
     * @throws SQLException si falla la consulta
     */
    public boolean existsByLicenseNumber(String licenseNumber) throws SQLException {
        return exists("SELECT 1 FROM drivers WHERE license_number = ?", licenseNumber);
    }

    /**
     * Indica si el conductor tiene vehículos asignados.
     *
     * @param driverId id del conductor
     * @return true si algún vehículo lo tiene como conductor
     * @throws SQLException si falla la consulta
     */
    public boolean hasVehicles(int driverId) throws SQLException {
        String sql = "SELECT 1 FROM vehicles WHERE driver_id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, driverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Ejecuta una consulta de existencia con un solo parámetro de texto.
     *
     * @param sql   consulta con un único signo de interrogación
     * @param value valor a buscar
     * @return true si la consulta devuelve alguna fila
     * @throws SQLException si falla la consulta
     */
    private boolean exists(String sql, String value) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    /**
     * Convierte la fila actual en un conductor.
     *
     * @param resultSet resultado posicionado en un conductor
     * @return conductor leído
     * @throws SQLException si falta alguna columna
     */
    private Driver mapRow(ResultSet resultSet) throws SQLException {
        return new Driver(
                resultSet.getInt("id"),
                resultSet.getString("national_id"),
                resultSet.getString("name"),
                resultSet.getString("last_name"),
                resultSet.getString("license_number"),
                resultSet.getString("phone"),
                resultSet.getString("status"),
                resultSet.getInt("route_id"));
    }
}