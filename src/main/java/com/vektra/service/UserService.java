package com.vektra.service;

import com.vektra.dao.UserDao;
import com.vektra.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reglas de negocio de la cuenta del administrador: autenticación.
 * El administrador no se registra desde la aplicación; lo crea el script SQL.
 * <p>
 * Lanza {@link IllegalArgumentException} si falta un campo o las credenciales no
 * coinciden, e {@link IllegalStateException} si falla la base de datos.
 */
public class UserService {

    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    private final UserDao userDao;

    /** @param userDao acceso a datos del administrador */
    public UserService(UserDao userDao) {
        this.userDao = Objects.requireNonNull(userDao, "userDao no puede ser nulo");
    }

    /**
     * Verifica correo y contraseña del administrador.
     *
     * @param email    correo del administrador
     * @param password contraseña en texto plano
     * @return el administrador autenticado
     * @throws IllegalArgumentException si falta un campo o las credenciales no coinciden
     * @throws IllegalStateException    si falla la base de datos
     */
    public User authenticate(String email, String password) {
        String cleanEmail = normalizeEmail(email);
        requireCredentials(cleanEmail, password);
        try {
            User admin = userDao.findAdminByEmail(cleanEmail);
            if (admin == null || !BCrypt.checkpw(password, admin.getPasswordHash())) {
                throw new IllegalArgumentException("Correo o contraseña incorrectos.");
            }
            return admin;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de base de datos en UserService", e);
            throw new IllegalStateException("No se pudo conectar con la base de datos. Intenta de nuevo.", e);
        }
    }

    /** @return el correo sin espacios en los extremos y en minúsculas, o vacío si es nulo */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /** Lanza si el correo o la contraseña vienen vacíos. */
    private void requireCredentials(String email, String password) {
        if (email.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }
    }
}