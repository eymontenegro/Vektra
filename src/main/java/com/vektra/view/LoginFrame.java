package com.vektra.view;

import com.vektra.model.Passenger;
import com.vektra.model.User;
import com.vektra.service.PassengerService;
import com.vektra.service.UserService;
import com.vektra.util.Styles;
import com.vektra.util.UserSession;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/**
 * Pantalla de inicio de sesión. Tiene dos columnas: a la izquierda el logo
 * y a la derecha el formulario, donde la persona elige si entra como pasajero
 * o como administrador.
 *
 * Esta clase no abre ninguna otra ventana por su cuenta: recibe dos acciones
 * por constructor, una para cuando el ingreso es correcto y otra para el
 * botón de crear cuenta. Así no depende de {@code MainFrameView} ni de
 * {@code CreateAccountFrame}; las conecta {@code Main}.
 *
 * La autenticación se hace en un hilo aparte para que la ventana no se congele.
 */
public class LoginFrame extends BaseWindow {

    private static final Logger LOGGER = Logger.getLogger(LoginFrame.class.getName());

    /** Opción del combo para el pasajero. */
    private static final String PASSENGER_OPTION = "Pasajero Regular";

    /** Opción del combo para el administrador. */
    private static final String ADMIN_OPTION = "Administrador";

    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 640;
    private static final int BRAND_WIDTH = 350;
    private static final int FORM_WIDTH = 380;
    private static final int FIELD_HEIGHT = 40;
    private static final int LOGO_HEIGHT = 200;
    private static final int MASCOT_HEIGHT = 140;

    private final UserService userService;
    private final PassengerService passengerService;
    private final Runnable onLoginSuccess;
    private final Runnable onCreateAccount;

    private JComboBox<String> cmbUserType;
    private JTextField txtEmail;
    private JPasswordField pwdPassword;
    private JButton btnLogin;
    private JLabel lblCreateAccount;
    private JButton btnCreateAccount;

    /**
     * Crea la pantalla de inicio de sesión.
     *
     * @param userService      servicio que valida al administrador
     * @param passengerService servicio que valida al pasajero
     * @param onLoginSuccess   se ejecuta cuando el ingreso es correcto
     * @param onCreateAccount  se ejecuta al pulsar el botón de crear cuenta
     */
    public LoginFrame(UserService userService, PassengerService passengerService,
            Runnable onLoginSuccess, Runnable onCreateAccount) {
        this.userService = Objects.requireNonNull(userService, "userService no puede ser nulo");
        this.passengerService = Objects.requireNonNull(passengerService, "passengerService no puede ser nulo");
        this.onLoginSuccess = Objects.requireNonNull(onLoginSuccess, "onLoginSuccess no puede ser nulo");
        this.onCreateAccount = Objects.requireNonNull(onCreateAccount, "onCreateAccount no puede ser nulo");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        initComponents();
        setSection("Iniciar sesión");
        pack();
        setMinimumSize(getSize());
        applyStyle();
    }

    /** Arma las dos columnas de la ventana. */
    private void initComponents() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy = 0;
        constraints.weighty = 1;

        constraints.gridx = 0;
        constraints.weightx = 0.35;
        content.add(buildBrandPanel(), constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.65;
        content.add(buildFormSide(), constraints);

        setContentPane(content);
        getRootPane().setDefaultButton(btnLogin);
    }

    /** Columna izquierda: fondo oscuro con el logo y el nombre. */
    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Styles.SIDE_PANEL);
        panel.setPreferredSize(new Dimension(BRAND_WIDTH, WINDOW_HEIGHT));

        JLabel logo = new JLabel(Styles.icon("vektra-logo.png", LOGO_HEIGHT));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel brand = new JLabel("Vektra");
        brand.setFont(Styles.font(Font.BOLD, 40f));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(logo);
        box.add(Box.createVerticalStrut(20));
        box.add(brand);

        panel.add(box);
        return panel;
    }

    /** Columna derecha: el formulario centrado y la mascota abajo a la derecha. */
    private JPanel buildFormSide() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Color.WHITE);
        side.setPreferredSize(new Dimension(WINDOW_WIDTH - BRAND_WIDTH, WINDOW_HEIGHT));

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(buildForm());

        JPanel mascotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        mascotRow.setOpaque(false);
        mascotRow.add(new JLabel(Styles.icon("login-mascot.png", MASCOT_HEIGHT)));

        side.add(center, BorderLayout.CENTER);
        side.add(mascotRow, BorderLayout.SOUTH);
        return side;
    }

    /** Título, tipo de usuario, correo, contraseña y botones. */
    private JPanel buildForm() {
        JLabel title = new JLabel("Iniciar sesión");
        title.setFont(Styles.font(Font.BOLD, 30f));
        title.setForeground(Styles.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Accede a Vektra");
        subtitle.setFont(Styles.font(Font.PLAIN, 16f));
        subtitle.setForeground(Styles.LABEL);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        cmbUserType = new JComboBox<>(new String[]{PASSENGER_OPTION, ADMIN_OPTION});
        cmbUserType.setFont(Styles.font(Font.PLAIN, 14f));
        cmbUserType.setBackground(Styles.FIELD_BACKGROUND);
        cmbUserType.addActionListener(e -> updateCreateAccountVisibility());
        fixSize(cmbUserType, FIELD_HEIGHT);

        txtEmail = new JTextField();
        Styles.styleField(txtEmail);
        Styles.setPlaceholder(txtEmail, "correo@ejemplo.com");
        fixSize(txtEmail, FIELD_HEIGHT);

        pwdPassword = new JPasswordField();
        Styles.styleField(pwdPassword);
        pwdPassword.addActionListener(e -> login());
        fixSize(pwdPassword, FIELD_HEIGHT);

        btnLogin = new JButton("Entrar");
        Styles.styleButton(btnLogin);
        btnLogin.setFont(Styles.font(Font.BOLD, 15f));
        btnLogin.addActionListener(e -> login());
        fixSize(btnLogin, 44);

        lblCreateAccount = new JLabel("¿No tienes una cuenta?, crea una:");
        lblCreateAccount.setFont(Styles.font(Font.PLAIN, 13f));
        lblCreateAccount.setForeground(Styles.PRIMARY);
        lblCreateAccount.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnCreateAccount = new JButton("Crear cuenta");
        btnCreateAccount.setFont(Styles.font(Font.BOLD, 14f));
        btnCreateAccount.setBackground(Color.WHITE);
        btnCreateAccount.setForeground(Styles.PRIMARY);
        btnCreateAccount.setOpaque(true);
        btnCreateAccount.setFocusPainted(false);
        btnCreateAccount.setBorder(BorderFactory.createLineBorder(Styles.PRIMARY, 2));
        btnCreateAccount.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCreateAccount.addActionListener(e -> onCreateAccount.run());
        fixSize(btnCreateAccount, 44);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(title);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(28));
        form.add(fieldLabel("Tipo de usuario:"));
        form.add(Box.createVerticalStrut(6));
        form.add(cmbUserType);
        form.add(Box.createVerticalStrut(16));
        form.add(fieldLabel("Correo electrónico:"));
        form.add(Box.createVerticalStrut(6));
        form.add(txtEmail);
        form.add(Box.createVerticalStrut(16));
        form.add(fieldLabel("Contraseña:"));
        form.add(Box.createVerticalStrut(6));
        form.add(pwdPassword);
        form.add(Box.createVerticalStrut(24));
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(20));
        form.add(lblCreateAccount);
        form.add(Box.createVerticalStrut(6));
        form.add(btnCreateAccount);
        return form;
    }

    /** Crea la etiqueta que va encima de cada campo. */
    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Styles.font(Font.BOLD, 14f));
        label.setForeground(Styles.TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** Da al componente el ancho del formulario y el alto indicado. */
    private static void fixSize(JComponent component, int height) {
        Dimension size = new Dimension(FORM_WIDTH, height);
        component.setPreferredSize(size);
        component.setMaximumSize(size);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** El administrador no se registra desde la aplicación, así que se oculta la opción de crear cuenta. */
    private void updateCreateAccountVisibility() {
        boolean passenger = PASSENGER_OPTION.equals(cmbUserType.getSelectedItem());
        lblCreateAccount.setVisible(passenger);
        btnCreateAccount.setVisible(passenger);
    }

    /**
     * Valida las credenciales en un hilo aparte con el servicio que corresponde
     * al tipo de usuario elegido, y al terminar actualiza la pantalla.
     */
    private void login() {
        String email = Styles.getFieldText(txtEmail);
        String password = new String(pwdPassword.getPassword());
        boolean admin = ADMIN_OPTION.equals(cmbUserType.getSelectedItem());

        setBusy(true);
        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() {
                return admin
                        ? userService.authenticate(email, password)
                        : passengerService.authenticate(email, password);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    finishLogin(get());
                } catch (ExecutionException e) {
                    showLoginError(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Guarda la sesión, cierra el login y avisa que el ingreso fue correcto. */
    private void finishLogin(Object account) {
        if (account instanceof User admin) {
            UserSession.startAdminSession(admin);
        } else if (account instanceof Passenger passenger) {
            UserSession.startPassengerSession(passenger);
        }
        dispose();
        onLoginSuccess.run();
    }

    /** Muestra el mensaje del servicio; si el error no es esperado, lo registra y muestra uno general. */
    private void showLoginError(Throwable cause) {
        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
            new ErrorView(cause.getMessage()).setVisible(true);
        } else {
            LOGGER.log(Level.SEVERE, "Error inesperado al iniciar sesión", cause);
            new ErrorView("Ocurrió un error inesperado. Intenta de nuevo.").setVisible(true);
        }
        pwdPassword.setText("");
    }

    /** Bloquea el botón mientras se valida, para que no se envíe dos veces. */
    private void setBusy(boolean busy) {
        btnLogin.setEnabled(!busy);
        btnLogin.setText(busy ? "Entrando..." : "Entrar");
    }
}