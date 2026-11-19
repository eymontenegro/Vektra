package com.vektra.web;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
/** Pruebas de las rutas públicas. */
class AccessFilterTest {
    @Test
    void loginRegisterAndStaticFilesArePublic() {
        assertTrue(AccessFilter.isPublic("/login"));
        assertTrue(AccessFilter.isPublic("/register"));
        assertTrue(AccessFilter.isPublic("/css/vektra.css"));
        assertTrue(AccessFilter.isPublic("/images/vektra-logo.png"));
    }
    @Test
    void applicationPagesNeedSession() {
        assertFalse(AccessFilter.isPublic("/map"));
        assertFalse(AccessFilter.isPublic("/admin/drivers"));
        assertFalse(AccessFilter.isPublic("/tickets/buy"));
        assertFalse(AccessFilter.isPublic("/api/map"));
    }
}
