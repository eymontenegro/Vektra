package com.vektra.web;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
/**
* Convierte las plantillas HTML de src/main/resources/templates en la
* página que recibe el navegador, usando Thymeleaf.
*
* Agrega a todas las páginas los datos comunes: la persona conectada, el
* token CSRF de los formularios, los mensajes pendientes y la ruta actual
* (para marcar el menú). Thymeleaf escapa todo texto que se muestra con
* th:text, así un dato del usuario nunca se ejecuta como HTML.
 */
public final class TemplateRenderer {
    /** Idioma de las páginas. */
    private static final Locale SPANISH_COLOMBIA = Locale.forLanguageTag("es-CO");
    private final TemplateEngine engine;
    /**
     * @param cacheTemplates true en producción; false mientras se desarrolla,
     *                       para ver los cambios del HTML sin reiniciar
     */
    public TemplateRenderer(boolean cacheTemplates) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(cacheTemplates);
        engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
    }
    /**
     * Genera una página y la envía al navegador.
     *
     * @param ctx      petición actual
     * @param template nombre de la plantilla sin .html, por ejemplo "login" o "fleet/drivers"
     * @param model    datos propios de la página
     */
    public void render(Context ctx, String template, Map<String, ?> model) {
        Map<String, Object> variables = new HashMap<>(model);
        variables.put("currentUser", WebSession.currentUser(ctx));
        variables.put("csrfToken", CsrfProtection.token(ctx));
        variables.put("flashMessages", FlashMessages.consume(ctx));
        variables.put("currentPath", ctx.path());
        org.thymeleaf.context.Context thymeleafContext =
                new org.thymeleaf.context.Context(SPANISH_COLOMBIA, variables);
        ctx.contentType("text/html; charset=utf-8");
        ctx.result(engine.process(template, thymeleafContext));
    }
    /**
     * Genera una página sin datos propios.
     *
     * @param ctx      petición actual
     * @param template nombre de la plantilla sin .html
     */
    public void render(Context ctx, String template) {
        render(ctx, template, Map.of());
    }
}