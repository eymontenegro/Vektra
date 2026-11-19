package com.vektra.dao;
import com.vektra.connection.DatabaseConnection;
import com.vektra.model.LineColor;
import com.vektra.model.Vehicle;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
/**
 * Acceso a datos de los vehículos (tabla vehicles).
 *
 * Cada método abre y cierra su propia conexión. Guarda y lee la línea y el
 * conductor de cada vehículo; el conductor se arma con DriverRowMapper.
 */
public class VehicleDao {
    /** Columnas del vehículo y de su conductor, para findAll y findById. */
    private static final String SELECT_WITH_DRIVER = """
            SELECT v.id, v.plate, v.brand, v.model, v.year, v.capacity, v.line,
                   d.id AS driver_id, d.national_id, d.name, d.last_name,
                   d.license_number, d.phone, d.status, d.route_id
            FROM vehicles v
            JOIN drivers d ON d.id = v.driver_id
            """;
    private static final String SELECT_ALL = SELECT_WITH_DRIVER + " ORDER BY v.plate";
    private static final String SELECT_BY_ID = SELECT_WITH_DRIVER + " WHERE v.id = ?";
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
     * Devuelve todos los vehículos, ordenados por placa, con su conductor.
     *
     * @return vehículos registrados; vacía si no hay ninguno
     * @throws SQLException si falla la consulta
     */
    public List<Vehicle> findAll() throws SQLException {
        List<Vehicle> vehicles = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                vehicles.add(mapRow(resultSet));
            }
        }
        return vehicles;
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
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM vehicles WHERE id = ?")) {
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
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM vehicles WHERE plate = ?")) 
{
            statement.setString(1, plate);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
    /**
     * Indica si existe un conductor con ese id.
     *
     * @param driverId id del conductor
     * @return true si existe
     * @throws SQLException si falla la consulta
     */
    public boolean driverExists(int driverId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM drivers WHERE id = ?")) {
            statement.setInt(1, driverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
    /**
     * Llena los siete datos del vehículo, en el mismo orden para el INSERT
     * y el UPDATE.
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
    /** Convierte la fila actual en un vehículo con su conductor. */
    private Vehicle mapRow(ResultSet resultSet) throws SQLException {
        try {
            return new Vehicle(
                    resultSet.getInt("id"),
                    resultSet.getString("plate"),
                    resultSet.getString("brand"),
                    resultSet.getString("model"),
                    resultSet.getInt("year"),
                    resultSet.getInt("capacity"),
                    LineColor.fromCode(resultSet.getString("line")),
                    DriverRowMapper.map(resultSet, "driver_id"));
        } catch (IllegalArgumentException e) {
            // El constructor de Vehicle valida año y capacidad: un dato viejo
            // fuera de rango se informa como error de datos, no como bug.
            throw new SQLException("Un vehículo tiene datos no válidos en la base de datos.", e);
        }
    }
}