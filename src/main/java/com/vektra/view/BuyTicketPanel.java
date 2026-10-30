package com.vektra.view;

import com.vektra.model.Passenger;
import com.vektra.model.Route;
import com.vektra.model.Station;
import com.vektra.model.Ticket;
import com.vektra.service.RouteService;
import com.vektra.service.TicketService;
import com.vektra.service.TicketService.PurchaseResult;
import com.vektra.util.Styles;
import com.vektra.util.TicketFormatter;
import com.vektra.util.UserSession;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingWorker;

/**
 * Pantalla donde el pasajero con sesión iniciada compra un ticket.
 *
 * Solo pide el origen y el destino: el pasajero sale de la sesión (antes se
 * volvían a pedir nombre, cédula, correo y contraseña). Las estaciones, la
 * ruta y la compra se consultan fuera del hilo de la interfaz con SwingWorker,
 * y la confirmación solo aparece si el ticket se guardó de verdad.
 */
public class BuyTicketPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = Logger.getLogger(BuyTicketPanel.class.getName());

    private static final String STATIONS_ERROR =
            "No se pudieron cargar las estaciones. Revise la conexión con la base de datos.";

    private static final String UNEXPECTED_ERROR =
            "No se pudo completar la compra. Intenta de nuevo.";

    /** Compra y guardado de tickets. */
    private final TicketService ticketService;

    /** Estaciones y rutas del metro. */
    private final RouteService routeService;

    /** Consulta de la ruta en curso, para cancelarla si el usuario cambia de estación. */
    private SwingWorker<Optional<Route>, Void> routeWorker;

    /**
     * Crea la pantalla de compra y empieza a cargar las estaciones.
     *
     * @param ticketService servicio que compra el ticket
     * @param routeService  servicio que entrega las estaciones y las rutas
     */
    public BuyTicketPanel(TicketService ticketService, RouteService routeService) {
        this.ticketService = ticketService;
        this.routeService = routeService;
        initComponents();
        applyStyles();
        cmbOrigin.addActionListener(event -> showRouteInfo());
        cmbDestination.addActionListener(event -> showRouteInfo());
        btnBuy.addActionListener(event -> buyTicket());
        showPassenger();
        loadStations();
    }

    /** Da a los textos y al botón la fuente y los colores de Vektra. */
    private void applyStyles() {
        setBackground(Color.WHITE);
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);
        lblSubtitle.setFont(Styles.font(Font.PLAIN, 14f));
        lblSubtitle.setForeground(Styles.LABEL);
        lblPassenger.setFont(Styles.font(Font.PLAIN, 14f));
        lblPassenger.setForeground(Styles.TEXT);
        lblOrigin.setFont(Styles.font(Font.BOLD, 13f));
        lblOrigin.setForeground(Styles.LABEL);
        lblDestination.setFont(Styles.font(Font.BOLD, 13f));
        lblDestination.setForeground(Styles.LABEL);
        cmbOrigin.setFont(Styles.font(Font.PLAIN, 14f));
        cmbDestination.setFont(Styles.font(Font.PLAIN, 14f));
        lblRouteInfo.setFont(Styles.font(Font.PLAIN, 13f));
        lblRouteInfo.setForeground(Styles.LABEL);
        btnBuy.setFont(Styles.font(Font.BOLD, 15f));
        Styles.styleButton(btnBuy);
    }

    /** Muestra quién está comprando, o bloquea la compra si no hay pasajero. */
    private void showPassenger() {
        try {
            Passenger passenger = UserSession.getActivePassenger();
            lblPassenger.setText("Comprando como " + passenger.getFullName() + " (" + passenger.getEmail() + ")");
        } catch (IllegalStateException e) {
            lblPassenger.setText("Inicia sesión como pasajero para comprar.");
            btnBuy.setEnabled(false);
        }
    }

    /** Carga las estaciones en los dos combos sin congelar la pantalla. */
    private void loadStations() {
        new SwingWorker<List<Station>, Void>() {
            @Override
            protected List<Station> doInBackground() throws Exception {
                return routeService.getStations();
            }

            @Override
            protected void done() {
                try {
                    List<Station> stations = get();
                    cmbOrigin.setModel(new DefaultComboBoxModel<>(stations.toArray(Station[]::new)));
                    cmbDestination.setModel(new DefaultComboBoxModel<>(stations.toArray(Station[]::new)));
                    clearSelection();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    LOGGER.log(Level.WARNING, "No se pudieron cargar las estaciones", e.getCause());
                    new ErrorView(STATIONS_ERROR).setVisible(true);
                }
            }
        }.execute();
    }

    /** Deja los combos sin estación elegida. */
    private void clearSelection() {
        cmbOrigin.setSelectedIndex(-1);
        cmbDestination.setSelectedIndex(-1);
        lblRouteInfo.setText("Elige la estación de origen y la de destino.");
    }

    /** Muestra el tiempo, la distancia y la línea del viaje elegido. */
    private void showRouteInfo() {
        Station origin = (Station) cmbOrigin.getSelectedItem();
        Station destination = (Station) cmbDestination.getSelectedItem();
        if (routeWorker != null) {
            routeWorker.cancel(true);
        }
        if (origin == null || destination == null) {
            lblRouteInfo.setText("Elige la estación de origen y la de destino.");
            return;
        }
        if (origin.equals(destination)) {
            lblRouteInfo.setText("El origen y el destino deben ser estaciones distintas.");
            return;
        }
        lblRouteInfo.setText("Buscando la ruta…");
        routeWorker = new SwingWorker<>() {
            @Override
            protected Optional<Route> doInBackground() throws Exception {
                return routeService.findRoute(origin.getId(), destination.getId());
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }
                try {
                    lblRouteInfo.setText(get()
                            .map(RouteFormatter::format)
                            .orElse("No hay una ruta directa entre estas estaciones."));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    LOGGER.log(Level.WARNING, "No se pudo consultar la ruta", e.getCause());
                    lblRouteInfo.setText("No se pudo consultar la ruta.");
                }
            }
        };
        routeWorker.execute();
    }

    /** Compra el ticket del pasajero de la sesión fuera del hilo de la interfaz. */
    private void buyTicket() {
        Station origin = (Station) cmbOrigin.getSelectedItem();
        Station destination = (Station) cmbDestination.getSelectedItem();
        Passenger passenger;
        try {
            passenger = UserSession.getActivePassenger();
        } catch (IllegalStateException e) {
            new ErrorView("Inicia sesión como pasajero para comprar.").setVisible(true);
            return;
        }
        btnBuy.setEnabled(false);
        btnBuy.setText("Comprando…");
        new SwingWorker<PurchaseResult, Void>() {
            @Override
            protected PurchaseResult doInBackground() {
                return ticketService.buyTicket(passenger, origin, destination);
            }

            @Override
            protected void done() {
                btnBuy.setText("Comprar ticket");
                btnBuy.setEnabled(true);
                try {
                    showPurchaseResult(get());
                    clearSelection();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    showPurchaseError(e.getCause());
                }
            }
        }.execute();
    }

        /**
     * Confirma la compra con el código del ticket y avisa si el correo no salió.
     *
     * @param result ticket guardado y si se envió el correo
     */
    private void showPurchaseResult(PurchaseResult result) {
        Ticket ticket = result.ticket();
        String message = "Compraste el ticket " + ticket.getCode()
                + " por " + TicketFormatter.formatPrice(ticket.getPrice()) + ".";
        if (result.emailSent()) {
            message += " Te lo enviamos a " + ticket.getPassengerEmail() + ".";
        } else {
            message += " No pudimos enviarlo por correo: guarda el código, también lo verás en Mis tickets.";
        }
        new ConfirmationView(message).setVisible(true);
    }

    /**
     * Muestra el error de la compra. Los mensajes del servicio ya están
     * escritos para el usuario: datos inválidos o fallo de la base de datos.
     *
     * @param error causa del fallo
     */
    private void showPurchaseError(Throwable error) {
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) {
            new ErrorView(error.getMessage()).setVisible(true);
            return;
        }
        LOGGER.log(Level.SEVERE, "Error inesperado al comprar el ticket", error);
        new ErrorView(UNEXPECTED_ERROR).setVisible(true);
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
        lblPassenger = new javax.swing.JLabel();
        lblOrigin = new javax.swing.JLabel();
        cmbOrigin = new javax.swing.JComboBox<>();
        lblDestination = new javax.swing.JLabel();
        cmbDestination = new javax.swing.JComboBox<>();
        lblRouteInfo = new javax.swing.JLabel();
        btnBuy = new javax.swing.JButton();

        lblTitle.setText("Comprar un ticket");

        lblSubtitle.setText("Elige tu origen y tu destino. El ticket llegará a tu correo.");

        lblPassenger.setText("Comprando como…");

        lblOrigin.setText("Estación de origen");

        lblDestination.setText("Estación de destino");

        lblRouteInfo.setText("Elige la estación de origen y la de destino.");

        btnBuy.setText("Comprar ticket");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle)
                    .addComponent(lblSubtitle)
                    .addComponent(lblPassenger)
                    .addComponent(lblOrigin)
                    .addComponent(cmbOrigin, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblDestination)
                    .addComponent(cmbDestination, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblRouteInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBuy, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(40, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(lblTitle)
                .addGap(6, 6, 6)
                .addComponent(lblSubtitle)
                .addGap(18, 18, 18)
                .addComponent(lblPassenger)
                .addGap(24, 24, 24)
                .addComponent(lblOrigin)
                .addGap(6, 6, 6)
                .addComponent(cmbOrigin, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblDestination)
                .addGap(6, 6, 6)
                .addComponent(cmbDestination, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblRouteInfo)
                .addGap(24, 24, 24)
                .addComponent(btnBuy, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(30, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuy;
    private javax.swing.JComboBox<Station> cmbDestination;
    private javax.swing.JComboBox<Station> cmbOrigin;
    private javax.swing.JLabel lblDestination;
    private javax.swing.JLabel lblOrigin;
    private javax.swing.JLabel lblPassenger;
    private javax.swing.JLabel lblRouteInfo;
    private javax.swing.JLabel lblSubtitle;
    private javax.swing.JLabel lblTitle;
    // End of variables declaration//GEN-END:variables
}

