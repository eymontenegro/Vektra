package com.vektra.service;

import com.vektra.dao.VehicleDao;
import com.vektra.model.Driver;
import com.vektra.model.LineColor;
import com.vektra.model.Vehicle;
import com.vektra.util.UserSession;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Reglas de negocio de los vehículos. Las vistas solo hablan con esta clase.
 *
 * Agregar, editar y borrar son del administrador: el Service vuelve a
 * comprobar el rol de la sesión, porque ocultar un botón no es seguridad.
 * Lanza IllegalArgumentException si los datos no son válidos y
 * SecurityException si el rol no tiene permiso.
 */
public class VehicleService {

    /** Largo máximo de la placa, igual que la columna plate. */
    private static final int MAX_PLATE_LENGTH = 15;

    /** Acceso a los vehículos en la base de datos. */
    private final VehicleDao vehicleDao;

    /**
     * Crea el servicio con el DAO que usará.
     *
     * @param vehicleDao DAO de los vehículos
     */
    public VehicleService(VehicleDao vehicleDao) {
        this.vehicleDao = vehicleDao;
    }

    /**
     * Registra un vehículo nuevo.
     *
     * @param plate    placa, obligatoria y única
     * @param brand    marca; si viene vacía se guarda sin marca
     * @param model    modelo; si viene vacío se guarda sin modelo
     * @param year     año de fabricación válido
     * @param capacity capacidad mayor que cero
     * @param line     línea en la que circula
     * @param driver   conductor existente
     * @return true si se guardó
     * @throws IllegalArgumentException si algún dato no es válido o la placa está repetida
     * @throws SecurityException        si quien tiene la sesión no es administrador
     * @throws SQLException             si falla la base de datos
     */
    public boolean addVehicle(String plate, String brand, String model, int year,
            int capacity, LineColor line, Driver driver) throws SQLException {
        requireAdmin();
        Vehicle vehicle = buildVehicle(0, plate, brand, model, year, capacity, line, driver);
        if (vehicleDao.existsByPlate(vehicle.getPlate())) {
            throw new IllegalArgumentException("Ya existe un vehículo con esa placa.");
        }
        return vehicleDao.save(vehicle);
    }

    /**
     * Cambia los datos de un vehículo existente.
     *
     * @param id       id del vehículo a editar
     * @param plate    placa, obligatoria y única
     * @param brand    marca; si viene vacía se guarda sin marca
     * @param model    modelo; si viene vacío se guarda sin modelo
     * @param year     año de fabricación válido
     * @param capacity capacidad mayor que cero
     * @param line     línea en la que circula
     * @param driver   conductor existente
     * @return true si se actualizó
     * @throws IllegalArgumentException si el vehículo no existe, algún dato no es válido
     *                                  o la placa nueva ya está en uso
     * @throws SecurityException        si quien tiene la sesión no es administrador
     * @throws SQLException             si falla la base de datos
     */
    public boolean updateVehicle(int id, String plate, String brand, String model,
            int year, int capacity, LineColor line, Driver driver) throws SQLException {
        requireAdmin();
        Vehicle current = vehicleDao.findById(id);
        if (current == null) {
            throw new IllegalArgumentException("El vehículo que se quiere editar no existe.");
        }
        Vehicle vehicle = buildVehicle(id, plate, brand, model, year, capacity, line, driver);
        boolean plateChanged = !current.getPlate().equals(vehicle.getPlate());
        if (plateChanged && vehicleDao.existsByPlate(vehicle.getPlate())) {
            throw new IllegalArgumentException("Ya existe un vehículo con esa placa.");
        }
        return vehicleDao.updateVehicle(vehicle);
    }

    /**
     * Borra un vehículo.
     *
     * @param id id del vehículo
     * @return true si se borró
     * @throws SecurityException si quien tiene la sesión no es administrador
     * @throws SQLException      si falla la base de datos
     */
    public boolean deleteVehicle(int id) throws SQLException {
        requireAdmin();
        return vehicleDao.deleteVehicle(id);
    }

    /**
     * Valida los datos y arma el vehículo. El año, la capacidad, la línea y
     * el conductor los comprueba el constructor de Vehicle.
     *
     * @return vehículo con la placa en mayúsculas y los textos limpios
     * @throws IllegalArgumentException si algún dato no es válido o el conductor no existe
     * @throws SQLException             si falla la consulta del conductor
     */
    private Vehicle buildVehicle(int id, String plate, String brand, String model, int year,
            int capacity, LineColor line, Driver driver) throws SQLException {
        String cleanPlate = requireText(plate, "La placa es obligatoria.").toUpperCase(Locale.ROOT);
        if (cleanPlate.length() > MAX_PLATE_LENGTH) {
            throw new IllegalArgumentException(
                    "La placa no puede tener más de " + MAX_PLATE_LENGTH + " caracteres.");
        }
        Vehicle vehicle = new Vehicle(id, cleanPlate, blankToNull(brand), blankToNull(model),
                year, capacity, line, driver);
        if (!vehicleDao.driverExists(driver.getId())) {
            throw new IllegalArgumentException("El conductor seleccionado no existe.");
        }
        return vehicle;
    }

    /**
     * Comprueba que quien tiene la sesión puede gestionar la flota.
     *
     * @throws SecurityException si no hay sesión o el rol no tiene permiso
     */
    private void requireAdmin() {
        if (!UserSession.isLoggedIn() || !UserSession.getRole().canManageFleet()) {
            throw new SecurityException("Solo el administrador puede gestionar vehículos.");
        }
    }

    /**
     * Exige un texto no vacío y lo devuelve sin espacios a los lados.
     *
     * @param value   texto recibido
     * @param message mensaje si está vacío
     * @return texto limpio
     * @throws IllegalArgumentException si es null o está vacío
     */
    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /**
     * Limpia un texto opcional.
     *
     * @param value texto recibido
     * @return texto sin espacios, o null si venía vacío
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}