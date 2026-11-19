package com.vektra.web;

import com.vektra.security.SessionUser;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpSession;

/**
* Guarda y lee la persona conectada en la sesión HTTP del navegador.
*
* Reemplaza a UserSession: cada navegador tiene su propia sesión, de modo
* que dos personas pueden usar Vektra al mismo tiempo sin mezclarse.
*/
public final class WebSession {
    /** Clave de la sesión donde se guarda el SessionUser. */
    private static final String USER_KEY = "vektra.user";
    /** Minutos sin actividad antes de que la sesión expire. */
    private static final int TIMEOUT_MINUTES = 30;
    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private WebSession() {
    }
    /**
     * Persona conectada.
     *
     * @param ctx petición actual
     * @return usuario de la sesión, o null si no hay nadie conectado
     */
    public static SessionUser currentUser(Context ctx) {
        HttpSession session = ctx.req().getSession(false);
        if (session == null) {
            return null;
        }
        Object user = session.getAttribute(USER_KEY);
        return user instanceof SessionUser sessionUser ? sessionUser : null;
    }
    /**
     * Persona conectada, obligatoria.
     *
     * @param ctx petición actual
     * @return usuario de la sesión
     * @throws LoginRequiredException si no hay nadie conectado
     */
    public static SessionUser requireUser(Context ctx) {
        SessionUser user = currentUser(ctx);
        if (user == null) {
            throw new LoginRequiredException();
        }
        return user;
    }
    /**
     * Inicia la sesión. Cambia el id de la sesión para que nadie pueda
     * reutilizar uno que obtuvo antes del inicio de sesión (fijación de sesión).
     *
     * @param ctx  petición actual
     * @param user persona que inició sesión
     */
    public static void login(Context ctx, SessionUser user) {
        HttpSession session = ctx.req().getSession(true);
        ctx.req().changeSessionId();
        session.setMaxInactiveInterval(TIMEOUT_MINUTES * 60);
        session.setAttribute(USER_KEY, user);
        CsrfProtection.reset(session);
    }
    /**
     * Cierra la sesión y borra todos sus datos.
     *
     * @param ctx petición actual
     */
    public static void logout(Context ctx) {
HttpSession session = ctx.req().getSession(false);
if (session != null) {
session.invalidate();
}
}
}