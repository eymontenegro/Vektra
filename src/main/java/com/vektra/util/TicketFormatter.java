package com.vektra.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Convierte los datos de un ticket en el texto que ve el usuario.
 *
 * El modelo {@code Ticket} solo guarda datos; el formato de la fecha y del
 * precio vive aquí para que lo compartan el correo y las pantallas (SRP).
 */
public final class TicketFormatter {

    /** Formato de la fecha de compra, por ejemplo 29/10/2026 18:30. */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Idioma y país del formato del precio: punto de miles, como en 2.500. */
    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    /** Constructor privado: esta clase solo tiene métodos estáticos. */
    private TicketFormatter() {
    }

    /**
     * Da formato a la fecha y hora de compra.
     *
     * @param date fecha y hora de compra; puede ser null
     * @return la fecha como dd/MM/yyyy HH:mm, o texto vacío si es null
     */
    public static String formatDate(LocalDateTime date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    /**
     * Da formato de pesos colombianos al precio.
     *
     * @param price precio del ticket; puede ser null
     * @return el precio como $2.500, o texto vacío si es null
     */
    public static String formatPrice(BigDecimal price) {
        // NumberFormat no es seguro entre hilos, por eso se crea en cada llamada.
        return price == null ? "" : "$" + NumberFormat.getIntegerInstance(COLOMBIA).format(price);
    }
}