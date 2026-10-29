package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.Passenger;
import com.vektra.model.Station;
import com.vektra.model.Ticket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de los tickets (tabla {@code tickets}).
 * No depende de la sesión: quien lo llama indica de qué pasajero se trata.
 */
public class TicketDao {

    /**
     * Guarda un ticket nuevo y le asigna el id generado.
     *
     * @return true si se insertó
     */
    public boolean saveTicket(Ticket ticket) throws SQLException {
        String sql = "INSERT INTO tickets (passenger_id, origin_station_id, destination_station_id, "
                + "purchase_date, price, ticket_code) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"id"})) {
            ps.setInt(1, ticket.getPassengerId());
            ps.setInt(2, ticket.getOriginId());
            ps.setInt(3, ticket.getDestinationId());
            ps.setTimestamp(4, Timestamp.valueOf(
                    ticket.getPurchaseDate() == null ? LocalDateTime.now() : ticket.getPurchaseDate()));
            ps.setBigDecimal(5, ticket.getPrice());
            ps.setString(6, ticket.getCode());

            if (ps.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ticket.setId(keys.getInt(1));
                }
            }
            return true;
        }
    }

    /**
     * Lista los tickets de un pasajero, del más reciente al más antiguo.
     *
     * @return la lista (vacía si no tiene tickets)
     */
    public List<Ticket> findByPassenger(Passenger passenger) throws SQLException {
        String sql = "SELECT t.id, t.ticket_code, t.purchase_date, t.price, "
                + "o.id AS origin_id, o.name AS origin_name, "
                + "d.id AS destination_id, d.name AS destination_name "
                + "FROM tickets t "
                + "JOIN stations o ON t.origin_station_id = o.id "
                + "JOIN stations d ON t.destination_station_id = d.id "
                + "WHERE t.passenger_id = ? "
                + "ORDER BY t.purchase_date DESC";
        List<Ticket> tickets = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, passenger.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapRow(rs, passenger));
                }
            }
        }
        return tickets;
    }

    private Ticket mapRow(ResultSet rs, Passenger passenger) throws SQLException {
        Timestamp purchase = rs.getTimestamp("purchase_date");
        return new Ticket(
                rs.getInt("id"),
                rs.getString("ticket_code"),
                purchase == null ? null : purchase.toLocalDateTime(),
                rs.getBigDecimal("price"),
                passenger,
                new Station(rs.getInt("origin_id"), rs.getString("origin_name")),
                new Station(rs.getInt("destination_id"), rs.getString("destination_name")));
    }
}
