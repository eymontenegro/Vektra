package com.vektra.security;
import com.vektra.model.Passenger;
import com.vektra.model.Role;
import com.vektra.model.User;
import java.io.Serializable;
import java.util.Objects;
/**
 * Persona que inició sesión, tal como se guarda en la sesión HTTP.
 *
 * Reemplaza a la clase estática UserSession: en una aplicación web cada
 * navegador tiene su propia sesión, así que el usuario ya no puede vivir en
 * una variable estática compartida. No guarda el hash de la contraseña.
 *
 * @param id    id en la tabla users
 * @param name  nombre que se muestra en pantalla
 * @param email correo de la cuenta
 * @param role  rol de la cuenta (ADMIN o PASSENGER)
 */
public record SessionUser(int id, String name, String email, Role role) implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Valida los datos obligatorios.
     *
     * @throws NullPointerException si falta el correo o el rol
     */
    public SessionUser {
        Objects.requireNonNull(email, "email no puede ser nulo");
        Objects.requireNonNull(role, "role no puede ser nulo");
    }
    /**
     * Crea la sesión de un administrador.
     *
     * @param admin cuenta autenticada
     * @return usuario de sesión
     */
    public static SessionUser fromAdmin(User admin) {
        return new SessionUser(admin.getId(), admin.getName(), admin.getEmail(), admin.getRole());
    }
    /**
     * Crea la sesión de un pasajero.
     *
     * @param passenger pasajero autenticado
     * @return usuario de sesión
     */
    public static SessionUser fromPassenger(Passenger passenger) {
        return new SessionUser(passenger.getId(), passenger.getFullName(),
                passenger.getEmail(), passenger.getRole());
    }
    /** @return true si puede gestionar conductores y vehículos */
    public boolean canManageFleet() {
        return role.canManageFleet();
    }
    /** @return true si puede comprar tickets */
    public boolean canBuyTickets() {
        return role.canBuyTickets();
    }
    /** @return true si puede ver el dashboard */
    public boolean canViewDashboard() {
        return role.canViewDashboard();
    }
}