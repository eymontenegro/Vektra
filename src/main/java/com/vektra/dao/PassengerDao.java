package com.vektra.dao;
import com.vektra.connection.DatabaseConnection;
import com.vektra.model.Passenger;
import com.vektra.model.Role;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
* Acceso a datos de los pasajeros (tabla {@code users}, rol PASSENGER).
* Los métodos lanzan {@link SQLException} para que el Service pueda distinguir
* un fallo de base de datos de unas credenciales incorrectas.
*/
public class PassengerDao {
private static final String SELECT_COLUMNS =
"SELECT id, national_id, name, last_name, email, password, birth_date, registration_date FROM users ";
private static final String INSERT = "INSERT INTO users (national_id, name, last_name, email, password, role, 
"
+ "birth_date, registration_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
/**
* Busca un pasajero por correo.
*
* @param email correo ya normalizado en minúsculas
* @return el pasajero, o null si no existe
* @throws SQLException si falla la consulta
*/
public Passenger findByEmail(String email) throws SQLException {
String sql = SELECT_COLUMNS + "WHERE email = ? AND role = ?";
try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, Role.PASSENGER.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }
    /**
     * Busca un pasajero por su id. La usa la web para recuperar los datos
     * completos del pasajero que tiene la sesión.
     *
     * @param id id del pasajero
     * @return el pasajero, o null si no existe
     * @throws SQLException si falla la consulta
     */
    public Passenger findById(int id) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE id = ? AND role = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setString(2, Role.PASSENGER.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }
    /**
     * Indica si ya hay una cuenta con ese correo (de cualquier rol).
     *
     * @param email correo ya normalizado en minúsculas
     * @return true si el correo ya está en uso
     * @throws SQLException si falla la consulta
     */
    public boolean existsByEmail(String email) throws SQLException {
        return exists("SELECT 1 FROM users WHERE email = ?", email);
    }
    /**
     * Indica si ya hay una cuenta con esa cédula.
     *
     * @param nationalId cédula
     * @return true si ya existe
     * @throws SQLException si falla la consulta
     */
    public boolean existsByNationalId(String nationalId) throws SQLException {
        return exists("SELECT 1 FROM users WHERE national_id = ?", nationalId);
    }
    /**
     * Guarda un pasajero nuevo y le asigna el id generado.
     *
     * @param passenger pasajero a guardar, con la contraseña ya convertida en hash
     * @return true si se insertó
     * @throws SQLException si falla la consulta o se rompe una restricción
     */
    public boolean save(Passenger passenger) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT, new String[]{"id"})) {
            ps.setString(1, passenger.getNationalId());
            ps.setString(2, passenger.getName());
            ps.setString(3, passenger.getLastName());
            ps.setString(4, passenger.getEmail());
            ps.setString(5, passenger.getPasswordHash());
            ps.setString(6, Role.PASSENGER.name());
            ps.setDate(7, passenger.getBirthDate() == null ? null : Date.valueOf(passenger.getBirthDate()));
            ps.setTimestamp(8, Timestamp.valueOf(
                    passenger.getRegistrationDate() == null ? LocalDateTime.now() : 
passenger.getRegistrationDate()));
            if (ps.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    passenger.setId(keys.getInt(1));
                }
            }
            return true;
        }
    }
    /** Ejecuta una consulta de existencia con un solo parámetro de texto. */
    private boolean exists(String sql, String value) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    /** Convierte la fila actual en un pasajero. */
    private Passenger mapRow(ResultSet rs) throws SQLException {
        Date birth = rs.getDate("birth_date");
        Timestamp registration = rs.getTimestamp("registration_date");
        LocalDate birthDate = birth == null ? null : birth.toLocalDate();
        LocalDateTime registrationDate = registration == null ? null : registration.toLocalDateTime();
        return new Passenger(
                rs.getInt("id"),
                rs.getString("national_id"),
                rs.getString("name"),
                rs.getString("last_name"),
                rs.getString("email"),
                rs.getString("password"),
                birthDate,
                registrationDate);
    }
}