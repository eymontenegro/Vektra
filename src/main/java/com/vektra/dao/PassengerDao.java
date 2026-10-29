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

    /**
     * Busca un pasajero por correo.
     *
     * @return el pasajero, o null si no existe
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

    /** @return true si ya hay un pasajero con esa cédula */
    public boolean existsByNationalId(String nationalId) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE national_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nationalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Guarda un pasajero nuevo y le asigna el id generado.
     *
     * @return true si se insertó
     */
    public boolean save(Passenger passenger) throws SQLException {
        String sql = "INSERT INTO users (national_id, name, last_name, email, password, role, "
                + "birth_date, registration_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"})) {
            ps.setString(1, passenger.getNationalId());
            ps.setString(2, passenger.getName());
            ps.setString(3, passenger.getLastName());
            ps.setString(4, passenger.getEmail());
            ps.setString(5, passenger.getPasswordHash());
            ps.setString(6, Role.PASSENGER.name());
            ps.setDate(7, passenger.getBirthDate() == null ? null : Date.valueOf(passenger.getBirthDate()));
            ps.setTimestamp(8, Timestamp.valueOf(
                    passenger.getRegistrationDate() == null ? LocalDateTime.now() : passenger.getRegistrationDate()));

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
