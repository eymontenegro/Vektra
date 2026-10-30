
package com.vektra.view;

import com.vektra.model.Driver;
import com.vektra.service.DriverService;
import com.vektra.util.Styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Lista de conductores registrados. Solo habla con DriverService.
 * Se refresca al abrirse y cada vez que se añade o borra un conductor.
 */
public class DriversPanel extends JPanel {

    /** Columnas de la tabla de conductores. */
    private static final String[] COLUMNS =
            {"Cédula", "Nombre", "Apellido", "Licencia", "Teléfono", "Estado"};

    private final DriverService driverService;
    private final DefaultTableModel tableModel;
    private final JTable tblDrivers;

    /**
     * @param driverService servicio con la lista de conductores
     */
    public DriversPanel(DriverService driverService) {
        super(new BorderLayout(10, 10));
        this.driverService = driverService;
        setBackground(Color.WHITE);
        add(createHeader(), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblDrivers = new JTable(tableModel);
        tblDrivers.setFillsViewportHeight(true);
        tblDrivers.setRowHeight(24);
        tblDrivers.getTableHeader().setFont(Styles.font(Font.BOLD, 14f));
        tblDrivers.setFont(Styles.font(Font.PLAIN, 14f));
        add(new JScrollPane(tblDrivers), BorderLayout.CENTER);
    }

    /** Título de la pestaña y botón de actualización. */
    private JPanel createHeader() {
        JLabel lblTitle = new JLabel("Conductores");
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);

        JButton btnRefresh = new JButton("Actualizar");
        Styles.styleButton(btnRefresh);
        btnRefresh.setFont(Styles.font(Font.BOLD, 14f));
        btnRefresh.addActionListener(e -> refresh());

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        header.add(lblTitle, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        return header;
    }

    /**
     * Carga la lista de conductores. Si falla la consulta se muestra el
     * error y la tabla queda como estaba.
     */
    public void refresh() {
        try {
            List<Driver> drivers = driverService.getDrivers();
            tableModel.setRowCount(0);
            for (Driver driver : drivers) {
                tableModel.addRow(new Object[] {
                        driver.getNationalId(),
                        driver.getName(),
                        driver.getLastName(),
                        driver.getLicenseNumber(),
                        driver.getPhone(),
                        driver.getStatus()
                });
            }
        } catch (SQLException e) {
            new ErrorView("No se pudo cargar la lista de conductores. "
                    + "Revisa la conexión con la base de datos.").setVisible(true);
        }
    }
}