package com.vektra.view;

import com.vektra.model.Passenger;
import com.vektra.service.PassengerService;
import com.vektra.util.Styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

/**
 * Pantalla para crear una cuenta de pasajero. Solo el pasajero se registra
 * desde la aplicación; el administrador ya existe en la base de datos.
 *
 * Al cerrarse (por registro exitoso o con la X) vuelve al inicio de sesión;
 * recibe esa acción por constructor y así no depende de LoginFrame.
 */
public class CreateAccountFrame extends BaseWindow {

    private static final Logger LOGGER = Logger.getLogger(CreateAccountFrame.class.getName());

    private static final int WINDOW_WIDTH = 520;
    private static final int WINDOW_HEIGHT = 700;
    private static final int FORM_WIDTH = 360;
    private static final int FIELD_HEIGHT = 40;

    private final PassengerService passengerService;
    private final Runnable onBackToLogin;

    private JTextField txtNationalId;
    private JTextField txtName;
    private JTextField txtLastName;
    private JTextField txtEmail;
    private JPasswordField pwdPassword;
    private JSpinner spnBirthDate;
    private JButton btnRegister;

    /**
     * Crea la pantalla de registro.
     *
     * @param passengerService servicio que registra al pasajero
     * @param onBackToLogin    se ejecuta al cerrar esta ventana
     */
    public CreateAccountFrame(PassengerService passengerService, Runnable onBackToLogin) {
        this.passengerService = passengerService;
        this.onBackToLogin = onBackToLogin;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        setSection("Crear cuenta");
        pack();
        setMinimumSize(getSize());
        applyStyle();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onBackToLogin.run();
            }
        });
    }

    /** Arma la ventana con el formulario centrado. */
    private void initComponents() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        content.add(buildForm());
        setContentPane(content);
        getRootPane().setDefaultButton(btnRegister);
    }

    /** Título, campos del registro y botón. */
    private JPanel buildForm() {
        JLabel lblTitle = new JLabel("Crear cuenta");
        lblTitle.setFont(Styles.font(Font.BOLD, 28f));
        lblTitle.setForeground(Styles.TEXT);

        JLabel lblSubtitle = new JLabel("Regístrate para comprar tickets");
        lblSubtitle.setFont(Styles.font(Font.PLAIN, 14f));
        lblSubtitle.setForeground(Styles.LABEL);

        txtNationalId = new JTextField();
        Styles.styleField(txtNationalId);
        Styles.setPlaceholder(txtNationalId, "Ej. 1020304050");
        fixSize(txtNationalId);

        txtName = new JTextField();
        Styles.styleField(txtName);
        Styles.setPlaceholder(txtName, "Ej. María");
        fixSize(txtName);

        txtLastName = new JTextField();
        Styles.styleField(txtLastName);
        Styles.setPlaceholder(txtLastName, "Ej. Pérez");
        fixSize(txtLastName);

        txtEmail = new JTextField();
        Styles.styleField(txtEmail);
        Styles.setPlaceholder(txtEmail, "correo@ejemplo.com");
        fixSize(txtEmail);

        pwdPassword = new JPasswordField();
        Styles.styleField(pwdPassword);
        fixSize(pwdPassword);

        JLabel lblPasswordHint = new JLabel("Mínimo " + PassengerService.MIN_PASSWORD_LENGTH
                + " caracteres, la misma regla de todo el sistema.");
        lblPasswordHint.setFont(Styles.font(Font.PLAIN, 12f));
        lblPasswordHint.setForeground(Styles.LABEL);

        spnBirthDate = new JSpinner(new SpinnerDateModel(
                new GregorianCalendar(2000, Calendar.JANUARY, 1).getTime(),
                null, null, Calendar.DAY_OF_MONTH));
        spnBirthDate.setEditor(new JSpinner.DateEditor(spnBirthDate, "dd/MM/yyyy"));
        spnBirthDate.setFont(Styles.font(Font.PLAIN, 14f));
        spnBirthDate.setBackground(Styles.FIELD_BACKGROUND);
        spnBirthDate.setBorder(new Styles.RoundedBorder(Styles.FIELD_BORDER, 8, 1, 10));
        fixSize(spnBirthDate);

        btnRegister = new JButton("Crear cuenta");
        Styles.styleButton(btnRegister);
        btnRegister.setFont(Styles.font(Font.BOLD, 15f));
        btnRegister.addActionListener(e -> register());
        btnRegister.setPreferredSize(new Dimension(FORM_WIDTH, 44));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 4, 4, 4);
        int row = 0;
        form.add(lblTitle, constraints);
        constraints.gridy = ++row;
        form.add(lblSubtitle, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Cédula:"), constraints);
        constraints.gridy = ++row;
        form.add(txtNationalId, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Nombre:"), constraints);
        constraints.gridy = ++row;
        form.add(txtName, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Apellido:"), constraints);
        constraints.gridy = ++row;
        form.add(txtLastName, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Correo electrónico:"), constraints);
        constraints.gridy = ++row;
        form.add(txtEmail, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Contraseña:"), constraints);
        constraints.gridy = ++row;
        form.add(pwdPassword, constraints);
        constraints.gridy = ++row;
        form.add(lblPasswordHint, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Fecha de nacimiento:"), constraints);
        constraints.gridy = ++row;
        form.add(spnBirthDate, constraints);
        constraints.gridy = ++row;
        form.add(new javax.swing.Box.Filler(
                new Dimension(0, 10), new Dimension(0, 10), new Dimension(0, 10)), constraints);
        constraints.gridy = ++row;
        form.add(btnRegister, constraints);
        return form;
    }

    /** Etiqueta que va encima de cada campo. */
    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Styles.font(Font.BOLD, 14f));
        label.setForeground(Styles.TEXT);
        return label;
    }

    /** Da al componente el ancho del formulario y el alto estándar. */
    private static void fixSize(JComponent component) {
        Dimension size = new Dimension(FORM_WIDTH, FIELD_HEIGHT);
        component.setPreferredSize(size);
        component.setMaximumSize(size);
    }

    /**
     * Valida el formato en la vista (solo ayuda visual) y registra en un
     * hilo aparte; las reglas de verdad las comprueba el Service.
     */
    private void register() {
        String nationalId = Styles.getFieldText(txtNationalId);
        String name = Styles.getFieldText(txtName);
        String lastName = Styles.getFieldText(txtLastName);
        String email = Styles.getFieldText(txtEmail);
        String password = new String(pwdPassword.getPassword());
        java.time.LocalDate birthDate = ((Date) spnBirthDate.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        boolean complete = !nationalId.isEmpty() && !name.isEmpty()
                && !lastName.isEmpty() && !email.isEmpty() && !password.isEmpty();
        Styles.markField(txtNationalId, !nationalId.isEmpty());
        Styles.markField(txtName, !name.isEmpty());
        Styles.markField(txtLastName, !lastName.isEmpty());
        Styles.markField(txtEmail, !email.isEmpty());
        Styles.markField(pwdPassword, !password.isEmpty());
        if (!complete) {
            new ErrorView("Todos los campos son obligatorios.").setVisible(true);
            return;
        }
        setBusy(true);
        new SwingWorker<Passenger, Void>() {
            @Override
            protected Passenger doInBackground() {
                return passengerService.register(nationalId, name, lastName, email, password, birthDate);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    get();
                    new ConfirmationView("¡Cuenta creada con éxito! Ya puedes iniciar sesión.").setVisible(true);
                    dispose();
                } catch (ExecutionException e) {
                    handleFailure(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Muestra el mensaje del servicio; lo inesperado se registra y se muestra genérico. */
    private void handleFailure(Throwable cause) {
        if (cause instanceof IllegalArgumentException || cause instanceof IllegalStateException) {
            new ErrorView(cause.getMessage()).setVisible(true);
        } else {
            LOGGER.log(Level.SEVERE, "Error inesperado al crear la cuenta", cause);
            new ErrorView("Ocurrió un error inesperado. Intenta de nuevo.").setVisible(true);
        }
    }

    /** Bloquea el botón mientras se registra, para no enviar dos veces. */
    private void setBusy(boolean busy) {
        btnRegister.setEnabled(!busy);
        btnRegister.setText(busy ? "Creando..." : "Crear cuenta");
    }
}