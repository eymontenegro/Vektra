package com.vektra.connection;
import com.vektra.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
/**
 * Única forma de abrir una conexión con la base de datos PostgreSQL.
 *
 * Los datos de conexión salen de AppConfig (variables de entorno o
 * config.properties). Cada llamada devuelve una conexión nueva, y quien la
 * pide debe cerrarla con try-with-resources.
 */
public final class DatabaseConnection {
    /** Segundos máximos de espera para conectarse antes de dar error. */
    private static final int LOGIN_TIMEOUT_SECONDS = 5;
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
        AppConfig config = AppConfig.global();
        String url;
        String user;
        String password;
        try {
            url = config.require("db.url");
            user = config.require("db.user");
            password = config.require("db.password");
        } catch (IllegalStateException e) {
            // Se convierte en SQLException para que los DAO la traten como un fallo de base de datos.
            throw new SQLException(e.getMessage(), e);
        }
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        return DriverManager.getConnection(url, user, password);
    }
}