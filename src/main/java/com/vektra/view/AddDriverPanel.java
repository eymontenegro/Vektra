package com.vektra.view;

import com.vektra.model.Route;
import com.vektra.service.DriverService;
import com.vektra.service.RouteService;
import com.vektra.util.Styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Formulario para registrar un conductor. El combo de rutas guarda el id de
 * la ruta, no su texto, y las rutas salen de RouteService (nunca del DAO).
 * Al guardar avisa a la lista de conductores para que se refresque.
 */
public class AddDriverPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(AddDriverPanel.class.getName());

    private static final int FORM_WIDTH = 340;
    private static final int FIELD_HEIGHT = 40;

    private final DriverService driverService;
    private final RouteService routeService;
    private final Runnable onDriverChanged;

    private JTextField txtNationalId;
    private JTextField txtName;
    private JTextField txtLastName;
    private JTextField txtLicense;
    private JTextField txtPhone;
    private JComboBox<RouteOption> cmbRoute;
    private JButton btnSave;

    /**
     * @param driverService   servicio que guarda el conductor
     * @param routeService    servicio que entrega las rutas del combo
     * @param onDriverChanged se ejecuta tras guardar, para refrescar la lista
     */
    public AddDriverPanel(DriverService driverService, RouteService routeService,
            Runnable onDriverChanged) {
        super(new BorderLayout(10, 10));
        this.driverService = driverService;
        this.routeService = routeService;
        this.onDriverChanged = onDriverChanged;
        setBackground(Color.WHITE);
        initComponents();
    }

    /** Arma el título y el formulario centrado. */
    private void initComponents() {
        JLabel lblTitle = new JLabel("Añadir conductor");
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        txtNationalId = new JTextField();
        Styles.styleField(txtNationalId);
        Styles.setPlaceholder(txtNationalId, "Ej. 1020304050");
        fixSize(txtNationalId);

        txtName = new JTextField();
        Styles.styleField(txtName);
        Styles.setPlaceholder(txtName, "Ej. Carlos");
        fixSize(txtName);

        txtLastName = new JTextField();
        Styles.styleField(txtLastName);
        Styles.setPlaceholder(txtLastName, "Ej. Pérez");
        fixSize(txtLastName);

        txtLicense = new JTextField();
        Styles.styleField(txtLicense);
        Styles.setPlaceholder(txtLicense, "Ej. LIC-0003 (opcional)");
        fixSize(txtLicense);

        txtPhone = new JTextField();
        Styles.styleField(txtPhone);
        Styles.setPlaceholder(txtPhone, "Ej. 3001112233 (opcional)");
        fixSize(txtPhone);

        cmbRoute = new JComboBox<>();
        cmbRoute.setFont(Styles.font(Font.PLAIN, 14f));
        cmbRoute.setBackground(Styles.FIELD_BACKGROUND);
        fixSize(cmbRoute);

        btnSave = new JButton("Guardar");
        Styles.styleButton(btnSave);
        btnSave.setFont(Styles.font(Font.BOLD, 15f));
        btnSave.addActionListener(e -> save());
        btnSave.setPreferredSize(new Dimension(FORM_WIDTH, 44));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 4, 4, 4);
        int row = 0;
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
        form.add(fieldLabel("Licencia (opcional):"), constraints);
        constraints.gridy = ++row;
        form.add(txtLicense, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Teléfono (opcional):"), constraints);
        constraints.gridy = ++row;
        form.add(txtPhone, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Ruta asignada:"), constraints);
        constraints.gridy = ++row;
        form.add(cmbRoute, constraints);
        constraints.gridy = ++row;
        form.add(Box.createVerticalStrut(10), constraints);
        constraints.gridy = ++row;
        form.add(btnSave, constraints);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Color.WHITE);
        center.add(form);
        add(center, BorderLayout.CENTER);
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
     * Carga las rutas del combo desde RouteService.
     */
    public void refresh() {
        try {
            List<Route> routes = routeService.getRoutes();
            cmbRoute.removeAllItems();
            for (Route route : routes) {
                cmbRoute.addItem(new RouteOption(route.getId(), RouteFormatter.format(route)));
            }
        } catch (SQLException e) {
            new ErrorView("No se pudieron cargar las rutas. "
                    + "Revisa la conexión con la base de datos.").setVisible(true);
        }
    }

    /** Valida el formato en la vista y guarda en un hilo aparte. */
    private void save() {
        String nationalId = Styles.getFieldText(txtNationalId);
        String name = Styles.getFieldText(txtName);
        String lastName = Styles.getFieldText(txtLastName);
        String license = Styles.getFieldText(txtLicense);
        String phone = Styles.getFieldText(txtPhone);
        RouteOption route = cmbRoute.getItemCount() > 0
                ? (RouteOption) cmbRoute.getSelectedItem() : null;

        boolean complete = !nationalId.isEmpty() && !name.isEmpty()
                && !lastName.isEmpty() && route != null;
        Styles.markField(txtNationalId, !nationalId.isEmpty());
        Styles.markField(txtName, !name.isEmpty());
        Styles.markField(txtLastName, !lastName.isEmpty());
        if (!complete) {
            new ErrorView("Cédula, nombre, apellido y ruta son obligatorios.").setVisible(true);
            return;
        }
        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return driverService.addDriver(nationalId, name, lastName, license, phone, route.id());
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (get()) {
                        new ConfirmationView("Conductor registrado con éxito.").setVisible(true);
                        onDriverChanged.run();
                        clearForm();
                    } else {
                        new ErrorView("No se pudo guardar el conductor. Intenta de nuevo.").setVisible(true);
                    }
                } catch (ExecutionException e) {
                    handleFailure(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Muestra el mensaje del servicio (dato repetido, sin permiso) o el error de base de datos. */
    private void handleFailure(Throwable cause) {
        if (cause instanceof IllegalArgumentException || cause instanceof SecurityException) {
            new ErrorView(cause.getMessage()).setVisible(true);
        } else {
            LOGGER.log(Level.SEVERE, "Error inesperado al guardar el conductor", cause);
            new ErrorView("No se pudo guardar el conductor. "
                    + "Revisa la conexión con la base de datos.").setVisible(true);
        }
    }

    /** Deja los campos en su aspecto neutro, sin borrar los textos de ejemplo. */
    private void clearForm() {
        clearField(txtNationalId);
        clearField(txtName);
        clearField(txtLastName);
        clearField(txtLicense);
        clearField(txtPhone);
    }

    private static void clearField(JTextField field) {
        field.setText("");
        field.setForeground(Styles.TEXT);
        field.setBackground(Styles.FIELD_BACKGROUND);
        field.setBorder(new Styles.RoundedBorder(Styles.FIELD_BORDER, 8, 1, 10));
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? "Guardando..." : "Guardar");
    }

    /** Opción del combo de rutas: muestra el texto de la ruta y guarda su id. */
    private record RouteOption(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}