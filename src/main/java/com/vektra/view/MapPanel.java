package com.vektra.view;

import com.vektra.service.RouteService;
import com.vektra.util.Styles;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Pestaña "Conoce nuestras rutas": un título arriba y el mapa debajo.
 *
 * Crea un solo MapView (antes se cargaban dos) y se arma por código, porque
 * el formulario de NetBeans que tenía antes se borraba con removeAll().
 */
public class MapPanel extends JPanel {

    /**
     * Crea la pestaña del mapa.
     *
     * @param routeService servicio que el mapa usa para obtener estaciones y rutas
     */
    public MapPanel(RouteService routeService) {
        super(new BorderLayout());
        setBackground(Color.WHITE);
        add(createHeader(), BorderLayout.NORTH);
        add(new MapView(routeService), BorderLayout.CENTER);
    }

    /** Arma el título y el subtítulo de la pestaña. */
    private JPanel createHeader() {
        JLabel lblTitle = new JLabel("Conoce nuestras rutas");
        lblTitle.setFont(Styles.font(Font.BOLD, 24f));
        lblTitle.setForeground(Styles.TEXT);

        JLabel lblSubtitle = new JLabel("Contamos con muchas estaciones, te llevamos a donde quieras.");
        lblSubtitle.setFont(Styles.font(Font.PLAIN, 14f));
        lblSubtitle.setForeground(Styles.LABEL);

        JPanel pnlHeader = new JPanel();
        pnlHeader.setLayout(new BoxLayout(pnlHeader, BoxLayout.Y_AXIS));
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        pnlHeader.add(lblTitle);
        pnlHeader.add(lblSubtitle);
        return pnlHeader;
    }
}
