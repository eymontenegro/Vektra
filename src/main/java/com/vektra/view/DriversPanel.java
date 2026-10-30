
package com.vektra.view;

import com.vektra.model.Driver;
import com.vektra.model.Route;
import com.vektra.service.DriverService;
import com.vektra.service.RouteService;
import com.vektra.util.FontUtil;
import com.vektra.util.Styles;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Panel con la tabla de conductores registrados. Desde aquí el administrador
 * pide añadir o borrar un conductor.
 */

public class DriversPanel extends javax.swing.JPanel {

        private static final Logger LOGGER = Logger.getLogger(DriversPanel.class.getName());

    /** Títulos de las columnas de la tabla. */
    private static final String[] COLUMNS = {"Cédula", "Nombre", "Licencia", "Teléfono", "Estado", "Ruta"};

    private final DriverService driverService;
    private final RouteService routeService;
    private final Runnable onAddRequested;
    private final Runnable onDeleteRequested;
    private final ReadOnlyTableModel tableModel = new ReadOnlyTableModel(COLUMNS);

    /**
     * Crea el panel de conductores y carga la tabla.
     *
     * @param driverService     servicio de conductores
     * @param routeService      servicio de rutas, para mostrar el nombre de cada ruta
     * @param onAddRequested    acción para abrir "Añadir conductor"; puede ser null
     * @param onDeleteRequested acción para abrir "Borrar conductor"; puede ser null
     */
    public DriversPanel(DriverService driverService, RouteService routeService,
            Runnable onAddRequested, Runnable onDeleteRequested) {
        this.driverService = driverService;
        this.routeService = routeService;
        this.onAddRequested = onAddRequested;
        this.onDeleteRequested = onDeleteRequested;
        initComponents();
        applyStyles();
        tblDrivers.setModel(tableModel);
        FontUtil.applyCustomFont(this);
        reload();
    }

    /** Aplica la paleta común de Styles a los componentes dibujados. */
    private void applyStyles() {
        setBackground(Color.WHITE);
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);
        lblSubtitle.setFont(Styles.font(Font.PLAIN, 14f));
        lblSubtitle.setForeground(Styles.LABEL);
        Styles.styleButton(btnAddDriver);
        Styles.styleButton(btnDeleteDriver);
        tblDrivers.setRowHeight(28);
    }

    /**
     * Vuelve a cargar la tabla desde la base de datos, fuera del hilo de la
     * interfaz. Se llama al abrir el panel y después de añadir o borrar.
     */
    public void reload() {
        new SwingWorker<DriverRows, Void>() {
            @Override
            protected DriverRows doInBackground() throws Exception {
                List<Driver> drivers = driverService.getDrivers();
                Map<Integer, String> routeNames = new HashMap<>();
                for (Route route : routeService.getRoutes()) {
                    routeNames.put(route.getId(), describe(route));
                }
                return new DriverRows(drivers, routeNames);
            }

            @Override
            protected void done() {
                try {
                    showRows(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showError(e.getCause());
                }
            }
        }.execute();
    }

    /** Llena la tabla con los conductores recibidos. */
    private void showRows(DriverRows rows) {
        tableModel.setRowCount(0);
        for (Driver driver : rows.drivers()) {
            tableModel.addRow(new Object[]{
                driver.getNationalId(),
                driver.getFullName(),
                valueOrDash(driver.getLicenseNumber()),
                valueOrDash(driver.getPhone()),
                statusText(driver.getStatus()),
                rows.routeNames().getOrDefault(driver.getAssignedRouteId(), "Ruta no disponible")
            });
        }
    }

    /** Avisa del error con un mensaje distinto según la causa. */
    private void showError(Throwable cause) {
        LOGGER.log(Level.WARNING, "No se pudieron cargar los conductores", cause);
        String message = cause instanceof SecurityException
                ? cause.getMessage()
                : "No se pudieron cargar los conductores. Revisa la conexión con la base de datos.";
        new ErrorView(message).setVisible(true);
    }

    /** Texto de una ruta para la tabla, por ejemplo "Terminal → Centro (Roja)". */
    private static String describe(Route route) {
        return route.getOriginName() + " → " + route.getDestinationName()
                + " (" + route.getLineName() + ")";
    }

    /** Traduce el estado guardado al texto que ve el usuario. */
    private static String statusText(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case "ACTIVE" -> "Activo";
            case "INACTIVE" -> "Inactivo";
            default -> status;
        };
    }

    /** Muestra un guion cuando el dato no existe. */
    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** Conductores y nombres de ruta que se cargan juntos en segundo plano. */
    private record DriverRows(List<Driver> drivers, Map<Integer, String> routeNames) {
    }

    /** Modelo de tabla cuyas celdas no se pueden editar. */
    private static final class ReadOnlyTableModel extends DefaultTableModel {

        ReadOnlyTableModel(String[] columns) {
            super(columns, 0);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();
        scrDrivers = new javax.swing.JScrollPane();
        tblDrivers = new javax.swing.JTable();
        pnlActions = new javax.swing.JPanel();
        btnAddDriver = new javax.swing.JButton();
        btnDeleteDriver = new javax.swing.JButton();

        setPreferredSize(new java.awt.Dimension(900, 600));
        setLayout(new java.awt.BorderLayout());

        pnlHeader.setPreferredSize(new java.awt.Dimension(100, 70));
        pnlHeader.setLayout(new java.awt.GridLayout(2, 1));

        lblTitle.setText("Conductores");
        pnlHeader.add(lblTitle);

        lblSubtitle.setText("Consulta los conductores registrados y su ruta");
        pnlHeader.add(lblSubtitle);

        add(pnlHeader, java.awt.BorderLayout.PAGE_START);

        tblDrivers.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        scrDrivers.setViewportView(tblDrivers);

        add(scrDrivers, java.awt.BorderLayout.CENTER);

        pnlActions.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnAddDriver.setText("Añadir conductor");
        btnAddDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddDriverActionPerformed(evt);
            }
        });
        pnlActions.add(btnAddDriver);

        btnDeleteDriver.setText("Borrar conductor");
        btnDeleteDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteDriverActionPerformed(evt);
            }
        });
        pnlActions.add(btnDeleteDriver);

        add(pnlActions, java.awt.BorderLayout.PAGE_END);
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddDriverActionPerformed
       if (onAddRequested != null) {
            onAddRequested.run();
        }
    }//GEN-LAST:event_btnAddDriverActionPerformed

    private void btnDeleteDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteDriverActionPerformed
               if (onDeleteRequested != null) {
            onDeleteRequested.run();
        }
    }//GEN-LAST:event_btnDeleteDriverActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddDriver;
    private javax.swing.JButton btnDeleteDriver;
    private javax.swing.JLabel lblSubtitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JPanel pnlActions;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JScrollPane scrDrivers;
    private javax.swing.JTable tblDrivers;
    // End of variables declaration//GEN-END:variables
}
