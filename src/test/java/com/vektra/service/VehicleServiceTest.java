package com.vektra.service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.vektra.dao.VehicleDao;
import com.vektra.exception.AuthorizationException;
import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Driver;
import com.vektra.model.DriverStatus;
import com.vektra.model.LineColor;
import com.vektra.model.Role;
import com.vektra.model.Vehicle;
import com.vektra.security.SessionUser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/** Pruebas de las reglas de los vehículos, con un DAO falso. */
class VehicleServiceTest {
private static final SessionUser ADMIN = new SessionUser(1, "Administrador", "admin@vektra.com", Role.ADMIN);
private static final SessionUser PASSENGER = new SessionUser(2, "Ana", "ana@correo.com", Role.PASSENGER);
private static final Driver CARLOS = new Driver(1, "1001234567", "Carlos", "Pérez",
            "LIC-0001", "3001112233", DriverStatus.ACTIVE, 1);
    private FakeVehicleDao dao;
    private VehicleService service;
    @BeforeEach
    void setUp() {
        dao = new FakeVehicleDao();
        service = new VehicleService(dao);
    }
    @Test
    void addVehicleUppercasesPlateAndCleansOptionalFields() {
        Vehicle vehicle = service.addVehicle(ADMIN, " vkt-010 ", " ", "Urbos", 2022, 250, LineColor.RED, CARLOS);
        assertEquals("VKT-010", vehicle.getPlate());
        assertNull(vehicle.getBrand());
        assertEquals(1, dao.vehicles.size());
    }
    @Test
    void passengerCannotAddVehicles() {
        assertThrows(AuthorizationException.class,
                () -> service.addVehicle(PASSENGER, "VKT-010", null, null, 2022, 250, LineColor.RED, CARLOS));
    }
    @Test
    void duplicatedPlateIsRejected() {
        service.addVehicle(ADMIN, "VKT-010", null, null, 2022, 250, LineColor.RED, CARLOS);
        assertThrows(ValidationException.class,
                () -> service.addVehicle(ADMIN, "vkt-010", null, null, 2022, 250, LineColor.RED, CARLOS));
    }
    @Test
    void invalidCapacityBecomesValidationError() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.addVehicle(ADMIN, "VKT-010", null, null, 2022, 0, LineColor.RED, CARLOS));
        assertEquals("La capacidad del vehículo debe ser mayor que cero.", error.getMessage());
    }
    @Test
    void checkViolationFromTriggerBecomesValidationError() {
        dao.failOnSave = new SQLException("línea distinta", "23514");
        assertThrows(ValidationException.class,
                () -> service.addVehicle(ADMIN, "VKT-010", null, null, 2022, 250, LineColor.YELLOW, CARLOS));
    }
    @Test
    void updateKeepsPlateAndChangesCapacity() {
        Vehicle vehicle = service.addVehicle(ADMIN, "VKT-010", null, null, 2022, 250, LineColor.RED, CARLOS);
        service.updateVehicle(ADMIN, vehicle.getId(), "VKT-010", "CAF", null, 2022, 300, LineColor.RED, CARLOS);
        assertEquals(300, dao.findById(vehicle.getId()).getCapacity());
    }
    @Test
    void updatingUnknownVehicleIsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.updateVehicle(ADMIN, 404, "VKT-010", null, null, 2022, 250, LineColor.RED, CARLOS));
    }
    /** Guarda los vehículos en una lista; el único conductor que existe es el 1. */
    private static final class FakeVehicleDao extends VehicleDao {
        private final List<Vehicle> vehicles = new ArrayList<>();
        private SQLException failOnSave;
        @Override
        public List<Vehicle> findAll() {
            return List.copyOf(vehicles);
        }
        @Override
        public Vehicle findById(int id) {
            return vehicles.stream().filter(v -> v.getId() == id).findFirst().orElse(null);
        }
        @Override
        public boolean save(Vehicle vehicle) throws SQLException {
            if (failOnSave != null) {
                throw failOnSave;
            }
            vehicle.setId(vehicles.size() + 1);
            vehicles.add(vehicle);
            return true;
        }
        @Override
        public boolean updateVehicle(Vehicle vehicle) {
            Vehicle old = findById(vehicle.getId());
            if (old == null) {
                return false;
            }
            vehicles.set(vehicles.indexOf(old), vehicle);
            return true;
        }
        @Override
        public boolean deleteVehicle(int id) {
            return vehicles.removeIf(v -> v.getId() == id);
        }
        @Override
        public boolean existsByPlate(String plate) {
            return vehicles.stream().anyMatch(v -> v.getPlate().equals(plate));
        }
        @Override
        public boolean driverExists(int driverId) {
            return driverId == 1;
        }
    }
}
