package com.vektra.view;

import com.vektra.model.Route;
import java.text.DecimalFormat;

/**
 * Convierte una ruta en el texto que ven las pantallas.
 *
 * Antes este texto lo armaba el propio modelo con formatoUI(); ahora el
 * modelo solo guarda datos y la presentación vive en la capa de vista (SRP).
 */
public final class RouteFormatter {

    /** Patrón de kilómetros y minutos: hasta un decimal, sin ceros sobrantes. */
    private static final String NUMBER_PATTERN = "0.#";

    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private RouteFormatter() {
    }

    /**
     * Texto de una línea con todos los datos de la ruta, por ejemplo
     * "Terminal → Centro | 4,5 km | 12 min | Línea Roja | Transbordos: 0".
     *
     * @param route ruta a mostrar
     * @return texto para combos, listas y ventanas emergentes
     */
    public static String format(Route route) {
        // DecimalFormat no es seguro entre hilos, por eso se crea en cada llamada.
        DecimalFormat number = new DecimalFormat(NUMBER_PATTERN);
        return route.getOriginName() + " → " + route.getDestinationName()
                + " | " + number.format(route.getKilometers()) + " km"
                + " | " + number.format(route.getTravelMinutes()) + " min"
                + " | Línea " + route.getLineName()
                + " | Transbordos: " + route.getTransfers();
    }
}
