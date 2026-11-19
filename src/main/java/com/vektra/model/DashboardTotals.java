package com.vektra.model;
import java.math.BigDecimal;
/**
* Totales del sistema que muestra el dashboard, leídos de la vista
* v_dashboard_totals en una sola consulta.
*
* @param totalDrivers    
conductores registrados
* @param activeDrivers   conductores con estado ACTIVE
* @param totalVehicles   vehículos registrados
* @param totalStations   estaciones registradas
* @param totalRoutes     
rutas registradas
* @param totalPassengers pasajeros registrados
* @param totalTickets    
tickets vendidos
dinero recaudado por los tickets
* @param totalRevenue    
*/
public record DashboardTotals(int totalDrivers, int activeDrivers, int totalVehicles,
int totalStations, int totalRoutes, int totalPassengers,
int totalTickets, BigDecimal totalRevenue) {
}