package com.vektra.view;

import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import com.vektra.util.FontUtil;

/**
 * Ventana base de Vektra. Todas las ventanas (JFrame) la extienden para
 * compartir el mismo título, fondo, fuente y posición, y que ninguna
 * se vea distinta de las demás.
 *
 * Uso: al final del constructor de cada ventana, después de initComponents(),
 * llamar a setSection("nombre de la pantalla") y a applyStyle().
 */
public class BaseWindow extends JFrame {

    /** Título que aparece en la barra de todas las ventanas. */
    private static final String APP_TITLE = "Vektra";

    /**
     * Crea la ventana con los valores comunes: título, fondo blanco y
     * cierre que solo libera esta ventana. Las ventanas principales
     * (login y marco principal) cambian el cierre a EXIT_ON_CLOSE.
     */
    public BaseWindow() {
        setTitle(APP_TITLE);
        getContentPane().setBackground(Color.WHITE);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    /**
     * Pone un subtítulo al título de la ventana, por ejemplo
     * "Vektra - Iniciar sesión".
     *
     * @param section nombre de la pantalla
     */
    public void setSection(String section) {
        setTitle(APP_TITLE + " - " + section);
    }

    /**
     * Aplica la fuente de Vektra a todos los componentes y centra la
     * ventana en pantalla. Se llama una sola vez, al final del constructor
     * de cada ventana, porque el tamaño final solo se conoce después de
     * initComponents().
     */
    protected void applyStyle() {
        FontUtil.applyCustomFont(this);
        setLocationRelativeTo(null);
    }
}