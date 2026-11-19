package com.vektra.service;
import com.vektra.dao.DriverDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.NotFoundException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Driver;
import com.vektra.model.DriverStatus;
import com.vektra.security.AccessControl;
import com.vektra.security.SessionUser;
import com.vektra.util.InputValidator;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Reglas de negocio de los conductores. Los controladores solo hablan con esta clase.
 *
 * Todas las operaciones son del administrador: cada método recibe quién
 * lo pide y vuelve a comprobar el permiso, porque ocultar un botón no es
 * seguridad. Lanza ValidationException si los datos no son válidos,
 * AuthorizationException si no hay permiso y DataAccessException si falla
 * la base de datos.
 */
public class DriverService {
    /** Largo máximo de la licencia, igual que la columna license_number. */
    private static final int MAX_LICENSE_LENGTH = 50;
    /** Largo máximo de nombres y apellidos, igual que las columnas de drivers. */
    private static final int MAX_NAME_LENGTH = 100;
    private static final String UNIQUE_VIOLATION = "23505";
    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final Logger LOGGER = Logger.getLogger(DriverService.class.getName());
    /** Acceso a los conductores en la base de datos. */
    private final DriverDao driverDao;
    /** Se usa para comprobar que la ruta asignada existe. */
    private final RouteService routeService;
    /**
     * Crea el servicio con sus dependencias.
     *
     * @param driverDao    DAO de los conductores
     * @param routeService servicio de rutas, para validar la ruta asignada
     */
    public DriverService(DriverDao driverDao, RouteService routeService) {
        this.driverDao = driverDao;
        this.routeService = routeService;
    }
    /**
     * Lista los conductores registrados.
     *
     * @param actor usuario de la sesión
     * @return conductores ordenados por apellido y nombre
     */
    public List<Driver> getDrivers(SessionUser actor) {
        AccessControl.requireFleetManager(actor);
        try {
            return driverDao.findAll();
        } catch (SQLException e) {
            throw failure("No se pudo cargar la lista de conductores.", e);
        }
    }
    /**
     * Busca un conductor por su id.
     *
     * @param actor    usuario de la sesión
     * @param driverId id del conductor
     * @return el conductor
     * @throws NotFoundException si no existe
     */
    public Driver getDriver(SessionUser actor, int driverId) {
        AccessControl.requireFleetManager(actor);
        try {
            Driver driver = driverDao.findById(driverId);
            if (driver == null) {
                throw new NotFoundException("El conductor seleccionado no existe.");
            }
            return driver;
        } catch (SQLException e) {
            throw failure("No se pudo cargar el conductor.", e);
        }
    }
    /**
     * Registra un conductor nuevo con estado activo.
     *
     * @param actor         usuario de la sesión
     * @param nationalId    cédula, obligatoria, 6 a 12 dígitos y única
     * @param name          nombres, obligatorios
     * @param lastName      apellidos, obligatorios
     * @param licenseNumber número de licencia; si viene vacío se guarda sin licencia
     * @param phone         teléfono de 7 a 15 dígitos; si viene vacío se guarda sin teléfono
     * @param routeId       id de una ruta existente
     * @return el conductor guardado, con su id
     */
    public Driver addDriver(SessionUser actor, String nationalId, String name, String lastName,
            String licenseNumber, String phone, int routeId) {
        AccessControl.requireFleetManager(actor);
        String cleanNationalId = InputValidator.requireText(nationalId, "La cédula es obligatoria.");
        String cleanName = InputValidator.requireText(name, "El nombre es obligatorio.");
        String cleanLastName = InputValidator.requireText(lastName, "El apellido es obligatorio.");
        String cleanLicense = InputValidator.optionalText(licenseNumber);
        String cleanPhone = InputValidator.optionalText(phone);
        InputValidator.requirePattern(cleanNationalId, InputValidator.NATIONAL_ID,
                "La cédula debe tener entre 6 y 12 dígitos, sin puntos ni espacios.");
        InputValidator.requireMaxLength(cleanName, MAX_NAME_LENGTH,
                "El nombre no puede tener más de " + MAX_NAME_LENGTH + " caracteres.");
        InputValidator.requireMaxLength(cleanLastName, MAX_NAME_LENGTH,
                "El apellido no puede tener más de " + MAX_NAME_LENGTH + " caracteres.");
        InputValidator.requireMaxLength(cleanLicense, MAX_LICENSE_LENGTH,
                "La licencia no puede tener más de " + MAX_LICENSE_LENGTH + " caracteres.");
        InputValidator.requirePattern(cleanPhone, InputValidator.PHONE,
                "El teléfono debe tener entre 7 y 15 dígitos, sin espacios ni guiones.");
        if (!routeService.routeExists(routeId)) {
            throw new ValidationException("La ruta seleccionada no existe.");
        }
        try {
            if (driverDao.existsByNationalId(cleanNationalId)) {
                throw new ValidationException("Ya existe un conductor con esa cédula.");
            }
            if (cleanLicense != null && driverDao.existsByLicenseNumber(cleanLicense)) {
                throw new ValidationException("Ya existe un conductor con ese número de licencia.");
            }
            Driver driver = new Driver(0, cleanNationalId, cleanName, cleanLastName,
                    cleanLicense, cleanPhone, DriverStatus.ACTIVE, routeId);
            if (!driverDao.save(driver)) {
                throw new DataAccessException("No se pudo guardar el conductor. Intenta de nuevo.", null);
            }
            return driver;
        } catch (SQLException e) {
            if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
                throw new ValidationException("Ya existe un conductor con esa cédula o licencia.", e);
            }
            throw failure("No se pudo guardar el conductor.", e);
        }
    }
    /**
     * Activa un conductor inactivo o desactiva uno activo.
     *
     * @param actor    usuario de la sesión
     * @param driverId id del conductor
     * @return el estado nuevo
     * @throws NotFoundException si el conductor no existe
     */
    public DriverStatus toggleStatus(SessionUser actor, int driverId) {
        Driver driver = getDriver(actor, driverId);
        DriverStatus newStatus = driver.getStatus().toggle();
        try {
            if (!driverDao.updateStatus(driverId, newStatus)) {
                throw new NotFoundException("El conductor seleccionado no existe.");
            }
            return newStatus;
        } catch (SQLException e) {
            throw failure("No se pudo cambiar el estado del conductor.", e);
        }
    }
    /**
     * Borra un conductor. No se puede borrar si tiene vehículos asignados.
     *
     * @param actor    usuario de la sesión
     * @param driverId id del conductor
     * @throws ValidationException si el conductor tiene vehículos asignados
     * @throws NotFoundException   si el conductor ya no existe
     */
    public void deleteDriver(SessionUser actor, int driverId) {
        AccessControl.requireFleetManager(actor);
        try {
            if (driverDao.hasVehicles(driverId)) {
                throw new ValidationException("No se puede borrar: el conductor tiene vehículos asignados.");
            }
            if (!driverDao.deleteById(driverId)) {
                throw new NotFoundException("El conductor ya no existe. Actualiza la lista.");
            }
        } catch (SQLException e) {
            // Alguien pudo asignarle un vehículo entre la comprobación y el borrado.
            if (FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) {
                throw new ValidationException("No se puede borrar: el conductor tiene vehículos asignados.", e);
            }
            throw failure("No se pudo borrar el conductor.", e);
        }
    }
    /** Registra el error técnico y devuelve uno con mensaje para el usuario. */
    private DataAccessException failure(String message, SQLException e) {
        LOGGER.log(Level.SEVERE, "Fallo de base de datos en DriverService", e);
        return new DataAccessException(message + " Revisa la conexión con la base de datos.", e);
    }
}