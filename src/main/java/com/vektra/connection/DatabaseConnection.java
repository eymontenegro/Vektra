package com.vektra.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Única forma de abrir una conexión con la base de datos PostgreSQL.
 *
 * Lee la URL, el usuario y la clave desde el recurso config.properties del
 * classpath (src/main/resources), o desde un archivo config.properties en el
 * directorio de trabajo si no está en el classpath, o desde las variables de
 * entorno DB_URL, DB_USER y DB_PASSWORD si no hay archivo. Cada llamada
 * devuelve una conexión nueva, y quien la pide debe cerrarla con
 * try-with-resources.
 */
public final class DatabaseConnection {

    /** Nombre del archivo donde cada integrante guarda sus datos de conexión. */
    private static final String CONFIG_FILE = "config.properties";

    /** Datos leídos de la configuración, cargados una sola vez. */
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
     * Carga config.properties: primero como recurso del classpath (se
     * encuentra sin importar desde qué carpeta se ejecute la app) y, si no
     * está ahí, como archivo en el directorio de trabajo. Si no existe en
     * ninguno de los dos lugares devuelve una configuración vacía y los
     * datos se buscan en las variables de entorno.
     */
    private static Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream resource = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (resource != null) {
                properties.load(resource);
                return properties;
            }
        } catch (IOException e) {
            // Recurso ilegible: se intenta con el archivo del directorio.
        }
        File file = new File(CONFIG_FILE);
        if (file.isFile()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException e) {
                // Archivo ilegible: se usan las variables de entorno.
            }
        }
        return properties;
    }

    /**
     * Busca un dato primero en la configuración cargada y luego en el entorno.
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
                    + "'. Revise config.properties (src/main/resources o raíz del "
                    + "proyecto) o la variable de entorno " + environmentName + ".");
        }
        return value.trim();
    }
}
