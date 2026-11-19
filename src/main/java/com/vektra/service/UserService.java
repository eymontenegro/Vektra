package com.vektra.service;
import com.vektra.dao.UserDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.ValidationException;
import com.vektra.model.User;
import com.vektra.util.InputValidator;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Reglas de negocio de la cuenta del administrador: autenticación.
 * El administrador no se registra desde la aplicación; lo crea el script SQL.
 * <p>
 * Lanza {@link ValidationException} si falta un campo o las credenciales no
 * coinciden, y {@link DataAccessException} si falla la base de datos.
 */
public class UserService {
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    /** Mismo mensaje si el correo no existe o la clave está mal: no se revela cuál falló. */
    static final String INVALID_CREDENTIALS = "Correo o contraseña incorrectos.";
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
     * @throws ValidationException si falta un campo o las credenciales no coinciden
     * @throws DataAccessException si falla la base de datos
     */
    public User authenticate(String email, String password) {
        String cleanEmail = InputValidator.clean(email).toLowerCase();
        if (cleanEmail.isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Todos los campos son obligatorios.");
        }
        try {
            User admin = userDao.findAdminByEmail(cleanEmail);
            if (admin == null || !BCrypt.checkpw(password, admin.getPasswordHash())) {
                throw new ValidationException(INVALID_CREDENTIALS);
            }
            return admin;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Fallo de base de datos en UserService", e);
            throw new DataAccessException("No se pudo conectar con la base de datos. Intenta de nuevo.", e);
        }
    }
}
