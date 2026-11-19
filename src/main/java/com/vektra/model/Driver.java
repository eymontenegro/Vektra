package com.vektra.model;
import java.util.Objects;
/**
 * Conductor del sistema. Lo registra el administrador y no inicia sesión.
 * Guarda el id de su ruta asignada, no el texto de la ruta.
 */
public class Driver extends Person {
    private final String licenseNumber;
    private final String phone;
    private final DriverStatus status;
    private final int assignedRouteId;
    /**
     * @param id              id en la base de datos (0 si aún no se guarda)
     * @param nationalId      cédula, como texto
     * @param name            nombres
     * @param lastName        apellidos
     * @param licenseNumber   número de licencia de conducción (puede ser null)
     * @param phone           teléfono de contacto (puede ser null)
     * @param status          estado del conductor, obligatorio
     * @param assignedRouteId id de la ruta asignada
     * @throws NullPointerException si status es null
     */
    public Driver(int id, String nationalId, String name, String lastName,
                  String licenseNumber, String phone,
                  DriverStatus status, int assignedRouteId) {
        super(id, nationalId, name, lastName);
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.status = Objects.requireNonNull(status, "status no puede ser nulo");
        this.assignedRouteId = assignedRouteId;
}
@Override
public Role getRole() {
return Role.DRIVER;
}
/** @return número de licencia de conducción, o null si no tiene */
public String getLicenseNumber() {
return licenseNumber;
}
/** @return teléfono de contacto, o null si no tiene */
public String getPhone() {
return phone;
}
/** @return estado del conductor */
public DriverStatus getStatus() {
return status;
}
/** @return true si el conductor está activo */
public boolean isActive() {
return status == DriverStatus.ACTIVE;
}
/** @return id de la ruta asignada */
public int getAssignedRouteId() {
return assignedRouteId;
}
@Override
public String toString() {
return "Driver{id=" + getId() + ", name=" + getFullName() + ", status=" + status + "}";
}
}