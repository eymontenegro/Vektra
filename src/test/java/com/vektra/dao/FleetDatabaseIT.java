package com.vektra.dao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.vektra.model.Driver;
import com.vektra.model.Vehicle;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
/**
* Pruebas de integración de conductores y vehículos contra PostgreSQL con
* los datos de prueba de DataBase/vektra.sql (versión 2).
* Solo se ejecutan con: mvn test -Pdatabase-tests. Son de solo lectura.
*/
@Tag("database")
class FleetDatabaseIT {
@Test
void driversAreReadWithTheirStatus() throws Exception {
List<Driver> drivers = new DriverDao().findAll();
assertEquals(2, drivers.size());
assertNotNull(drivers.get(0).getStatus());
}
@Test
void findByIdReturnsNullForUnknownDriver() throws Exception {
assertNull(new DriverDao().findById(-1));
}
@Test
void vehiclesComeWithTheirDriver() throws Exception {
List<Vehicle> vehicles = new VehicleDao().findAll();
assertEquals(2, vehicles.size());
assertNotNull(vehicles.get(0).getDriver().getFullName());
}
}
