package com.vektra.service;

import com.vektra.dao.PassengerDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Passenger;
import com.vektra.util.InputValidator;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reglas de negocio del pasajero: registro, autenticación y consulta.
 * <p>
 * Lanza {@link ValidationException} cuando los datos son inválidos o las
 * credenciales no coinciden, y {@link DataAccessException} cuando falla la
 * base de datos. El mensaje de cada excepción se puede mostrar al usuario.
 */
public class PassengerService {

    /** Longitud mínima de contraseña; el formulario HTML usa esta misma constante. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /** Longitud máxima de contraseña: BCrypt solo usa los primeros 72 bytes. */
    public static final int MAX_PASSWORD_LENGTH = 72;

    /** Largo máximo de nombres y apellidos, igual que las columnas de users. */
    private static final int MAX_NAME_LENGTH = 100;

    /** Largo máximo del correo, igual que la columna email. */
    private static final int MAX_EMAIL_LENGTH = 150;

    private static final Logger LOGGER = Logger.getLogger(PassengerService.class.getName());

    /** Código SQLSTATE de PostgreSQL para una violación de restricción UNIQUE. */
    private static final String UNIQUE_VIOLATION = "23505";

    private final PassengerDao passengerDao;

    /** @param passengerDao acceso a datos del pasajero */
    public PassengerService(PassengerDao passengerDao) {
        this.passengerDao = passengerDao;
    }

    /**
     * Registra un pasajero nuevo. La contraseña se guarda con hash BCrypt.
     *
     * @param nationalId identificación (6 a 12 dígitos)
     * @param name       nombres
     * @param lastName   apellidos
     * @param email      correo con formato válido
     * @param password   contraseña en texto plano
     * @param birthDate  fecha de nacimiento
     * @return el pasajero guardado, con su id
     * @throws ValidationException si un dato es inválido o el correo/cédula ya existen
     * @throws DataAccessException si falla la base de datos
     */
    public Passenger register(String nationalId, String name, String lastName,
                              String email, String password, LocalDate birthDate) {
        String cleanId = InputValidator.clean(nationalId);
        String cleanName = InputValidator.clean(name);
        String cleanLastName = InputValidator.clean(lastName);
        String cleanEmail = InputValidator.clean(email).toLowerCase();

        validateRegistration(cleanId, cleanName, cleanLastName, cleanEmail, password, birthDate);

        try {
            if (passengerDao.existsByEmail(cleanEmail)) {
                throw new ValidationException("Ese correo ya está registrado.");
            }
            if (passengerDao.existsByNationalId(cleanId)) {
                throw new ValidationException("Esa identificación ya está registrada.");
            }
            Passenger passenger = new Passenger(0, cleanId, cleanName, cleanLastName, cleanEmail,
                    BCrypt.hashpw(password, BCrypt.gensalt()), birthDate, LocalDateTime.now());
            if (!passengerDao.save(passenger)) {
                throw new DataAccessException("No se pudo guardar la cuenta. Intenta de nuevo.", null);
            }
            return passenger;
        } catch (SQLException e) {
            // Otra petición pudo registrar el mismo dato entre la comprobación y el INSERT.
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new ValidationException("El correo o la identificación ya están registrados.", e);
            }
            throw databaseFailure(e);
        }
    }

    /**
     * Verifica correo y contraseña de un pasajero.
     *
     * @param email    correo del pasajero
     * @param password contraseña en texto plano
     * @return el pasajero autenticado
     * @throws ValidationException si falta un campo o las credenciales no coinciden
     * @throws DataAccessException si falla la base de datos
     */
    public Passenger authenticate(String email, String password) {
        String cleanEmail = InputValidator.clean(email).toLowerCase();
        if (cleanEmail.isEmpty() || password == null || password.isEmpty()) {
            throw new ValidationException("Todos los campos son obligatorios.");
        }
        try {
            Passenger passenger = passengerDao.findByEmail(cleanEmail);
            if (passenger == null || !BCrypt.checkpw(password, passenger.getPasswordHash())) {
                throw new ValidationException(UserService.INVALID_CREDENTIALS);
            }
            return passenger;
        } catch (SQLException e) {
            throw databaseFailure(e);
        }
    }

    /**
     * Busca un pasajero por su id.
     *
     * @param id id del pasajero
     * @return el pasajero
     * @throws NotFoundException   si no existe
     * @throws DataAccessException si falla la base de datos
     */
    public Passenger getPassenger(int id) {
        try {
            Passenger passenger = passengerDao.findById(id);
            if (passenger == null) {
                throw new NotFoundException("La cuenta del pasajero ya no existe.");
            }
            return passenger;
        } catch (SQLException e) {
            throw databaseFailure(e);
        }
    }

    /** Valida los datos de registro ya limpios; lanza con el mensaje del primer dato inválido. */
    private void validateRegistration(String nationalId, String name, String lastName,
                                      String email, String password, LocalDate birthDate) {
        if (nationalId.isEmpty() || name.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || password == null || password.isEmpty() || birthDate == null) {
            throw new ValidationException("Todos los campos son obligatorios.");
        }
        InputValidator.requirePattern(nationalId, InputValidator.NATIONAL_ID,
                "La identificación debe tener entre 6 y 12 dígitos, sin puntos ni espacios.");
        InputValidator.requireMaxLength(name, MAX_NAME_LENGTH,
                "El nombre no puede tener más de " + MAX_NAME_LENGTH + " caracteres.");
        InputValidator.requireMaxLength(lastName, MAX_NAME_LENGTH,
                "El apellido no puede tener más de " + MAX_NAME_LENGTH + " caracteres.");
        InputValidator.requireMaxLength(email, MAX_EMAIL_LENGTH,
                "El correo no puede tener más de " + MAX_EMAIL_LENGTH + " caracteres.");
        InputValidator.requirePattern(email, InputValidator.EMAIL, "El correo no tiene un formato válido.");
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("La contraseña no puede tener más de " + MAX_PASSWORD_LENGTH + " caracteres.");
        }
        if (birthDate.isAfter(LocalDate.now()) || birthDate.getYear() < 1900) {
            throw new ValidationException("La fecha de nacimiento no es válida.");
        }
    }

    /** Registra el error en el log y devuelve una excepción con mensaje para el usuario. */
    private DataAccessException databaseFailure(SQLException e) {
        LOGGER.log(Level.SEVERE, "Fallo de base de datos en PassengerService", e);
        return new DataAccessException("No se pudo conectar con la base de datos. Intenta de nuevo.", e);
    }
}
