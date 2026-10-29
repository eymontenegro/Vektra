package com.vektra.model;

/**
 * Conductor del sistema. Lo registra el administrador y no inicia sesión.
 * Guarda el id de su ruta asignada, no el texto de la ruta.
 */
public class Driver extends Person {

    private final String licenseNumber;
    private final String phone;
    private final String status;
    private final int assignedRouteId;

    /**
     * @param id              id en la base de datos (0 si aún no se guarda)
     * @param nationalId      cédula, como texto
     * @param name            nombres
     * @param lastName        apellidos
     * @param licenseNumber   número de licencia de conducción
     * @param phone           teléfono de contacto
     * @param status          estado del conductor
     * @param assignedRouteId id de la ruta asignada
     */
    public Driver(int id, String nationalId, String name, String lastName,
                  String licenseNumber, String phone,
                  String status, int assignedRouteId) {
        super(id, nationalId, name, lastName);
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.status = status;
        this.assignedRouteId = assignedRouteId;
    }

    @Override
    public Role getRole() {
        return Role.DRIVER;
    }

    /** @return número de licencia de conducción */
    public String getLicenseNumber() {
        return licenseNumber;
    }

    /** @return teléfono de contacto */
    public String getPhone() {
        return phone;
    }

    /** @return estado del conductor */
    public String getStatus() {
        return status;
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