package com.vektra.view;

import com.vektra.util.Styles;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Ventana pequeña que avisa de un error. Muestra el mensaje que recibe,
 * con la mascota y un botón para cerrarla.
 */
public class ErrorView extends JFrame {

    /** Ancho en píxeles del texto del mensaje. */
    private static final int MESSAGE_WIDTH = 280;

    /** Alto en píxeles de la mascota. */
    private static final int MASCOT_HEIGHT = 90;

    /** Alto en píxeles del ícono de advertencia. */
    private static final int WARNING_HEIGHT = 28;

    /**
     * Crea la ventana de error con el mensaje indicado.
     *
     * @param message texto que verá la persona, en español
     */
    public ErrorView(String message) {
        setTitle("Vektra - Error");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);
        initComponents(message);
        pack();
        setLocationRelativeTo(null);
    }

    /** Arma los componentes de la ventana. */
    private void initComponents(String message) {
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(java.awt.Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Algo salió mal",
                Styles.icon("error-view-warning.png", WARNING_HEIGHT), JLabel.LEFT);
        titleLabel.setIconTextGap(10);
        titleLabel.setFont(Styles.font(Font.BOLD, 18f));
        titleLabel.setForeground(Styles.ERROR_BORDER);

        JLabel messageLabel = new JLabel(toHtml(message));
        messageLabel.setFont(Styles.font(Font.PLAIN, 14f));
        messageLabel.setForeground(Styles.TEXT);

        JLabel mascotLabel = new JLabel(Styles.icon("error-view-mascot.png", MASCOT_HEIGHT));

        JButton okButton = new JButton("OK");
        okButton.setFont(Styles.font(Font.BOLD, 14f));
        okButton.setBackground(Styles.ERROR_BORDER);
        okButton.setForeground(java.awt.Color.WHITE);
        okButton.setOpaque(true);
        okButton.setBorderPainted(false);
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new BorderLayout(15, 0));
        bottom.setOpaque(false);
        bottom.add(mascotLabel, BorderLayout.WEST);
        bottom.add(okButton, BorderLayout.EAST);

        content.add(titleLabel, BorderLayout.NORTH);
        content.add(messageLabel, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(okButton);
    }

    /**
     * Convierte el mensaje a HTML para que el texto largo se divida en
     * varias líneas. Escapa los símbolos especiales para que se vean tal cual.
     */
    private static String toHtml(String message) {
        String safe = message == null ? "" : message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return "<html><div style='width:" + MESSAGE_WIDTH + "px'>" + safe + "</div></html>";
    }
}