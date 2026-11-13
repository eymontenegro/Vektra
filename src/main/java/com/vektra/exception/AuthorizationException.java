package com.vektra.exception;
/**
 * Quien hizo la petición no tiene permiso para esa operación.
*
* Los servicios la lanzan aunque la pantalla ya oculte el botón, porque
* ocultar un botón no es seguridad. El mensaje está en español.
*/
public class AuthorizationException extends RuntimeException {
private static final long serialVersionUID = 1L;
/**
* @param message explicación para el usuario, en español
*/
public AuthorizationException(String message) {
super(message);
}
}