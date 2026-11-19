package com.vektra.web;
import io.javalin.Javalin;

/**
* Controlador web de una parte de la aplicación (autenticación, mapa,
* tickets, flota, dashboard). Cada controlador registra sus propias rutas,
* así ningún archivo concentra todas las URL del sistema.
*/
public interface Controller {
/**
* Registra las rutas del controlador en el servidor.
*
* @param app servidor Javalin
*/
void register(Javalin app);
}