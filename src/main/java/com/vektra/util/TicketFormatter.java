package com.vektra.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Convierte los datos de un ticket en el texto que ve el usuario.
 *
 * El modelo {@code Ticket} solo guarda datos; el formato de la fecha vive aquí
 * para que lo compartan el correo y las pantallas (SRP).
 */
public final class TicketFormatter {

    /** Formato de la fecha de compra, por ejemplo 29/10/2026 18:30. */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
}