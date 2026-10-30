package com.vektra.service;

import com.vektra.dao.PassengerDao;
import com.vektra.model.Passenger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Reglas de negocio del pasajero: registro y autenticación.
 * <p>
 * Lanza {@link IllegalArgumentException} cuando los datos son inválidos o las
 * credenciales no coinciden, e {@link IllegalStateException} cuando falla la
 * base de datos. El mensaje de cada excepción está pensado para mostrarse al usuario.
 */
public class PassengerService {

    /** Longitud mínima de contraseña; la vista usa esta misma constante. */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final Logger LOGGER = Logger.getLogger(PassengerService.class.getName());
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("\\d{6,12}");

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
     * @param password   contraseña en texto plano (mínimo {@link #MIN_PASSWORD_LENGTH} caracteres)
     * @param birthDate  fecha de nacimiento
     * @return el pasajero guardado, con su id
     * @throws IllegalArgumentException si un dato es inválido o el correo/cédula ya existen
     * @throws IllegalStateException    si falla la base de datos
     */
    public Passenger register(String nationalId, String name, String lastName,
                              String email, String password, LocalDate birthDate) {
        String cleanId = clean(nationalId);
        String cleanName = clean(name);
        String cleanLastName = clean(lastName);
        String cleanEmail = clean(email).toLowerCase();

        validateRegistration(cleanId, cleanName, cleanLastName, cleanEmail, password, birthDate);

        try {
            if (passengerDao.findByEmail(cleanEmail) != null) {
                throw new IllegalArgumentException("Ese correo ya está registrado.");
            }
            if (passengerDao.existsByNationalId(cleanId)) {
                throw new IllegalArgumentException("Esa identificación ya está registrada.");
            }
            Passenger passenger = new Passenger(0, cleanId, cleanName, cleanLastName, cleanEmail,
                    BCrypt.hashpw(password, BCrypt.gensalt()), birthDate, LocalDateTime.now());
            if (!passengerDao.save(passenger)) {
                throw new IllegalStateException("No se pudo guardar la cuenta. Intenta de nuevo.");
            }
            return passenger;
        } catch (SQLException e) {
            // Otra solicitud pudo registrar el mismo dato entre la comprobación y el INSERT.
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new IllegalArgumentException("El correo o la identificación ya están registrados.");
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
     * @throws IllegalArgumentException si falta un campo o las credenciales no coinciden
     * @throws IllegalStateException    si falla la base de datos
     */
    public Passenger authenticate(String email, String password) {
        String cleanEmail = clean(email).toLowerCase();
        if (cleanEmail.isEmpty() || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }
        try {
            Passenger passenger = passengerDao.findByEmail(cleanEmail);
            if (passenger == null || !BCrypt.checkpw(password, passenger.getPasswordHash())) {
                throw new IllegalArgumentException("Correo o contraseña incorrectos.");
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
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }
        if (!NATIONAL_ID_PATTERN.matcher(nationalId).matches()) {
            throw new IllegalArgumentException("La identificación debe tener entre 6 y 12 dígitos, sin puntos ni espacios.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("El correo no tiene un formato válido.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres.");
        }
        if (birthDate.isAfter(LocalDate.now()) || birthDate.getYear() < 1900) {
            throw new IllegalArgumentException("La fecha de nacimiento no es válida.");
        }
    }

    /** @return el texto sin espacios en los extremos, o vacío si es nulo */
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    /** Registra el error en el log y devuelve una excepción con mensaje para el usuario. */
    private IllegalStateException databaseFailure(SQLException e) {
        LOGGER.log(Level.SEVERE, "Fallo de base de datos en PassengerService", e);
        return new IllegalStateException("No se pudo conectar con la base de datos. Intenta de nuevo.", e);
    }
}