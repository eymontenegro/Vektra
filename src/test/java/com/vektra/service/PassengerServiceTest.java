package com.vektra.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.vektra.dao.PassengerDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Passenger;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * Pruebas de registro y autenticación de pasajeros. Usa un DAO falso en
 * memoria, así no necesita la base de datos.
 */
class PassengerServiceTest {
    private static final LocalDate BIRTH_DATE = LocalDate.of(2000, 1, 1);
    private FakePassengerDao dao;
    private PassengerService service;
    @BeforeEach
    void setUp() {
        dao = new FakePassengerDao();
        service = new PassengerService(dao);
    }
    @Test
    void registerNormalizesEmailAndHashesPassword() {
        Passenger saved = service.register("1020304050", " Ana ", "Ruiz", " ANA@Correo.com ",
                "claveSegura1", BIRTH_DATE);
        assertEquals("ana@correo.com", saved.getEmail());
        assertEquals("Ana", saved.getName());
        assertNotEquals("claveSegura1", saved.getPasswordHash());
        assertEquals(1, saved.getId());
    }
    @Test
    void registerRejectsShortPassword() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.register("1020304050", "Ana", "Ruiz", "ana@correo.com", "corta", BIRTH_DATE));
        assertTrue(error.getMessage().contains(String.valueOf(PassengerService.MIN_PASSWORD_LENGTH)));
    }
    @Test
    void registerRejectsInvalidNationalIdAndEmail() {
        assertThrows(ValidationException.class,
                () -> service.register("12-34", "Ana", "Ruiz", "ana@correo.com", "claveSegura1", BIRTH_DATE));
        assertThrows(ValidationException.class,
                () -> service.register("1020304050", "Ana", "Ruiz", "sin-arroba", "claveSegura1", BIRTH_DATE));
    }
    @Test
    void registerRejectsFutureBirthDate() {
        assertThrows(ValidationException.class, () -> service.register("1020304050", "Ana", "Ruiz",
                "ana@correo.com", "claveSegura1", LocalDate.now().plusDays(1)));
    }
    @Test
    void registerRejectsDuplicatedEmail() {
        service.register("1020304050", "Ana", "Ruiz", "ana@correo.com", "claveSegura1", BIRTH_DATE);
        ValidationException error = assertThrows(ValidationException.class, () -> service.register(
                "1020304051", "Otra", "Persona", "ana@correo.com", "claveSegura1", BIRTH_DATE));
        assertEquals("Ese correo ya está registrado.", error.getMessage());
    }
    @Test
    void uniqueViolationFromDatabaseBecomesValidationError() {
        dao.failOnSave = new SQLException("duplicado", "23505");
        assertThrows(ValidationException.class, () -> service.register("1020304050", "Ana", "Ruiz",
                "ana@correo.com", "claveSegura1", BIRTH_DATE));
    }
    @Test
    void otherDatabaseErrorsBecomeDataAccessErrors() {
        dao.failOnSave = new SQLException("sin conexión", "08001");
        assertThrows(DataAccessException.class, () -> service.register("1020304050", "Ana", "Ruiz",
                "ana@correo.com", "claveSegura1", BIRTH_DATE));
    }
    @Test
    void authenticateAcceptsRightPasswordAndRejectsWrongOne() {
        service.register("1020304050", "Ana", "Ruiz", "ana@correo.com", "claveSegura1", BIRTH_DATE);
        assertEquals("ana@correo.com", service.authenticate("ANA@correo.com", "claveSegura1").getEmail());
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.authenticate("ana@correo.com", "otraClave1"));
        assertEquals("Correo o contraseña incorrectos.", error.getMessage());
    }
    @Test
    void authenticateUsesSameMessageForUnknownEmail() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.authenticate("nadie@correo.com", "claveSegura1"));
        assertEquals("Correo o contraseña incorrectos.", error.getMessage());
    }
    /** DAO falso que guarda los pasajeros en una lista. */
    private static final class FakePassengerDao extends PassengerDao {
        private final List<Passenger> passengers = new ArrayList<>();
        private SQLException failOnSave;
        @Override
        public Passenger findByEmail(String email) {
            return passengers.stream().filter(p -> p.getEmail().equals(email)).findFirst().orElse(null);
        }
        @Override
        public Passenger findById(int id) {
            return passengers.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
        }
        @Override
        public boolean existsByEmail(String email) {
            return findByEmail(email) != null;
        }
        @Override
        public boolean existsByNationalId(String nationalId) {
            return passengers.stream().anyMatch(p -> p.getNationalId().equals(nationalId));
        }
        @Override
        public boolean save(Passenger passenger) throws SQLException {
            if (failOnSave != null) {
                throw failOnSave;
            }
            passenger.setId(passengers.size() + 1);
            passengers.add(passenger);
            return true;
        }
    }
}
