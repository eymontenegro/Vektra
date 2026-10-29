package com.vektra.service;

import com.vektra.dao.DriverDao;
import com.vektra.model.Driver;
import com.vektra.util.UserSession;
import java.sql.SQLException;
import java.util.List;

/**
 * Reglas de negocio de los conductores. Los paneles solo hablan con esta clase.
 *
 * Todas las operaciones son del administrador: el Service vuelve a comprobar
 * el rol de la sesión, porque ocultar un botón no es seguridad. Lanza
 * IllegalArgumentException si los datos no son válidos y SecurityException si
 * el rol no tiene permiso.
 */
public class DriverService {

    /** Estado con el que se crea un conductor nuevo. */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** Largo máximo de la cédula, igual que la columna national_id. */
    private static final int MAX_NATIONAL_ID_LENGTH = 12;

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
     * @return conductores ordenados por apellido y nombre
     * @throws SecurityException si quien tiene la sesión no es administrador
     * @throws SQLException      si falla la consulta
     */
    public List<Driver> getDrivers() throws SQLException {
        requireAdmin();
        return driverDao.findAll();
    }

    /**
     * Registra un conductor nuevo con estado activo.
     *
     * @param nationalId    cédula, obligatoria y única
     * @param name          nombres, obligatorios
     * @param lastName      apellidos, obligatorios
     * @param licenseNumber número de licencia; si viene vacío se guarda sin licencia
     * @param phone         teléfono; si viene vacío se guarda sin teléfono
     * @param routeId       id de una ruta existente
     * @return true si se guardó
     * @throws IllegalArgumentException si algún dato no es válido o está repetido
     * @throws SecurityException        si quien tiene la sesión no es administrador
     * @throws SQLException             si falla la base de datos
     */
    public boolean addDriver(String nationalId, String name, String lastName,
            String licenseNumber, String phone, int routeId) throws SQLException {
        requireAdmin();
        String cleanNationalId = requireText(nationalId, "La cédula es obligatoria.");
        String cleanName = requireText(name, "El nombre es obligatorio.");
        String cleanLastName = requireText(lastName, "El apellido es obligatorio.");
        String cleanLicense = blankToNull(licenseNumber);
        String cleanPhone = blankToNull(phone);

        if (cleanNationalId.length() > MAX_NATIONAL_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "La cédula no puede tener más de " + MAX_NATIONAL_ID_LENGTH + " caracteres.");
        }
        if (driverDao.existsByNationalId(cleanNationalId)) {
            throw new IllegalArgumentException("Ya existe un conductor con esa cédula.");
        }
        if (cleanLicense != null && driverDao.existsByLicenseNumber(cleanLicense)) {
            throw new IllegalArgumentException("Ya existe un conductor con ese número de licencia.");
        }
        if (!routeExists(routeId)) {
            throw new IllegalArgumentException("La ruta seleccionada no existe.");
        }
        Driver driver = new Driver(0, cleanNationalId, cleanName, cleanLastName,
                cleanLicense, cleanPhone, STATUS_ACTIVE, routeId);
        return driverDao.save(driver);
    }

    /**
     * Borra un conductor. No se puede borrar si tiene vehículos asignados.
     *
     * @param id id del conductor
     * @return true si se borró
     * @throws IllegalArgumentException si el conductor tiene vehículos asignados
     * @throws SecurityException        si quien tiene la sesión no es administrador
     * @throws SQLException             si falla la base de datos
     */
    public boolean deleteDriver(int id) throws SQLException {
        requireAdmin();
        if (driverDao.hasVehicles(id)) {
            throw new IllegalArgumentException(
                    "No se puede borrar: el conductor tiene vehículos asignados.");
        }
        return driverDao.deleteById(id);
    }

    /**
     * Comprueba que quien tiene la sesión puede gestionar la flota.
     *
     * @throws SecurityException si no hay sesión o el rol no tiene permiso
     */
    private void requireAdmin() {
        if (!UserSession.isLoggedIn() || !UserSession.getRole().canManageFleet()) {
            throw new SecurityException("Solo el administrador puede gestionar conductores.");
        }
    }

    /**
     * Indica si existe una ruta con ese id.
     *
     * @param routeId id de la ruta
     * @return true si existe
     * @throws SQLException si falla la consulta de rutas
     */
    private boolean routeExists(int routeId) throws SQLException {
        return routeService.getRoutes().stream().anyMatch(route -> route.getId() == routeId);
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