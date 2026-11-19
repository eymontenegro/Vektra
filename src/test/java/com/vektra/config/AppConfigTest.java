package com.vektra.config;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
/** Pruebas del orden de búsqueda de la configuración. */
class AppConfigTest {
    @Test
    void environmentVariableWinsOverFile() {
        Properties file = new Properties();
        file.setProperty("db.url", "jdbc:postgresql://archivo/Vektra");
        AppConfig config = new AppConfig(file, Map.of("DB_URL", "jdbc:postgresql://entorno/Vektra"));
        assertEquals("jdbc:postgresql://entorno/Vektra", config.require("db.url"));
    }
    @Test
    void fileIsUsedWhenEnvironmentIsMissing() {
        Properties file = new Properties();
        file.setProperty("db.user", "  vektra_app  ");
        AppConfig config = new AppConfig(file, Map.of());
        assertEquals("vektra_app", config.require("db.user"));
    }
    @Test
    void missingRequiredValueExplainsWhatToDo() {
        AppConfig config = new AppConfig(new Properties(), Map.of());
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> config.require("db.password"));
        assertTrue(error.getMessage().contains("DB_PASSWORD"));
    }
    @Test
    void numbersUseDefaultAndRejectText() {
        Properties file = new Properties();
        file.setProperty("server.port", "ocho");
        AppConfig config = new AppConfig(file, Map.of());
        assertEquals(7070, new AppConfig(new Properties(), Map.of()).getInt("server.port", 7070));
        assertThrows(IllegalStateException.class, () -> config.getInt("server.port", 7070));
    }
}