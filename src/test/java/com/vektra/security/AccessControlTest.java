package com.vektra.security;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.vektra.exception.AuthorizationException;
import com.vektra.model.Role;
import org.junit.jupiter.api.Test;
/** Pruebas de los permisos por rol. */
class AccessControlTest {
    private static final SessionUser ADMIN = new SessionUser(1, "Admin", "admin@vektra.com", Role.ADMIN);
    private static final SessionUser PASSENGER = new SessionUser(2, "Ana Ruiz", "ana@correo.com", Role.PASSENGER);
    @Test
    void adminCanManageFleetAndDashboard() {
        assertDoesNotThrow(() -> AccessControl.requireFleetManager(ADMIN));
        assertDoesNotThrow(() -> AccessControl.requireDashboardViewer(ADMIN));
    }
    @Test
    void adminCannotBuyTickets() {
        assertThrows(AuthorizationException.class, () -> AccessControl.requirePassenger(ADMIN));
    }
    @Test
    void passengerCannotManageFleetOrDashboard() {
        assertThrows(AuthorizationException.class, () -> AccessControl.requireFleetManager(PASSENGER));
        assertThrows(AuthorizationException.class, () -> AccessControl.requireDashboardViewer(PASSENGER));
        assertDoesNotThrow(() -> AccessControl.requirePassenger(PASSENGER));
    }
    @Test
    void missingSessionIsRejected() {
        assertThrows(AuthorizationException.class, () -> AccessControl.requireFleetManager(null));
        assertThrows(AuthorizationException.class, () -> AccessControl.requirePassenger(null));
    }
}