package com.vektra.exception;
/**
* Falló la base de datos (no hay conexión, se cayó la consulta...).
*
* Los servicios envuelven aquí la SQLException de los DAO: así las capas
* de arriba no dependen de JDBC y el usuario ve un mensaje claro en
* español, mientras el detalle técnico queda en el registro.
*/
public class DataAccessException extends RuntimeException {
private static final long serialVersionUID = 1L;
/**
* @param message explicación para el usuario, en español
* @param cause   error original de la base de datos
*/
public DataAccessException(String message, Throwable cause) {
super(message, cause);
}
}