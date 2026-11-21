package com.vektra.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vektra.dao.DriverDao;
import com.vektra.exception.AuthorizationException;
import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Driver;
import com.vektra.model.DriverStatus;
import com.vektra.model.Role;
import com.vektra.security.SessionUser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas de las reglas de los conductores, con DAO y servicio de rutas falsos. */
class DriverServiceTest {

    private static final SessionUser ADMIN = new SessionUser(1, "Administrador", "admin@vektra.com", Role.ADMIN);
    private static final SessionUser PASSENGER = new SessionUser(2, "Ana", "ana@correo.com", Role.PASSENGER);

    private FakeDriverDao dao;
    private DriverService service;

    @BeforeEach
    void setUp() {
        dao = new FakeDriverDao();
        service = new DriverService(dao, new FakeRouteService());
    }

    @Test
    void addDriverCreatesActiveDriverAndCleansOptionalFields() {
        Driver driver = service.addDriver(ADMIN, " 1002003004 ", "Luis", "Mora", "  ", "", 1);
        assertEquals(DriverStatus.ACTIVE, driver.getStatus());
        assertEquals("1002003004", driver.getNationalId());
        assertNull(driver.getLicenseNumber());
        assertNull(driver.getPhone());
    }

    @Test
    void passengerCannotAddDrivers() {
        assertThrows(AuthorizationException.class,
                () -> service.addDriver(PASSENGER, "1002003004", "Luis", "Mora", null, null, 1));
    }

    @Test
    void invalidDataIsRejected() {
        assertThrows(ValidationException.class,
                () -> service.addDriver(ADMIN, "ABC", "Luis", "Mora", null, null, 1));
        assertThrows(ValidationException.class,
                () -> service.addDriver(ADMIN, "1002003004", "Luis", "Mora", null, "300-111", 1));
        assertThrows(ValidationException.class,
                () -> service.addDriver(ADMIN, "1002003004", "Luis", "Mora", null, null, 99));
    }

    @Test
    void duplicatedNationalIdIsRejected() {
        service.addDriver(ADMIN, "1002003004", "Luis", "Mora", null, null, 1);
        assertThrows(ValidationException.class,
                () -> service.addDriver(ADMIN, "1002003004", "Otro", "Mora", null, null, 1));
    }

    @Test
    void toggleStatusSwitchesBetweenActiveAndInactive() {
        Driver driver = service.addDriver(ADMIN, "1002003004", "Luis", "Mora", null, null, 1);
        assertEquals(DriverStatus.INACTIVE, service.toggleStatus(ADMIN, driver.getId()));
        assertEquals(DriverStatus.ACTIVE, service.toggleStatus(ADMIN, driver.getId()));
    }

    @Test
    void driverWithVehiclesCannotBeDeleted() {
        Driver driver = service.addDriver(ADMIN, "1002003004", "Luis", "Mora", null, null, 1);
        dao.driversWithVehicles.add(driver.getId());
        assertThrows(ValidationException.class, () -> service.deleteDriver(ADMIN, driver.getId()));
    }

    @Test
    void foreignKeyRaceIsReportedAsValidation() {
        Driver driver = service.addDriver(ADMIN, "1002003004", "Luis", "Mora", null, null, 1);
        dao.failOnDelete = new SQLException("tiene vehículos", "23503");
        assertThrows(ValidationException.class, () -> service.deleteDriver(ADMIN, driver.getId()));
    }

    @Test
    void deletingUnknownDriverIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.deleteDriver(ADMIN, 404));
    }

    /** Guarda los conductores en una lista. */
    private static final class FakeDriverDao extends DriverDao {
        private final List<Driver> drivers = new ArrayList<>();
        private final List<Integer> driversWithVehicles = new ArrayList<>();
        private SQLException failOnDelete;

        @Override
        public List<Driver> findAll() {
            return List.copyOf(drivers);
        }

        @Override
        public Driver findById(int id) {
            return drivers.stream().filter(d -> d.getId() == id).findFirst().orElse(null);
        }

        @Override
        public boolean save(Driver driver) {
            driver.setId(drivers.size() + 1);
            drivers.add(driver);
            return true;
        }

        @Override
        public boolean updateStatus(int id, DriverStatus status) {
            Driver old = findById(id);
            if (old == null) {
                return false;
            }
            Driver updated = new Driver(id, old.getNationalId(), old.getName(), old.getLastName(),
                    old.getLicenseNumber(), old.getPhone(), status, old.getAssignedRouteId());
            drivers.set(drivers.indexOf(old), updated);
            return true;
        }

        @Override
        public boolean deleteById(int id) throws SQLException {
            if (failOnDelete != null) {
                throw failOnDelete;
            }
            return drivers.removeIf(d -> d.getId() == id);
        }

        @Override
        public boolean existsByNationalId(String nationalId) {
            return drivers.stream().anyMatch(d -> d.getNationalId().equals(nationalId));
        }

        @Override
        public boolean existsByLicenseNumber(String licenseNumber) {
            return drivers.stream().anyMatch(d -> licenseNumber.equals(d.getLicenseNumber()));
        }

        @Override
        public boolean hasVehicles(int driverId) {
            return driversWithVehicles.contains(driverId);
        }
    }

    /** Solo existen las rutas 1 y 2. */
    private static final class FakeRouteService extends RouteService {
        FakeRouteService() {
            super(null, null);
        }

        @Override
        public boolean routeExists(int routeId) {
            return Set.of(1, 2).contains(routeId);
        }
    }
}
