package com.vektra.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Carga la fuente personalizada de Vektra y la aplica a los componentes
 * de las pantallas.
 */
public final class FontUtil {

    /** Ruta de la fuente dentro de src/main/resources. */
    private static final String FONT_PATH = "/fonts/Coolvetica Rg.otf";

    /** Tamaño que se usa cuando un componente no tiene fuente definida. */
    private static final float DEFAULT_SIZE = 14f;

    private static final Logger LOGGER = Logger.getLogger(FontUtil.class.getName());

    /** Fuente cargada. Se lee una sola vez para ahorrar memoria. */
    private static Font customFont;

    /** Constructor privado: esta clase solo tiene métodos estáticos y no se instancia. */
    private FontUtil() {
    }

    /**
     * Devuelve la fuente personalizada, cargándola la primera vez.
     * Si no se puede cargar, devuelve una fuente SansSerif de respaldo.
     *
     * @return fuente lista para usar, nunca null
     */
    public static Font getCustomFont() {
        if (customFont == null) {
            customFont = loadFont();
        }
        return customFont;
    }

    /**
     * Aplica la fuente a un componente y a todos sus hijos. Conserva el
     * estilo y el tamaño que el diseñador de NetBeans le puso a cada uno.
     *
     * @param component componente o contenedor (por ejemplo un JPanel completo)
     */
    public static void applyCustomFont(Component component) {
        Font base = getCustomFont();
        Font current = component.getFont();
        if (current != null) {
            component.setFont(base.deriveFont(current.getStyle(), current.getSize()));
        } else {
            component.setFont(base.deriveFont(Font.PLAIN, DEFAULT_SIZE));
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyCustomFont(child);
            }
        }
    }

    /**
     * Lee el archivo de la fuente. Cierra el flujo siempre y, si algo falla,
     * deja un aviso en el registro y usa la fuente de respaldo.
     */
    private static Font loadFont() {
        try (InputStream input = FontUtil.class.getResourceAsStream(FONT_PATH)) {
            if (input == null) {
                LOGGER.log(Level.WARNING, "No se encontró el archivo de fuente: {0}", FONT_PATH);
                return fallbackFont();
            }
            return Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (IOException | FontFormatException e) {
            LOGGER.log(Level.WARNING, "No se pudo cargar la fuente: " + FONT_PATH, e);
            return fallbackFont();
        }
    }

    /** Fuente que se usa si la personalizada no está disponible. */
    private static Font fallbackFont() {
        return new Font(Font.SANS_SERIF, Font.PLAIN, (int) DEFAULT_SIZE);
    }
}