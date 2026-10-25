package com.vektra.connection;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Única forma de abrir una conexión con la base de datos PostgreSQL.
 *
 * Lee la URL, el usuario y la clave desde el archivo config.properties o,
 * si el archivo no existe, desde las variables de entorno DB_URL, DB_USER
 * y DB_PASSWORD. Cada llamada devuelve una conexión nueva, y quien la pide
 * debe cerrarla con try-with-resources.
 */
public final class DatabaseConnection {

    /** Nombre del archivo donde cada integrante guarda sus datos de conexión. */
    private static final String CONFIG_FILE = "config.properties";

    /** Datos leídos del archivo de configuración, cargados una sola vez. */
    private static final Properties CONFIG = loadConfig();

    /** Constructor privado: esta clase solo tiene métodos estáticos y no se instancia. */
    private DatabaseConnection() {
    }

    /**
     * Abre una conexión nueva con la base de datos.
     *
     * @return conexión abierta, nunca null
     * @throws SQLException si falta algún dato de configuración o no se
     *                      puede conectar con la base de datos
     */
    public static Connection getConnection() throws SQLException {
        String url = readSetting("db.url", "DB_URL");
        String user = readSetting("db.user", "DB_USER");
        String password = readSetting("db.password", "DB_PASSWORD");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Carga config.properties. Si el archivo no existe devuelve una
     * configuración vacía, y los datos se buscan en las variables de entorno.
     */
    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            // Sin archivo: se usan las variables de entorno.
        }
        return properties;
    }

    /**
     * Busca un dato primero en config.properties y luego en el entorno.
     *
     * @param key             nombre del dato en config.properties
     * @param environmentName nombre de la variable de entorno equivalente
     * @return valor encontrado, sin espacios sobrantes
     * @throws SQLException si el dato no está en ninguno de los dos lugares
     */
    private static String readSetting(String key, String environmentName) throws SQLException {
        String value = CONFIG.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(environmentName);
        }
        if (value == null || value.isBlank()) {
            throw new SQLException("Falta la configuración '" + key
                    + "'. Revise el archivo config.properties.");
        }
        return value.trim();
    }
}