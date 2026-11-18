package com.vektra.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Configuración única de Vektra.
 *
 * Reemplaza la lectura de config.properties que estaba copiada en
 * DatabaseConnection y en EmailService. Cada dato se busca en este orden:
 *   1. Variable de entorno (por ejemplo DB_URL para db.url).
 *   2. Archivo config.properties en la carpeta desde donde se ejecuta la
 *      aplicación (la raíz del proyecto, junto a pom.xml), o el archivo que
 *      indique la propiedad del sistema vektra.config.
 * El archivo NO se busca en el classpath a propósito: si estuviera en
 * src/main/resources se empaquetaría dentro del .jar con las claves.
 */
public final class AppConfig {
    /** Nombre del archivo de configuración. */
    public static final String CONFIG_FILE = "config.properties";
    /** Propiedad del sistema que permite indicar otra ruta del archivo. */
    public static final String CONFIG_PATH_PROPERTY = "vektra.config";
    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    /** Configuración global, cargada la primera vez que se pide. */
    private static volatile AppConfig global;
    private final Properties fileProperties;
    private final Map<String, String> environment;
    /**
     * Crea una configuración con datos ya cargados. Las pruebas la usan
     * para no depender del archivo real ni del entorno de la máquina.
     *
     * @param fileProperties datos leídos del archivo
     * @param environment    variables de entorno
     */
    public AppConfig(Properties fileProperties, Map<String, String> environment) {
        this.fileProperties = fileProperties == null ? new Properties() : fileProperties;
        this.environment = environment == null ? Map.of() : environment;
    }
    /**
     * Devuelve la configuración de la aplicación, leyéndola una sola vez.
     *
     * @return configuración global, nunca null
     */
    public static AppConfig global() {
        AppConfig current = global;
        if (current == null) {
            synchronized (AppConfig.class) {
                current = global;
                if (current == null) {
                    current = new AppConfig(loadFile(), System.getenv());
                    global = current;
                }
            }
        }
        return current;
    }
    /**
     * Busca un dato. Primero en el entorno y luego en el archivo.
     *
     * @param key nombre del dato en config.properties, por ejemplo db.url
     * @return el valor sin espacios sobrantes, o vacío si no está
     */
    public Optional<String> get(String key) {
        String fromEnvironment = environment.get(toEnvironmentName(key));
        if (fromEnvironment != null && !fromEnvironment.isBlank()) {
            return Optional.of(fromEnvironment.trim());
        }
        String fromFile = fileProperties.getProperty(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return Optional.of(fromFile.trim());
        }
        return Optional.empty();
    }
    /**
     * Busca un dato obligatorio.
     *
     * @param key nombre del dato en config.properties
     * @return el valor
     * @throws IllegalStateException si el dato no está configurado
     */
    public String require(String key) {
        return get(key).orElseThrow(() -> new IllegalStateException(
                "Falta la configuración '" + key + "'. Agréguela a " + CONFIG_FILE
                        + " o defina la variable de entorno " + toEnvironmentName(key) + "."));
    }
    /**
     * Busca un dato numérico.
     *
     * @param key          nombre del dato
     * @param defaultValue valor si el dato no está
     * @return el número configurado o el valor por defecto
     * @throws IllegalStateException si el dato existe pero no es un número
     */
    public int getInt(String key, int defaultValue) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.get());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("La configuración '" + key + "' debe ser un número.", e);
        }
    }
    /**
     * Convierte el nombre de un dato en el de su variable de entorno:
     * db.url pasa a DB_URL.
     *
     * @param key nombre del dato
     * @return nombre de la variable de entorno
     */
    static String toEnvironmentName(String key) {
        return key.toUpperCase().replace('.', '_');
    }
    /** Lee el archivo de configuración; si no existe devuelve datos vacíos. */
    private static Properties loadFile() {
        Properties properties = new Properties();
        Path file = Path.of(System.getProperty(CONFIG_PATH_PROPERTY, CONFIG_FILE));
        if (!Files.isRegularFile(file)) {
            LOGGER.log(Level.INFO, "No se encontró {0}; se usarán las variables de entorno.", 
file.toAbsolutePath());
            return properties;
        }
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "No se pudo leer " + file.toAbsolutePath(), e);
        }
        return properties;
    }
}