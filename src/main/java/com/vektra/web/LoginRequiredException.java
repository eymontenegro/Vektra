package com.vektra.web;
/**
* Se pidió una página que necesita sesión y no hay nadie conectado.
* WebServer la convierte en una redirección a /login.
*/
public class LoginRequiredException extends RuntimeException {
private static final long serialVersionUID = 1L;
/** Crea la excepción con un mensaje fijo para el registro. */
public LoginRequiredException() {
super("Se necesita iniciar sesión.");
}
}