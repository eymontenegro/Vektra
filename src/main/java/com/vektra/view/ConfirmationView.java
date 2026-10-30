package com.vektra.view;

import com.vektra.util.Styles;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Ventana pequeña que confirma que algo salió bien. Muestra el mensaje
 * que recibe, con la mascota y un botón para cerrarla.
 */
public class ConfirmationView extends BaseWindow {

    /** Ancho en píxeles del texto del mensaje. */
    private static final int MESSAGE_WIDTH = 280;

    /** Alto en píxeles de la mascota. */
    private static final int MASCOT_HEIGHT = 90;

    /** Alto en píxeles del ícono del título. */
    private static final int VEHICLE_HEIGHT = 32;

    /**
     * Crea la ventana de confirmación con el mensaje indicado.
     *
     * @param message texto que verá la persona, en español
     */
    public ConfirmationView(String message) {
        setSection("Confirmación");
        setResizable(false);
        initComponents(message);
        pack();
        applyStyle();
    }

    /** Arma los componentes de la ventana. */
    private void initComponents(String message) {
        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("¡Listo!",
                Styles.icon("confirmation-view-vehicle.png", VEHICLE_HEIGHT), JLabel.LEFT);
        titleLabel.setIconTextGap(10);
        titleLabel.setFont(Styles.font(Font.BOLD, 18f));
        titleLabel.setForeground(Styles.OK_BORDER);

        JLabel messageLabel = new JLabel(toHtml(message));
        messageLabel.setFont(Styles.font(Font.PLAIN, 14f));
        messageLabel.setForeground(Styles.TEXT);

        JLabel mascotLabel = new JLabel(Styles.icon("confirmation-view-mascot.png", MASCOT_HEIGHT));

        JButton closeButton = new JButton("Cerrar");
        closeButton.setFont(Styles.font(Font.BOLD, 14f));
        closeButton.setBackground(Styles.OK_BORDER);
        closeButton.setForeground(Color.WHITE);
        closeButton.setOpaque(true);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(e -> dispose());

        JPanel bottom = new JPanel(new BorderLayout(15, 0));
        bottom.setOpaque(false);
        bottom.add(mascotLabel, BorderLayout.WEST);
        bottom.add(closeButton, BorderLayout.EAST);

        content.add(titleLabel, BorderLayout.NORTH);
        content.add(messageLabel, BorderLayout.CENTER);
        content.add(bottom, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(closeButton);
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
