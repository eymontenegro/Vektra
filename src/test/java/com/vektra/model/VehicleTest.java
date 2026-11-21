package com.vektra.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Year;
import org.junit.jupiter.api.Test;

/** Pruebas de las validaciones del constructor de Vehicle y de DriverStatus. */
class VehicleTest {

    private final Driver driver = new Driver(1, "1001234567", "Carlos", "Pérez",
            "LIC-0001", "3001112233", DriverStatus.ACTIVE, 1);

    @Test
    void validVehicleKeepsItsData() {
        Vehicle vehicle = new Vehicle(0, "VKT-003", "CAF", "Urbos", 2022, 250, LineColor.BLUE, driver);
        assertEquals("Carlos Pérez", vehicle.getDriverName());
        assertEquals("Azul", vehicle.getLineName());
    }

    @Test
    void capacityMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> new Vehicle(0, "VKT-003", null, null, 2022, 0, LineColor.BLUE, driver));
    }

    @Test
    void yearCannotBeInTheFarFuture() {
        int tooLate = Year.now().getValue() + 2;
        assertThrows(IllegalArgumentException.class,
                () -> new Vehicle(0, "VKT-003", null, null, tooLate, 10, LineColor.BLUE, driver));
    }

    @Test
    void driverIsRequired() {
        assertThrows(IllegalArgumentException.class,
                () -> new Vehicle(0, "VKT-003", null, null, 2022, 10, LineColor.BLUE, null));
    }

    @Test
    void driverStatusTogglesAndParses() {
        assertEquals(DriverStatus.INACTIVE, DriverStatus.ACTIVE.toggle());
        assertEquals(DriverStatus.ACTIVE, DriverStatus.fromCode(" active "));
        assertThrows(IllegalArgumentException.class, () -> DriverStatus.fromCode("VACATION"));
    }
}
