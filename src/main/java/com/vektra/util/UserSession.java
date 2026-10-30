package com.vektra.util;

import com.vektra.model.Passenger;
import com.vektra.model.Role;
import com.vektra.model.User;

/**
 * Guarda quién inició sesión: un pasajero o el administrador.
 *
 * Solo hay una sesión a la vez. Al iniciar una nueva se descarta la anterior.
 * Si se pide un dato y no hay sesión de ese tipo, se lanza una excepción en
 * lugar de devolver null, así un olvido se nota de inmediato.
 */
public final class UserSession {

    /** Pasajero con sesión iniciada, o null si no lo hay. */
    private static Passenger activePassenger;

    /** Administrador con sesión iniciada, o null si no lo hay. */
    private static User activeAdmin;

    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private UserSession() {
    }

    /**
     * Inicia la sesión de un pasajero.
     *
     * @param passenger pasajero que inició sesión, obligatorio
     * @throws IllegalArgumentException si el pasajero es null
     */
    public static void startPassengerSession(Passenger passenger) {
        if (passenger == null) {
            throw new IllegalArgumentException("No se puede iniciar sesión sin un pasajero.");
        }
        close();
        activePassenger = passenger;
    }

    /**
     * Inicia la sesión del administrador.
     *
     * @param admin cuenta del administrador, obligatoria y con rol ADMIN
     * @throws IllegalArgumentException si es null o no tiene rol ADMIN
     */
    public static void startAdminSession(User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("La cuenta no es de un administrador.");
        }
        close();
        activeAdmin = admin;
    }

    /**
     * Pasajero con sesión iniciada.
     *
     * @return pasajero activo
     * @throws IllegalStateException si no hay un pasajero con sesión
     */
    public static Passenger getActivePassenger() {
        if (activePassenger == null) {
            throw new IllegalStateException("No hay un pasajero con sesión iniciada.");
        }
        return activePassenger;
    }

    /**
     * Administrador con sesión iniciada.
     *
     * @return administrador activo
     * @throws IllegalStateException si no hay un administrador con sesión
     */
    public static User getActiveAdmin() {
        if (activeAdmin == null) {
            throw new IllegalStateException("No hay un administrador con sesión iniciada.");
        }
        return activeAdmin;
    }

    /**
     * Rol de quien tiene la sesión iniciada.
     *
     * @return ADMIN o PASSENGER
     * @throws IllegalStateException si no hay sesión
     */
    public static Role getRole() {
        if (activePassenger != null) {
            return activePassenger.getRole();
        }
        if (activeAdmin != null) {
            return activeAdmin.getRole();
        }
        throw new IllegalStateException("No hay una sesión iniciada.");
    }

    /**
     * Indica si hay alguien con sesión iniciada.
     *
     * @return true si hay un pasajero o un administrador
     */
    public static boolean isLoggedIn() {
        return activePassenger != null || activeAdmin != null;
    }

    /** Cierra la sesión y olvida al usuario. */
    public static void close() {
        activePassenger = null;
        activeAdmin = null;
    }
}