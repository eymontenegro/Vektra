package com.vektra.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Colores, fuente y estilos comunes de todas las pantallas de Vektra.
 *
 * Reúne en un solo lugar lo que antes estaba copiado en cada vista:
 * la paleta, los bordes redondeados, el texto de ejemplo de los campos
 * y la validación visual (rojo si está mal, verde si está bien).
 */
public final class Styles {

    /** Fondo del panel lateral oscuro. */
    public static final Color SIDE_PANEL = Color.decode("#191717");

    /** Texto principal. */
    public static final Color TEXT = Color.decode("#333333");

    /** Texto de las etiquetas que acompañan a los campos. */
    public static final Color LABEL = Color.decode("#666666");

    /** Color del botón principal. */
    public static final Color PRIMARY = Color.decode("#0263C8");

    /** Color del botón principal cuando el ratón pasa por encima. */
    public static final Color PRIMARY_HOVER = Color.decode("#0251A3");

    /** Fondo de los campos de texto. */
    public static final Color FIELD_BACKGROUND = Color.decode("#F3F4F6");

    /** Borde de un campo sin validar. */
    public static final Color FIELD_BORDER = Color.decode("#E5E7EB");

    /** Borde de un campo con el cursor dentro. */
    public static final Color FOCUS_BORDER = Color.decode("#37B9D6");

    /** Texto de ejemplo dentro de un campo vacío. */
    public static final Color PLACEHOLDER = Color.decode("#9CA3AF");

    /** Borde de un campo con datos incorrectos. */
    public static final Color ERROR_BORDER = Color.decode("#EF4444");

    /** Fondo de un campo con datos incorrectos. */
    public static final Color ERROR_BACKGROUND = Color.decode("#FEF2F2");

    /** Borde de un campo con datos correctos. */
    public static final Color OK_BORDER = Color.decode("#10B981");

    /** Fondo de un campo con datos correctos. */
    public static final Color OK_BACKGROUND = Color.decode("#F0FDF4");

    /** Radio de las esquinas redondeadas. */
    private static final int RADIUS = 8;

    /** Espacio entre el borde y el texto de un campo. */
    private static final int PADDING = 10;

    /** Clave donde cada campo guarda su texto de ejemplo. */
    private static final String PLACEHOLDER_KEY = "placeholder";

    /** Constructor privado: esta clase solo tiene miembros estáticos y no se instancia. */
    private Styles() {
    }

    /**
     * Devuelve la fuente de Vektra con el estilo y tamaño pedidos.
     *
     * @param style estilo, por ejemplo Font.BOLD
     * @param size  tamaño en puntos
     * @return fuente lista para usar
     */
    public static Font font(int style, float size) {
        return FontUtil.getCustomFont().deriveFont(style, size);
    }

    /**
     * Da a un campo de texto el aspecto normal de Vektra: fondo gris claro,
     * borde redondeado y borde celeste mientras tiene el cursor dentro.
     *
     * @param field campo de texto a estilizar
     */
    public static void styleField(JTextField field) {
        field.setBackground(FIELD_BACKGROUND);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setBorder(new RoundedBorder(FIELD_BORDER, RADIUS, 1, PADDING));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (FIELD_BACKGROUND.equals(field.getBackground())) {
                    field.setBorder(new RoundedBorder(FOCUS_BORDER, RADIUS, 1, PADDING));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (FIELD_BACKGROUND.equals(field.getBackground())) {
                    field.setBorder(new RoundedBorder(FIELD_BORDER, RADIUS, 1, PADDING));
                }
            }
        });
    }

    /**
     * Da a un botón el aspecto del botón principal: fondo azul, letra blanca
     * y un azul más oscuro cuando el ratón pasa por encima.
     *
     * @param button botón a estilizar
     */
    public static void styleButton(JButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(PRIMARY_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY);
            }
        });
    }

    /**
     * Pinta un campo en verde si sus datos son correctos o en rojo si no lo son.
     *
     * @param field campo de texto a marcar
     * @param valid true si los datos son correctos
     */
    public static void markField(JTextField field, boolean valid) {
        field.setBackground(valid ? OK_BACKGROUND : ERROR_BACKGROUND);
        field.setBorder(new RoundedBorder(valid ? OK_BORDER : ERROR_BORDER, RADIUS, 1, PADDING));
    }

    /**
     * Pone un texto de ejemplo en gris que desaparece al hacer clic en el
     * campo y vuelve si se deja vacío. No usar con JPasswordField, porque
     * el ejemplo se vería como puntos.
     *
     * @param field       campo de texto
     * @param placeholder texto de ejemplo
     */
    public static void setPlaceholder(JTextField field, String placeholder) {
        field.putClientProperty(PLACEHOLDER_KEY, placeholder);
        showPlaceholder(field, placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (isShowingPlaceholder(field)) {
                    field.setForeground(TEXT);
                    field.setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isBlank()) {
                    showPlaceholder(field, placeholder);
                }
            }
        });
    }

    /**
     * Devuelve lo que la persona escribió, sin espacios sobrantes. Si el
     * campo solo muestra el texto de ejemplo, devuelve una cadena vacía.
     *
     * @param field campo de texto
     * @return texto escrito por la persona, nunca null
     */
    public static String getFieldText(JTextField field) {
        return isShowingPlaceholder(field) ? "" : field.getText().trim();
    }

    /** Muestra el texto de ejemplo en gris. */
    private static void showPlaceholder(JTextField field, String placeholder) {
        field.setForeground(PLACEHOLDER);
        field.setText(placeholder);
    }

    /** Indica si el campo está mostrando su texto de ejemplo. */
    private static boolean isShowingPlaceholder(JTextField field) {
        Object placeholder = field.getClientProperty(PLACEHOLDER_KEY);
        return placeholder != null
                && placeholder.equals(field.getText())
                && PLACEHOLDER.equals(field.getForeground());
    }

    /**
     * Escucha los cambios del texto de un campo y ejecuta una acción cada
     * vez que cambia, sin importar si se escribió, se borró o se pegó.
     */
    public static final class SimpleDocListener implements DocumentListener {

        private final Runnable action;

        /**
         * @param action acción que se ejecuta con cada cambio del texto
         */
        public SimpleDocListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            action.run();
        }
    }

    /**
     * Borde de esquinas redondeadas con un espacio interior para que el
     * texto no quede pegado a la línea.
     */
    public static final class RoundedBorder implements Border {

        private final Color color;
        private final int radius;
        private final int thickness;
        private final int padding;

        /**
         * @param color     color de la línea
         * @param radius    radio de las esquinas
         * @param thickness grosor de la línea
         * @param padding   espacio entre la línea y el contenido
         */
        public RoundedBorder(Color color, int radius, int thickness, int padding) {
            this.color = color;
            this.radius = radius;
            this.thickness = thickness;
            this.padding = padding;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
                    width - thickness, height - thickness, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int space = thickness + padding;
            return new Insets(space, space, space, space);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}