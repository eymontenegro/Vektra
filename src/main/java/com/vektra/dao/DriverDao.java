package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.Driver;
import com.vektra.model.DriverStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los conductores (tabla drivers).
 *
 * Cada método abre y cierra su propia conexión. Todas las consultas usan
 * PreparedStatement, así ningún dato del usuario se pega dentro del SQL.
 */
public class DriverDao {

    private static final String COLUMNS =
            "id, national_id, name, last_name, license_number, phone, status, route_id";

    private static final String SELECT_ALL =
            "SELECT " + COLUMNS + " FROM drivers ORDER BY last_name, name";

    private static final String SELECT_BY_ID =
            "SELECT " + COLUMNS + " FROM drivers WHERE id = ?";

    private static final String INSERT = """
            INSERT INTO drivers (national_id, name, last_name, license_number, phone, status, route_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_STATUS = "UPDATE drivers SET status = ? WHERE id = ?";

    private static final String DELETE = "DELETE FROM drivers WHERE id = ?";

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
                drivers.add(DriverRowMapper.map(resultSet, "id"));
            }
        }
        return drivers;
    }

    /**
     * Busca un conductor por su id.
     *
     * @param id id del conductor
     * @return el conductor, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Driver findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? DriverRowMapper.map(resultSet, "id") : null;
            }
        }
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
            statement.setString(6, driver.getStatus().name());
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
     * Cambia el estado de un conductor.
     *
     * @param id     id del conductor
     * @param status estado nuevo
     * @return true si el conductor existía y se actualizó
     * @throws SQLException si falla la consulta
     */
    public boolean updateStatus(int id, DriverStatus status) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_STATUS)) {
            statement.setString(1, status.name());
            statement.setInt(2, id);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Borra un conductor por su id.
     *
     * @param id id del conductor
     * @return true si existía y se borró
     * @throws SQLException si falla la consulta (por ejemplo, 23503 si
     *                      todavía tiene vehículos asignados)
     */
    public boolean deleteById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE)) {
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
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM vehicles WHERE driver_id = ? LIMIT 1")) {
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
}
