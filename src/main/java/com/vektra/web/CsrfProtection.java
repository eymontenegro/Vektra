package com.vektra.web;

import com.vektra.exception.AuthorizationException;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Protección contra CSRF (que otra página envíe formularios a Vektra
 * usando la sesión de la persona).
 *
 * Cada sesión tiene un token aleatorio. Todo formulario POST debe enviarlo
 * en el campo oculto _csrf (o el JavaScript en la cabecera X-CSRF-Token);
 * si no coincide, la petición se rechaza.
 */
public final class CsrfProtection {

    /** Nombre del campo oculto de los formularios. */
    public static final String FORM_FIELD = "_csrf";

    /** Nombre de la cabecera para peticiones hechas con fetch(). */
    public static final String HEADER = "X-CSRF-Token";

    private static final String SESSION_KEY = "vektra.csrf";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private CsrfProtection() {
    }

    /**
     * Token de la sesión actual; lo crea si todavía no existe.
     *
     * @param ctx petición actual
     * @return token para poner en los formularios
     */
    public static String token(Context ctx) {
        HttpSession session = ctx.req().getSession(true);
        Object token = session.getAttribute(SESSION_KEY);
        if (token instanceof String existing) {
            return existing;
        }
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String created = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, created);
        return created;
    }

    /**
     * Borra el token de la sesión para que se cree uno nuevo. WebSession la
     * llama al iniciar sesión, así un token visto antes del login deja de servir.
     *
     * @param session sesión actual
     */
    static void reset(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    /**
     * Comprueba el token en toda petición que cambia datos (POST).
     * Se registra como filtro "before" en WebServer.
     *
     * @param ctx petición actual
     * @throws AuthorizationException si el token falta o no coincide
     */
    public static void verify(Context ctx) {
        if (ctx.method() != HandlerType.POST) {
            return;
        }
        HttpSession session = ctx.req().getSession(false);
        Object expected = session == null ? null : session.getAttribute(SESSION_KEY);
        String received = ctx.header(HEADER);
        if (received == null) {
            received = ctx.formParam(FORM_FIELD);
        }
        if (!(expected instanceof String expectedToken) || received == null
                || !MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                        received.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthorizationException("La página expiró. Recárgala e intenta de nuevo.");
        }
    }
}
