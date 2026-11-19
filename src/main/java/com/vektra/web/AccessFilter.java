package com.vektra.web;
import com.vektra.exception.AuthorizationException;
import com.vektra.security.SessionUser;
import io.javalin.http.Context;
import java.util.List;
/**
 * Decide quién puede abrir cada grupo de páginas antes de que se ejecute
 * el controlador. Es una primera barrera: los servicios vuelven a comprobar
 * el permiso en cada operación.
 *
 *   - Públicas: /login, /register y los archivos estáticos.
 *   - /admin/...: solo el administrador.
 *   - /tickets/... y /api/routes/...: solo pasajeros.
 *   - El resto (mapa, cerrar sesión): cualquier persona conectada.
 */
public final class AccessFilter {
    /** Rutas que se pueden abrir sin iniciar sesión. */
    private static final List<String> PUBLIC_PREFIXES =
            List.of("/login", "/register", "/css/", "/js/", "/images/", "/fonts/", "/favicon");
    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private AccessFilter() {
    }
    /**
     * Filtro "before" que se registra en WebServer.
*
* @param ctx petición actual
* @throws LoginRequiredException si la página necesita sesión y no hay
* @throws AuthorizationException si el rol no tiene acceso
*/
public static void check(Context ctx) {
String path = ctx.path();
if (isPublic(path)) {
return;
}
SessionUser user = WebSession.requireUser(ctx);
if (path.startsWith("/admin") && !user.canManageFleet() && !user.canViewDashboard()) {
throw new AuthorizationException("Esta sección es solo para el administrador.");
}
if ((path.startsWith("/tickets") || path.startsWith("/api/routes")) && !user.canBuyTickets()) {
throw new AuthorizationException("Esta sección es solo para pasajeros.");
}
}
/**
* @param path ruta pedida
* @return true si no necesita sesión
*/
static boolean isPublic(String path) {
return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
}
}