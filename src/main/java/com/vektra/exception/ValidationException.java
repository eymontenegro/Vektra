package com.vektra.exception;
/**
 * Los datos que envió la persona no cumplen una regla del negocio
 * (campo vacío, formato inválido, dato repetido...).
 *
 * El mensaje siempre está en español y se puede mostrar tal cual en pantalla.
 */
public class ValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    /**
     * @param message explicación para el usuario, en español
     */
    public ValidationException(String message) {
        super(message);
    }
    /**
     * @param message explicación para el usuario, en español
     * @param cause   error original, para el registro
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}