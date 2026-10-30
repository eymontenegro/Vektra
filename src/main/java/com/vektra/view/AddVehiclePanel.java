package com.vektra.view;

import com.vektra.model.Driver;
import com.vektra.model.LineColor;
import com.vektra.service.DriverService;
import com.vektra.service.VehicleService;
import com.vektra.util.Styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.Year;
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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;

/**
 * Formulario para registrar un vehículo. Guarda de verdad con
 * VehicleService y muestra el error si el guardado falla. Los combos de
 * línea y conductor tienen valores válidos por defecto, así que nunca
 * bloquean el envío.
 */
public class AddVehiclePanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(AddVehiclePanel.class.getName());

    private static final int FORM_WIDTH = 340;
    private static final int FIELD_HEIGHT = 40;

    private final VehicleService vehicleService;
    private final DriverService driverService;

    private JTextField txtPlate;
    private JTextField txtBrand;
    private JTextField txtModel;
    private JSpinner spnYear;
    private JSpinner spnCapacity;
    private JComboBox<LineOption> cmbLine;
    private JComboBox<DriverOption> cmbDriver;
    private JButton btnSave;

    /**
     * @param vehicleService servicio que guarda el vehículo
     * @param driverService  servicio que entrega los conductores del combo
     */
    public AddVehiclePanel(VehicleService vehicleService, DriverService driverService) {
        super(new BorderLayout(10, 10));
        this.vehicleService = vehicleService;
        this.driverService = driverService;
        setBackground(Color.WHITE);
        initComponents();
    }

    /** Arma el título y el formulario centrado. */
    private void initComponents() {
        JLabel lblTitle = new JLabel("Añadir vehículo");
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        txtPlate = new JTextField();
        Styles.styleField(txtPlate);
        Styles.setPlaceholder(txtPlate, "Ej. VKT-003");
        fixSize(txtPlate);

        txtBrand = new JTextField();
        Styles.styleField(txtBrand);
        Styles.setPlaceholder(txtBrand, "Ej. Siemens (opcional)");
        fixSize(txtBrand);

        txtModel = new JTextField();
        Styles.styleField(txtModel);
        Styles.setPlaceholder(txtModel, "Ej. Inspiro (opcional)");
        fixSize(txtModel);

        spnYear = new JSpinner(new SpinnerNumberModel(
                Year.now().getValue(), 1900, Year.now().getValue() + 1, 1));
        styleSpinner(spnYear);
        fixSize(spnYear);

        spnCapacity = new JSpinner(new SpinnerNumberModel(50, 1, 2000, 1));
        styleSpinner(spnCapacity);
        fixSize(spnCapacity);

        cmbLine = new JComboBox<>();
        cmbLine.setFont(Styles.font(Font.PLAIN, 14f));
        cmbLine.setBackground(Styles.FIELD_BACKGROUND);
        for (LineColor line : LineColor.values()) {
            cmbLine.addItem(new LineOption(line));
        }
        fixSize(cmbLine);

        cmbDriver = new JComboBox<>();
        cmbDriver.setFont(Styles.font(Font.PLAIN, 14f));
        cmbDriver.setBackground(Styles.FIELD_BACKGROUND);
        fixSize(cmbDriver);

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
        form.add(fieldLabel("Placa:"), constraints);
        constraints.gridy = ++row;
        form.add(txtPlate, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Marca (opcional):"), constraints);
        constraints.gridy = ++row;
        form.add(txtBrand, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Modelo (opcional):"), constraints);
        constraints.gridy = ++row;
        form.add(txtModel, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Año:"), constraints);
        constraints.gridy = ++row;
        form.add(spnYear, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Capacidad:"), constraints);
        constraints.gridy = ++row;
        form.add(spnCapacity, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Línea:"), constraints);
        constraints.gridy = ++row;
        form.add(cmbLine, constraints);
        constraints.gridy = ++row;
        form.add(fieldLabel("Conductor:"), constraints);
        constraints.gridy = ++row;
        form.add(cmbDriver, constraints);
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

    private static void styleSpinner(JSpinner spinner) {
        spinner.setFont(Styles.font(Font.PLAIN, 14f));
        spinner.setBackground(Styles.FIELD_BACKGROUND);
        spinner.setBorder(new Styles.RoundedBorder(Styles.FIELD_BORDER, 8, 1, 10));
    }

    /** Carga los conductores del combo. Los valores por defecto ya son válidos. */
    public void refresh() {
        try {
            cmbDriver.removeAllItems();
            for (Driver driver : driverService.getDrivers()) {
                cmbDriver.addItem(new DriverOption(driver,
                        driver.getFullName() + " - " + driver.getNationalId()));
            }
        } catch (SQLException e) {
            new ErrorView("No se pudieron cargar los conductores. "
                    + "Revisa la conexión con la base de datos.").setVisible(true);
        }
    }

    /** Valida el formato en la vista y guarda en un hilo aparte. */
    private void save() {
        String plate = Styles.getFieldText(txtPlate);
        String brand = Styles.getFieldText(txtBrand);
        String model = Styles.getFieldText(txtModel);
        int year = ((Number) spnYear.getValue()).intValue();
        int capacity = ((Number) spnCapacity.getValue()).intValue();
        LineOption line = (LineOption) cmbLine.getSelectedItem();
        DriverOption driver = cmbDriver.getItemCount() > 0
                ? (DriverOption) cmbDriver.getSelectedItem() : null;

        Styles.markField(txtPlate, !plate.isEmpty());
        if (plate.isEmpty() || line == null || driver == null) {
            new ErrorView("La placa, la línea y el conductor son obligatorios.").setVisible(true);
            return;
        }
        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return vehicleService.addVehicle(plate, brand, model, year,
                        capacity, line.line(), driver.driver());
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (get()) {
                        new ConfirmationView("Vehículo registrado con éxito.").setVisible(true);
                        clearForm();
                    } else {
                        new ErrorView("No se pudo guardar el vehículo. Intenta de nuevo.").setVisible(true);
                    }
                } catch (ExecutionException e) {
                    handleFailure(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Muestra el mensaje del servicio (placa repetida, conductor inexistente, sin permiso). */
    private void handleFailure(Throwable cause) {
        if (cause instanceof IllegalArgumentException || cause instanceof SecurityException) {
            new ErrorView(cause.getMessage()).setVisible(true);
        } else {
            LOGGER.log(Level.SEVERE, "Error inesperado al guardar el vehículo", cause);
            new ErrorView("No se pudo guardar el vehículo. "
                    + "Revisa la conexión con la base de datos.").setVisible(true);
        }
    }

    private void clearForm() {
        txtPlate.setText("");
        txtPlate.setForeground(Styles.TEXT);
        txtPlate.setBackground(Styles.FIELD_BACKGROUND);
        txtPlate.setBorder(new Styles.RoundedBorder(Styles.FIELD_BORDER, 8, 1, 10));
        txtBrand.setText("");
        txtModel.setText("");
    }

    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? "Guardando..." : "Guardar");
    }

    /** Opción del combo de líneas: guarda la línea y muestra su nombre en español. */
    private record LineOption(LineColor line) {
        @Override
        public String toString() {
            return line.getDisplayName();
        }
    }

    /** Opción del combo de conductores: guarda el conductor completo. */
    private record DriverOption(Driver driver, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}