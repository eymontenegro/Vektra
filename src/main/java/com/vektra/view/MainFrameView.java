package com.vektra.view;

import com.vektra.model.Role;
import com.vektra.util.Styles;
import com.vektra.util.UserSession;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * Ventana principal de Vektra: menú lateral a la izquierda y la sección
 * elegida al centro. Muestra solo las secciones que el rol permite; el
 * Service vuelve a comprobar el rol en cada operación, porque ocultar un
 * botón no es seguridad.
 */
public class MainFrameView extends BaseWindow {

    /**
     * Una sección del menú: el panel que se muestra y la acción que prepara
     * sus datos (por ejemplo, recargar la lista) cada vez que se abre.
     */
    public record Section(JPanel panel, Runnable onShow) { }

    /** Ancho del menú lateral. */
    private static final int SIDEBAR_WIDTH = 220;

    private final Map<String, Section> sections;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    /**
     * Crea la ventana principal.
     *
     * @param sections secciones disponibles, en el orden del menú
     * @param onLogout se ejecuta al cerrar sesión (Main vuelve al login)
     */
    public MainFrameView(Map<String, Section> sections, Runnable onLogout) {
        this.sections = sections;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        content.setPreferredSize(new Dimension(1000, 660));
        setContentPane(buildContent(onLogout));
        setSection("Principal");
        pack();
        applyStyle();
    }

    /** Menú lateral + área de contenido. */
    private JPanel buildContent(Runnable onLogout) {
        JPanel window = new JPanel(new BorderLayout());
        window.add(buildSidebar(onLogout), BorderLayout.WEST);
        window.add(buildSections(), BorderLayout.CENTER);
        return window;
    }

    private JPanel buildSidebar(Runnable onLogout) {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(Styles.SIDE_PANEL);
        sidebar.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        JLabel lblBrand = new JLabel("Vektra");
        lblBrand.setFont(Styles.font(Font.BOLD, 26f));
        lblBrand.setForeground(Color.WHITE);
        lblBrand.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblBrand);

        JLabel lblWelcome = new JLabel("Hola, " + currentUserName());
        lblWelcome.setFont(Styles.font(Font.PLAIN, 13f));
        lblWelcome.setForeground(Styles.LABEL);
        lblWelcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblWelcome);
        sidebar.add(Box.createVerticalStrut(25));

        for (String name : sections.keySet()) {
            if (isAllowed(name)) {
                sidebar.add(menuButton(name, () -> showSection(name)));
                sidebar.add(Box.createVerticalStrut(10));
            }
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(menuButton("Cerrar sesión", () -> {
            UserSession.close();
            dispose();
            onLogout.run();
        }));
        return sidebar;
    }

    private JPanel buildSections() {
        content.setBackground(Color.WHITE);
        String firstName = null;
        for (Map.Entry<String, Section> entry : sections.entrySet()) {
            if (isAllowed(entry.getKey())) {
                content.add(entry.getValue().panel(), entry.getKey());
                if (firstName == null) {
                    firstName = entry.getKey();
                }
            }
        }
        if (firstName != null) {
            showSection(firstName);
        }
        return content;
    }

    /** Muestra una sección y prepara sus datos. */
    private void showSection(String name) {
        cards.show(content, name);
        Section section = sections.get(name);
        if (section.onShow() != null) {
            section.onShow().run();
        }
    }

    /** Indica si el rol actual puede ver la sección. */
    private boolean isAllowed(String section) {
        Role role = UserSession.getRole();
        return switch (section) {
            case "Mapa" -> true;
            case "Comprar ticket", "Mis tickets" -> role.canBuyTickets();
            case "Dashboard" -> role.canViewDashboard();
            default -> role.canManageFleet();
        };
    }

    private JButton menuButton(String text, Runnable action) {
        JButton button = new JButton(text);
        Styles.styleButton(button);
        button.setFont(Styles.font(Font.BOLD, 13f));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(e -> action.run());
        return button;
    }

    private String currentUserName() {
        return UserSession.getRole() == Role.PASSENGER
                ? UserSession.getActivePassenger().getFullName()
                : UserSession.getActiveAdmin().getName();
    }
}
