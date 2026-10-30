package com.vektra.view;

import com.vektra.model.LineColor;
import com.vektra.model.Passenger;
import com.vektra.model.Route;
import com.vektra.model.Ticket;
import com.vektra.service.RouteService;
import com.vektra.service.TicketService;
import com.vektra.util.Styles;
import com.vektra.util.TicketFormatter;
import com.vektra.util.UserSession;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Pantalla "Mis tickets": muestra solo los tickets del pasajero con sesión
 * iniciada, del más reciente al más antiguo.
 *
 * Antes se mostraban los tickets de todos los pasajeros y el color de la
 * línea siempre salía azul. Ahora cada ticket muestra el color real de la
 * línea de su ruta, y el código es el mismo que llega por correo.
 */
public class ActiveTicketsPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = Logger.getLogger(ActiveTicketsPanel.class.getName());

    /** Títulos de las columnas de la tabla. */
    private static final String[] COLUMNS = {"Código", "Origen", "Destino", "Línea", "Fecha", "Precio"};

    /** Ancho inicial de cada columna en píxeles: más espacio para los nombres de estación. */
    private static final int[] COLUMN_WIDTHS = {120, 230, 230, 120, 130, 80};

    /** Posición de la columna "Línea", que se pinta con el color de la línea. */
    private static final int LINE_COLUMN = 3;

    private static final String LOAD_ERROR =
            "No se pudieron cargar tus tickets. Revisa la conexión e intenta de nuevo.";

    /** Consulta de los tickets del pasajero. */
    private final TicketService ticketService;

    /** Rutas del metro, para saber la línea de cada ticket. */
    private final RouteService routeService;

    /** Filas de la tabla; no se pueden editar desde la pantalla. */
    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    /**
     * Crea la pantalla y empieza a cargar los tickets del pasajero.
     *
     * @param ticketService servicio que consulta los tickets
     * @param routeService  servicio que entrega las rutas
     */
    public ActiveTicketsPanel(TicketService ticketService, RouteService routeService) {
        this.ticketService = ticketService;
        this.routeService = routeService;
        initComponents();
        tblTickets.setModel(tableModel);
        for (int column = 0; column < COLUMN_WIDTHS.length; column++) {
            tblTickets.getColumnModel().getColumn(column).setPreferredWidth(COLUMN_WIDTHS[column]);
        }
        tblTickets.getColumnModel().getColumn(LINE_COLUMN).setCellRenderer(new LineCellRenderer());
        applyStyles();
        btnRefresh.addActionListener(event -> refresh());
        refresh();
    }

    /**
     * Vuelve a cargar los tickets. La ventana principal puede llamarlo
     * después de una compra para que el ticket nuevo aparezca.
     */
    public void refresh() {
        Passenger passenger;
        try {
            passenger = UserSession.getActivePassenger();
        } catch (IllegalStateException e) {
            showStatus("Inicia sesión como pasajero para ver tus tickets.");
            return;
        }
        btnRefresh.setEnabled(false);
        showStatus("Cargando tus tickets…");
        new SwingWorker<List<TicketRow>, Void>() {
            @Override
            protected List<TicketRow> doInBackground() throws Exception {
                List<Ticket> tickets = ticketService.getPassengerTickets(passenger);
                List<Route> routes = routeService.getRoutes();
                List<TicketRow> rows = new ArrayList<>();
                for (Ticket ticket : tickets) {
                    rows.add(new TicketRow(ticket, findLine(ticket, routes)));
                }
                return rows;
            }

            @Override
            protected void done() {
                btnRefresh.setEnabled(true);
                try {
                    showTickets(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    LOGGER.log(Level.WARNING, "No se pudieron cargar los tickets", e.getCause());
                    showStatus(LOAD_ERROR);
                }
            }
        }.execute();
    }

    /** Da a los textos, la tabla y el botón la fuente y los colores de Vektra. */
    private void applyStyles() {
        setBackground(Color.WHITE);
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);
        lblSubtitle.setFont(Styles.font(Font.PLAIN, 14f));
        lblSubtitle.setForeground(Styles.LABEL);
        lblStatus.setFont(Styles.font(Font.PLAIN, 14f));
        lblStatus.setForeground(Styles.LABEL);
        tblTickets.setFont(Styles.font(Font.PLAIN, 13f));
        tblTickets.getTableHeader().setFont(Styles.font(Font.BOLD, 13f));
        tblTickets.setForeground(Styles.TEXT);
        tblTickets.setRowHeight(32);
        tblTickets.setShowVerticalLines(false);
        tblTickets.setGridColor(Styles.FIELD_BORDER);
        scrTickets.getViewport().setBackground(Color.WHITE);
        btnRefresh.setFont(Styles.font(Font.BOLD, 14f));
        Styles.styleButton(btnRefresh);
    }

    /**
     * Llena la tabla, o muestra un aviso si el pasajero aún no tiene tickets.
     *
     * @param rows tickets con su línea
     */
    private void showTickets(List<TicketRow> rows) {
        tableModel.setRowCount(0);
        for (TicketRow row : rows) {
            Ticket ticket = row.ticket();
            tableModel.addRow(new Object[] {
                ticket.getCode(),
                ticket.getOriginName(),
                ticket.getDestinationName(),
                row.line(),
                TicketFormatter.formatDate(ticket.getPurchaseDate()),
                TicketFormatter.formatPrice(ticket.getPrice())
            });
        }
        showStatus(rows.isEmpty() ? "Todavía no tienes tickets. Cuando compres uno, aparecerá aquí." : "");
    }

    /**
     * Muestra un aviso debajo del título; con texto vacío lo oculta.
     *
     * @param message aviso para el usuario
     */
    private void showStatus(String message) {
        lblStatus.setText(message);
        lblStatus.setVisible(!message.isEmpty());
    }

    /**
     * Busca la línea de la ruta que une el origen y el destino del ticket.
     *
     * @param ticket ticket del pasajero
     * @param routes todas las rutas del metro
     * @return la línea, o null si ninguna ruta une esas estaciones
     */
    private static LineColor findLine(Ticket ticket, List<Route> routes) {
        for (Route route : routes) {
            if (route.connects(ticket.getOriginId(), ticket.getDestinationId())) {
                return route.getLineColor();
            }
        }
        return null;
    }

    /** Un ticket junto con la línea de su ruta (null si no hay ruta directa). */
    private record TicketRow(Ticket ticket, LineColor line) {
    }

    /** Pinta la columna "Línea" con un punto del color real de la línea. */
    private static final class LineCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof LineColor line) {
                setText(line.getDisplayName());
                setIcon(new LineDot(line.toColor()));
            } else {
                setText("Sin ruta directa");
                setIcon(null);
            }
            return this;
        }
    }

    /** Círculo pequeño del color de una línea. */
    private record LineDot(Color color) implements Icon {

        /** Diámetro del círculo en píxeles. */
        private static final int SIZE = 12;

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, SIZE, SIZE);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
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

        lblTitle = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        scrTickets = new javax.swing.JScrollPane();
        tblTickets = new javax.swing.JTable();
        btnRefresh = new javax.swing.JButton();

        lblTitle.setText("Mis tickets");

        lblSubtitle.setText("Tus tickets comprados, del más reciente al más antiguo.");

        lblStatus.setText("Cargando tus tickets…");

        scrTickets.setViewportView(tblTickets);

        btnRefresh.setText("Actualizar");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle)
                    .addComponent(lblSubtitle)
                    .addComponent(lblStatus)
                    .addComponent(scrTickets, javax.swing.GroupLayout.DEFAULT_SIZE, 760, Short.MAX_VALUE)
                    .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(40, 40, 40))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(lblTitle)
                .addGap(6, 6, 6)
                .addComponent(lblSubtitle)
                .addGap(18, 18, 18)
                .addComponent(lblStatus)
                .addGap(12, 12, 12)
                .addComponent(scrTickets, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnRefresh;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblSubtitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JScrollPane scrTickets;
    private javax.swing.JTable tblTickets;
    // End of variables declaration//GEN-END:variables
}