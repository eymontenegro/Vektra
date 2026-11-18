package com.vektra.security;
import com.vektra.exception.AuthorizationException;
/**
 * Comprobaciones de permisos que usan los servicios.
 *
 * Antes DriverService y VehicleService tenían cada uno su propio
 * requireAdmin() que leía la sesión estática. Ahora cada operación recibe
 * quién la pide (SessionUser) y la comprobación vive aquí.
 */
public final class AccessControl {
    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private AccessControl() {
    }
    /**
     * Exige que haya una sesión iniciada.
     *
     * @param actor usuario de la sesión; puede ser null
     * @return el mismo usuario, ya comprobado
     * @throws AuthorizationException si no hay sesión
     */
    public static SessionUser requireLoggedIn(SessionUser actor) {
        if (actor == null) {
            throw new AuthorizationException("Debes iniciar sesión para continuar.");
        }
        return actor;
    }
    /**
     * Exige permiso para gestionar conductores y vehículos.
     *
     * @param actor usuario de la sesión
     * @throws AuthorizationException si no tiene el permiso
     */
    public static void requireFleetManager(SessionUser actor) {
        if (!requireLoggedIn(actor).canManageFleet()) {
            throw new AuthorizationException("Solo el administrador puede gestionar conductores y vehículos.");
        }
    }
    /**
     * Exige permiso para ver el dashboard.
     *
     * @param actor usuario de la sesión
     * @throws AuthorizationException si no tiene el permiso
     */
    public static void requireDashboardViewer(SessionUser actor) {
        if (!requireLoggedIn(actor).canViewDashboard()) {
            throw new AuthorizationException("Solo el administrador puede ver el dashboard.");
        }
    }
    /**
     * Exige que la sesión sea de un pasajero.
     *
     * @param actor usuario de la sesión
     * @throws AuthorizationException si no es pasajero
     */
    public static void requirePassenger(SessionUser actor) {
        if (!requireLoggedIn(actor).canBuyTickets()) {
throw new AuthorizationException("Inicia sesión como pasajero para usar los tickets.");
}
}
}