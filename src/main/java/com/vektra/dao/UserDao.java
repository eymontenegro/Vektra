package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.Role;
import com.vektra.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Acceso a datos de la cuenta del administrador (tabla {@code users}, rol ADMIN).
 * La verificación del hash la hace el Service.
 */
public class UserDao {

    /**
     * Busca al administrador por correo.
     *
     * @return el administrador, o null si no existe
     */
    public User findAdminByEmail(String email) throws SQLException {
        String sql = "SELECT id, name, email, password, role FROM users WHERE email = ? AND role = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, Role.ADMIN.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                Role.valueOf(rs.getString("role")));
    }
}
