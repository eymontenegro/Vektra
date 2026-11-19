package com.vektra.dao;
import com.vektra.connection.DatabaseConnection;
import com.vektra.model.DashboardTotals;
import com.vektra.model.StationSales;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
Ctrl+A
 
→
 pega
/**
 * Consulta los datos del dashboard en las vistas v_dashboard_totals y
 * v_ticket_sales_by_origin (creadas por vektra.sql versión 2 o por la
 * migración V2).
 *
 * Antes se hacían cuatro consultas COUNT, una conexión por cada una; ahora
 * todos los totales llegan en una sola fila. Si la consulta falla lanza una
 * SQLException, de modo que un error nunca se confunde con un total de cero.
 */
public class DashboardDao {
    private static final String SELECT_TOTALS = """
            SELECT total_drivers, active_drivers, total_vehicles, total_stations,
                   total_routes, total_passengers, total_tickets, total_revenue
            FROM v_dashboard_totals
            """;
    private static final String SELECT_TOP_ORIGINS = """
            SELECT station_name, tickets_sold, revenue
            FROM v_ticket_sales_by_origin
            ORDER BY tickets_sold DESC, station_name
            LIMIT ?
            """;
    /**
     * Lee todos los totales del sistema.
     *
     * @return totales actuales
     * @throws SQLException si falla la consulta o la vista no existe
     */
    public DashboardTotals findTotals() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_TOTALS);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("La consulta de totales no devolvió resultado.");
            }
            return new DashboardTotals(
                    resultSet.getInt("total_drivers"),
                    resultSet.getInt("active_drivers"),
                    resultSet.getInt("total_vehicles"),
                    resultSet.getInt("total_stations"),
                    resultSet.getInt("total_routes"),
                    resultSet.getInt("total_passengers"),
                    resultSet.getInt("total_tickets"),
                    resultSet.getBigDecimal("total_revenue"));
        }
    }
    /**
     * Estaciones de origen con más tickets vendidos.
     *
     * @param limit cuántas estaciones devolver, mayor que cero
     * @return estaciones ordenadas de más a menos ventas
     * @throws SQLException si falla la consulta o la vista no existe
     */
    public List<StationSales> findTopOrigins(int limit) throws SQLException {
        List<StationSales> sales = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_TOP_ORIGINS)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    sales.add(new StationSales(
                            resultSet.getString("station_name"),
                            resultSet.getInt("tickets_sold"),
                            resultSet.getBigDecimal("revenue")));
                }
            }
        }
        return sales;
    }
}