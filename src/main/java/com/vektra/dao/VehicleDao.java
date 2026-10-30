package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.Driver;
import com.vektra.model.LineColor;
import com.vektra.model.Vehicle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Acceso a datos de los vehículos (tabla vehicles).
 *
 * Cada método abre y cierra su propia conexión. Antes findById,
 * updateVehicle y deleteVehicle usaban una conexión sin abrir y fallaban.
 * Guarda y lee la línea y el conductor de cada vehículo.
 */
public class VehicleDao {

    private static final String SELECT_BY_ID = """
            SELECT v.id, v.plate, v.brand, v.model, v.year, v.capacity, v.line,
                   d.id AS driver_id, d.national_id, d.name, d.last_name,
                   d.license_number, d.phone, d.status, d.route_id
            FROM vehicles v
            JOIN drivers d ON d.id = v.driver_id
            WHERE v.id = ?
            """;

    private static final String INSERT = """
            INSERT INTO vehicles (plate, brand, model, year, capacity, line, driver_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE = """
            UPDATE vehicles
            SET plate = ?, brand = ?, model = ?, year = ?, capacity = ?, line = ?, driver_id = ?
            WHERE id = ?
            """;

    /**
     * Guarda un vehículo nuevo y le asigna el id generado por la base.
     *
     * @param vehicle vehículo a guardar
     * @return true si se insertó
     * @throws SQLException si falla la consulta o se rompe una restricción
     */
    public boolean save(Vehicle vehicle) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT, new String[]{"id"})) {
            fillVehicleData(statement, vehicle);
            if (statement.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    vehicle.setId(keys.getInt(1));
                }
            }
            return true;
        }
    }

    /**
     * Busca un vehículo por su id, con su conductor.
     *
     * @param id id del vehículo
     * @return el vehículo, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Vehicle findById(int id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_ID)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    /**
     * Actualiza todos los datos de un vehículo existente.
     *
     * @param vehicle vehículo con su id y los datos nuevos
     * @return true si existía y se actualizó
     * @throws SQLException si falla la consulta o se rompe una restricción
     */
    public boolean updateVehicle(Vehicle vehicle) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            fillVehicleData(statement, vehicle);
            statement.setInt(8, vehicle.getId());
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Borra un vehículo por su id.
     *
     * @param id id del vehículo
     * @return true si existía y se borró
     * @throws SQLException si falla la consulta
     */
    public boolean deleteVehicle(int id) throws SQLException {
        String sql = "DELETE FROM vehicles WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Indica si ya hay un vehículo con esa placa.
     *
     * @param plate placa a buscar
     * @return true si ya existe
     * @throws SQLException si falla la consulta
     */
    public boolean existsByPlate(String plate) throws SQLException {
        return exists("SELECT 1 FROM vehicles WHERE plate = ?", plate);
    }

    /**
     * Indica si existe un conductor con ese id.
     *
     * @param driverId id del conductor
     * @return true si existe
     * @throws SQLException si falla la consulta
     */
    public boolean driverExists(int driverId) throws SQLException {
        String sql = "SELECT 1 FROM drivers WHERE id = ?";
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
     * Llena los siete datos del vehículo, en el mismo orden para el INSERT
     * y el UPDATE.
     *
     * @param statement consulta preparada con siete parámetros
     * @param vehicle   vehículo del que se toman los datos
     * @throws SQLException si falla al asignar algún parámetro
     */
    private void fillVehicleData(PreparedStatement statement, Vehicle vehicle) throws SQLException {
        statement.setString(1, vehicle.getPlate());
        statement.setString(2, vehicle.getBrand());
        statement.setString(3, vehicle.getModel());
        statement.setInt(4, vehicle.getYear());
        statement.setInt(5, vehicle.getCapacity());
        statement.setString(6, vehicle.getLine().name());
        statement.setInt(7, vehicle.getDriver().getId());
    }

    /**
     * Convierte la fila actual en un vehículo con su conductor.
     *
     * @param resultSet resultado posicionado en un vehículo
     * @return vehículo leído
     * @throws SQLException si falta alguna columna
     */
    private Vehicle mapRow(ResultSet resultSet) throws SQLException {
        Driver driver = new Driver(
                resultSet.getInt("driver_id"),
                resultSet.getString("national_id"),
                resultSet.getString("name"),
                resultSet.getString("last_name"),
                resultSet.getString("license_number"),
                resultSet.getString("phone"),
                resultSet.getString("status"),
                resultSet.getInt("route_id"));
        return new Vehicle(
                resultSet.getInt("id"),
                resultSet.getString("plate"),
                resultSet.getString("brand"),
                resultSet.getString("model"),
                resultSet.getInt("year"),
                resultSet.getInt("capacity"),
                LineColor.fromCode(resultSet.getString("line")),
                driver);
    }
}