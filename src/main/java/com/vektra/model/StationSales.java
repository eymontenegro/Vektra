package com.vektra.model;

import java.math.BigDecimal;

/**
 * Ventas de tickets de una estación de origen, leídas de la vista
 * v_ticket_sales_by_origin.
 *
 * @param stationName nombre de la estación
 * @param ticketsSold tickets que salieron de esa estación
 * @param revenue     dinero recaudado por esos tickets
 */
public record StationSales(String stationName, int ticketsSold, BigDecimal revenue) {
}
