package com.vektra.model;

/**
 * Estado de un conductor. Reemplaza los textos sueltos "ACTIVE" e
 * "INACTIVE": en la base de datos se guarda el nombre de la constante y la
 * restricción chk_drivers_status acepta exactamente estos dos valores.
 */
public enum DriverStatus {

    ACTIVE("Activo"),
    INACTIVE("Inactivo");

    /** Nombre del estado que ve el usuario. */
    private final String displayName;

    DriverStatus(String displayName) {
        this.displayName = displayName;
    }

    /** @return nombre del estado en español, por ejemplo "Activo" */
    public String getDisplayName() {
        return displayName;
    }

    /** @return el estado contrario, para el botón de activar o desactivar */
    public DriverStatus toggle() {
        return this == ACTIVE ? INACTIVE : ACTIVE;
    }

    /**
     * Convierte el valor guardado en la base de datos en un estado.
     *
     * @param code valor de la columna status, por ejemplo "ACTIVE"
     * @return estado correspondiente
     * @throws IllegalArgumentException si el valor no corresponde a ningún estado
     */
    public static DriverStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El conductor no tiene estado.");
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El estado '" + code + "' no existe.", e);
        }
    }
}
