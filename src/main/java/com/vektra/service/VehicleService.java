package com.vektra.service;
import com.vektra.dao.VehicleDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Driver;
import com.vektra.model.LineColor;
import com.vektra.model.Vehicle;
import com.vektra.security.AccessControl;
import com.vektra.security.SessionUser;
import com.vektra.util.InputValidator;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Reglas de negocio de los vehículos. Los controladores solo hablan con esta clase.
 *
 * Todas las operaciones son del administrador: cada método recibe quién lo
 * pide y comprueba el permiso. Lanza ValidationException si los datos no son
 * válidos, AuthorizationException si no hay permiso y DataAccessException si
 * falla la base de datos.
 */
public class VehicleService {
    /** Largo máximo de la placa, igual que la columna plate. */
    private static final int MAX_PLATE_LENGTH = 15;
    /** Largo máximo de marca y modelo, igual que las columnas brand y model. */
    private static final int MAX_BRAND_MODEL_LENGTH = 60;
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String CHECK_VIOLATION = "23514";
    private static final Logger LOGGER = Logger.getLogger(VehicleService.class.getName());
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
     * Lista los vehículos registrados con su conductor.
     *
     * @param actor usuario de la sesión
     * @return vehículos ordenados por placa
     */
    public List<Vehicle> getVehicles(SessionUser actor) {
        AccessControl.requireFleetManager(actor);
        try {
            return vehicleDao.findAll();
        } catch (SQLException e) {
            throw failure("No se pudo cargar la lista de vehículos.", e);
        }
    }
    /**
     * Busca un vehículo por su id.
     *
     * @param actor     usuario de la sesión
     * @param vehicleId id del vehículo
     * @return el vehículo
     * @throws NotFoundException si no existe
     */
    public Vehicle getVehicle(SessionUser actor, int vehicleId) {
        AccessControl.requireFleetManager(actor);
        try {
            Vehicle vehicle = vehicleDao.findById(vehicleId);
            if (vehicle == null) {
                throw new NotFoundException("El vehículo seleccionado no existe.");
            }
            return vehicle;
        } catch (SQLException e) {
            throw failure("No se pudo cargar el vehículo.", e);
        }
    }
    /**
     * Registra un vehículo nuevo.
     *
     * @param actor    usuario de la sesión
     * @param plate    placa, obligatoria y única
     * @param brand    marca; si viene vacía se guarda sin marca
     * @param model    modelo; si viene vacío se guarda sin modelo
     * @param year     año de fabricación válido
     * @param capacity capacidad mayor que cero
     * @param line     línea en la que circula
     * @param driver   conductor existente
     * @return el vehículo guardado, con su id
     */
    public Vehicle addVehicle(SessionUser actor, String plate, String brand, String model, int year,
            int capacity, LineColor line, Driver driver) {
        AccessControl.requireFleetManager(actor);
        Vehicle vehicle = buildVehicle(0, plate, brand, model, year, capacity, line, driver);
        try {
            requireDriverExists(driver);
            if (vehicleDao.existsByPlate(vehicle.getPlate())) {
                throw new ValidationException("Ya existe un vehículo con esa placa.");
            }
            if (!vehicleDao.save(vehicle)) {
                throw new DataAccessException("No se pudo guardar el vehículo. Intenta de nuevo.", null);
            }
            return vehicle;
        } catch (SQLException e) {
            throw translate(e, "No se pudo guardar el vehículo.");
        }
    }
    /**
     * Cambia los datos de un vehículo existente.
     *
     * @param actor     usuario de la sesión
     * @param vehicleId id del vehículo a editar
     * @param plate     placa, obligatoria y única
     * @param brand     marca; si viene vacía se guarda sin marca
     * @param model     modelo; si viene vacío se guarda sin modelo
     * @param year      año de fabricación válido
     * @param capacity  capacidad mayor que cero
     * @param line      línea en la que circula
     * @param driver    conductor existente
     * @throws NotFoundException si el vehículo no existe
     */
    public void updateVehicle(SessionUser actor, int vehicleId, String plate, String brand, String model,
            int year, int capacity, LineColor line, Driver driver) {
        Vehicle current = getVehicle(actor, vehicleId);
        Vehicle vehicle = buildVehicle(vehicleId, plate, brand, model, year, capacity, line, driver);
        try {
            requireDriverExists(driver);
            boolean plateChanged = !current.getPlate().equals(vehicle.getPlate());
            if (plateChanged && vehicleDao.existsByPlate(vehicle.getPlate())) {
                throw new ValidationException("Ya existe un vehículo con esa placa.");
            }
            if (!vehicleDao.updateVehicle(vehicle)) {
                throw new NotFoundException("El vehículo ya no existe. Actualiza la lista.");
            }
        } catch (SQLException e) {
            throw translate(e, "No se pudo actualizar el vehículo.");
        }
    }
    /**
     * Borra un vehículo.
     *
     * @param actor     usuario de la sesión
     * @param vehicleId id del vehículo
     * @throws NotFoundException si el vehículo ya no existe
     */
    public void deleteVehicle(SessionUser actor, int vehicleId) {
        AccessControl.requireFleetManager(actor);
        try {
            if (!vehicleDao.deleteVehicle(vehicleId)) {
                throw new NotFoundException("El vehículo ya no existe. Actualiza la lista.");
            }
        } catch (SQLException e) {
            throw failure("No se pudo borrar el vehículo.", e);
        }
    }
    /**
     * Valida los datos y arma el vehículo. El año, la capacidad, la línea y el
     * conductor los valida además el constructor de Vehicle.
     */
    private Vehicle buildVehicle(int id, String plate, String brand, String model, int year,
            int capacity, LineColor line, Driver driver) {
        String cleanPlate = InputValidator.requireText(plate, "La placa es obligatoria.")
                .toUpperCase(Locale.ROOT);
        InputValidator.requireMaxLength(cleanPlate, MAX_PLATE_LENGTH,
                "La placa no puede tener más de " + MAX_PLATE_LENGTH + " caracteres.");
        String cleanBrand = InputValidator.requireMaxLength(InputValidator.optionalText(brand),
                MAX_BRAND_MODEL_LENGTH, "La marca no puede tener más de " + MAX_BRAND_MODEL_LENGTH + " 
caracteres.");
        String cleanModel = InputValidator.requireMaxLength(InputValidator.optionalText(model),
                MAX_BRAND_MODEL_LENGTH, "El modelo no puede tener más de " + MAX_BRAND_MODEL_LENGTH + " 
caracteres.");
        try {
            return new Vehicle(id, cleanPlate, cleanBrand, cleanModel, year, capacity, line, driver);
        } catch (IllegalArgumentException e) {
            // Vehicle valida año, capacidad, línea y conductor con mensajes en español.
            throw new ValidationException(e.getMessage(), e);
        }
    }
    /** Comprueba que el conductor elegido todavía exista. */
    private void requireDriverExists(Driver driver) throws SQLException {
        if (!vehicleDao.driverExists(driver.getId())) {
            throw new ValidationException("El conductor seleccionado no existe.");
        }
    }
    /** Convierte los errores de restricciones en mensajes claros. */
    private RuntimeException translate(SQLException e, String message) {
        if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
            return new ValidationException("Ya existe un vehículo con esa placa.", e);
        }
        if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
            return new ValidationException("El conductor seleccionado no existe.", e);
        }
        if (CHECK_VIOLATION.equals(e.getSQLState())) {
            // Por ejemplo, el trigger de la línea del vehículo si no es la de la ruta del conductor.
            return new ValidationException(
                    "Los datos del vehículo no cumplen una regla de la base de datos. "
                            + "Revisa que la línea coincida con la ruta del conductor.", e);
        }
        return failure(message, e);
    }
    /** Registra el error técnico y devuelve uno con mensaje para el usuario. */
    private DataAccessException failure(String message, SQLException e) {
        LOGGER.log(Level.SEVERE, "Fallo de base de datos en VehicleService", e);
        return new DataAccessException(message + " Revisa la conexión con la base de datos.", e);
    }
}