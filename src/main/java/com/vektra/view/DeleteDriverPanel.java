package com.vektra.view;

import com.vektra.model.Driver;
import com.vektra.service.DriverService;
import com.vektra.util.Styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * Pantalla para borrar un conductor. Pide doble confirmación, el combo
 * guarda el objeto completo (no se saca el id partiendo el texto) y si el
 * conductor tiene vehículos asignados el Service impide el borrado.
 */
public class DeleteDriverPanel extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(DeleteDriverPanel.class.getName());

    private static final int FORM_WIDTH = 340;
    private static final int FIELD_HEIGHT = 40;

    private final DriverService driverService;
    private final Runnable onDriverChanged;

    private JComboBox<DriverOption> cmbDriver;
    private JButton btnDelete;

    /**
     * @param driverService   servicio que borra el conductor
     * @param onDriverChanged se ejecuta tras borrar, para refrescar la lista
     */
    public DeleteDriverPanel(DriverService driverService, Runnable onDriverChanged) {
        super(new BorderLayout(10, 10));
        this.driverService = driverService;
        this.onDriverChanged = onDriverChanged;
        setBackground(Color.WHITE);
        initComponents();
    }

    /** Arma el título, el combo y el botón. */
    private void initComponents() {
        JLabel lblTitle = new JLabel("Eliminar conductor");
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        cmbDriver = new JComboBox<>();
        cmbDriver.setFont(Styles.font(Font.PLAIN, 14f));
        cmbDriver.setBackground(Styles.FIELD_BACKGROUND);
        fixSize(cmbDriver);

        btnDelete = new JButton("Borrar conductor");
        Styles.styleButton(btnDelete);
        btnDelete.setFont(Styles.font(Font.BOLD, 15f));
        btnDelete.addActionListener(e -> deleteSelected());
        btnDelete.setPreferredSize(new Dimension(FORM_WIDTH, 44));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(4, 4, 4, 4);
        form.add(fieldLabel("Conductor:"), constraints);
        constraints.gridy = 1;
        form.add(cmbDriver, constraints);
        constraints.gridy = 2;
        form.add(Box.createVerticalStrut(10), constraints);
        constraints.gridy = 3;
        form.add(btnDelete, constraints);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Color.WHITE);
        center.add(form);
        add(center, BorderLayout.CENTER);
    }

    /** Etiqueta que va encima del combo. */
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

    /** Carga los conductores en el combo, con el objeto completo. */
    public void refresh() {
        try {
            cmbDriver.removeAllItems();
            for (Driver driver : driverService.getDrivers()) {
                cmbDriver.addItem(new DriverOption(driver,
                        driver.getFullName() + " - " + driver.getNationalId()));
            }
        } catch (SQLException e) {
            new ErrorView("No se pudo cargar la lista de conductores. "
                    + "Revisa la conexión con la base de datos.").setVisible(true);
        }
    }

    /** Doble confirmación y borrado en un hilo aparte. */
    private void deleteSelected() {
        DriverOption selected = cmbDriver.getItemCount() > 0
                ? (DriverOption) cmbDriver.getSelectedItem() : null;
        if (selected == null) {
            new ErrorView("No hay conductores registrados.").setVisible(true);
            return;
        }
        int first = JOptionPane.showConfirmDialog(this,
                "¿Seguro que quieres borrar a " + selected.label() + "?",
                "Borrar conductor", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (first != JOptionPane.YES_OPTION) {
            return;
        }
        int second = JOptionPane.showConfirmDialog(this,
                "Esta acción no se puede deshacer. ¿Borrar de todas formas?",
                "Confirmar borrado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (second != JOptionPane.YES_OPTION) {
            return;
        }
        setBusy(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return driverService.deleteDriver(selected.id());
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    if (get()) {
                        new ConfirmationView("Conductor eliminado con éxito.").setVisible(true);
                        onDriverChanged.run();
                        refresh();
                    } else {
                        new ErrorView("El conductor ya no existe. Actualiza la lista.").setVisible(true);
                    }
                } catch (ExecutionException e) {
                    handleFailure(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Muestra el mensaje del servicio (vehículos asignados, sin permiso) o el error de base de datos. */
    private void handleFailure(Throwable cause) {
        if (cause instanceof IllegalArgumentException || cause instanceof SecurityException) {
            new ErrorView(cause.getMessage()).setVisible(true);
        } else {
            LOGGER.log(Level.SEVERE, "Error inesperado al borrar el conductor", cause);
            new ErrorView("No se pudo borrar el conductor. "
                    + "Revisa la conexión con la base de datos.").setVisible(true);
        }
    }

    private void setBusy(boolean busy) {
        btnDelete.setEnabled(!busy);
        btnDelete.setText(busy ? "Borrando..." : "Borrar conductor");
    }

    /** Opción del combo: guarda el conductor completo y muestra su nombre y cédula. */
    private record DriverOption(Driver driver, String label) {

        /** @return id del conductor guardado en esta opción */
        int id() {
            return driver.getId();
        }

        @Override
        public String toString() {
            return label;
        }
    }
}