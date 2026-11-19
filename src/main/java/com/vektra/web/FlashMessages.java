package com.vektra.web;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
* Mensajes de una sola vez ("Conductor registrado con éxito", "Algo salió
* mal..."). Se guardan en la sesión antes de redirigir y se muestran en la
* siguiente página. Reemplazan a ConfirmationView y ErrorView.
*/
public final class FlashMessages {
/** Clave de la sesión donde se guardan los mensajes pendientes. */
private static final String KEY = "vektra.flash";
/**
* Un mensaje pendiente.
*
* @param type "success" o "error"; la plantilla lo usa como clase CSS
* @param text texto en español
*/
public record FlashMessage(String type, String text) implements Serializable {
private static final long serialVersionUID = 1L;
}
/** Constructor privado: esta clase solo tiene métodos estáticos. */
private FlashMessages() {
}
/**
* Agrega un mensaje de éxito.
*
* @param ctx  petición actual
* @param text texto en español
*/
public static void success(Context ctx, String text) {
add(ctx, new FlashMessage("success", text));
}
    /**
     * Agrega un mensaje de error.
     *
     * @param ctx  petición actual
     * @param text texto en español
     */
    public static void error(Context ctx, String text) {
        add(ctx, new FlashMessage("error", text));
    }
    /**
     * Devuelve los mensajes pendientes y los borra, para que se vean una sola vez.
     *
     * @param ctx petición actual
     * @return mensajes pendientes; vacía si no hay
     */
    @SuppressWarnings("unchecked")
    public static List<FlashMessage> consume(Context ctx) {
        HttpSession session = ctx.req().getSession(false);
        if (session == null || session.getAttribute(KEY) == null) {
            return List.of();
        }
        List<FlashMessage> messages = (List<FlashMessage>) session.getAttribute(KEY);
        session.removeAttribute(KEY);
        return messages;
    }
    @SuppressWarnings("unchecked")
    private static void add(Context ctx, FlashMessage message) {
        HttpSession session = ctx.req().getSession(true);
        List<FlashMessage> messages = (List<FlashMessage>) session.getAttribute(KEY);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        session.setAttribute(KEY, messages);
    }
}