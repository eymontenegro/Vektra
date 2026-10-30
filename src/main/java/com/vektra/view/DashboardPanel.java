package com.vektra.view;

import com.vektra.service.DashboardService;
import com.vektra.util.Styles;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * Métricas del sistema: totales de conductores, vehículos, estaciones y
 * rutas. Solo habla con DashboardService (nunca con el DAO) y solo se
 * muestra al administrador. Un error de base de datos nunca se disfraza
 * de cero: se muestra el error y los números quedan como estaban.
 */
public class DashboardPanel extends JPanel {

    private final DashboardService dashboardService;
    private final List<JLabel> valueLabels = new ArrayList<>();

    /**
     * @param dashboardService servicio con los totales
     */
    public DashboardPanel(DashboardService dashboardService) {
        super(new BorderLayout(10, 10));
        this.dashboardService = dashboardService;
        setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Dashboard");
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
        add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        valueLabels.add(createCard(grid, "Conductores"));
        valueLabels.add(createCard(grid, "Vehículos"));
        valueLabels.add(createCard(grid, "Estaciones"));
        valueLabels.add(createCard(grid, "Rutas"));
        add(grid, BorderLayout.CENTER);
    }

    /** Crea una tarjeta de métrica y devuelve la etiqueta del número. */
    private JLabel createCard(JPanel grid, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Styles.FIELD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                new Styles.RoundedBorder(Styles.FIELD_BORDER, 12, 1, 12),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Styles.font(Font.BOLD, 14f));
        lblTitle.setForeground(Styles.LABEL);
        JLabel lblValue = new JLabel("-");
        lblValue.setFont(Styles.font(Font.BOLD, 40f));
        lblValue.setForeground(Styles.PRIMARY);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        grid.add(card);
        return lblValue;
    }

    /** Carga los totales reales desde la base de datos, en un hilo aparte. */
    public void refresh() {
        new SwingWorker<int[], Void>() {
            @Override
            protected int[] doInBackground() throws SQLException {
                return new int[] {
                        dashboardService.getTotalDrivers(),
                        dashboardService.getTotalVehicles(),
                        dashboardService.getTotalStations(),
                        dashboardService.getTotalRoutes()
                };
            }

            @Override
            protected void done() {
                try {
                    int[] totals = get();
                    for (int i = 0; i < totals.length; i++) {
                        valueLabels.get(i).setText(String.valueOf(totals[i]));
                    }
                } catch (ExecutionException e) {
                    new ErrorView("No se pudieron cargar las métricas. "
                            + "Revisa la conexión con la base de datos.").setVisible(true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }
}
