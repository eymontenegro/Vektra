package com.vektra.exception;
/**
* Se pidió un registro (conductor, vehículo, estación...) que no existe.
* El mensaje está en español.
*/
public class NotFoundException extends RuntimeException {
private static final long serialVersionUID = 1L;
/**
* @param message explicación para el usuario, en español
*/
public NotFoundException(String message) {
super(message);
}
}