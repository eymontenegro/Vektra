package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.LineColor;
import com.vektra.model.Route;
import com.vektra.model.Station;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta las rutas del metro con sus estaciones intermedias.
 *
 * Los kilómetros, el color de la línea y los transbordos se leen de la tabla
 * routes (antes se inventaban: tiempo × 1.2 y cero transbordos). Las
 * estaciones las recibe ya cargadas, así no repite la lectura de StationDao.
 */
public class RouteDao {

    private static final String SELECT_ROUTES = """
            SELECT id, origin_station_id, destination_station_id,
                   line_color, travel_minutes, kilometers, transfers
            FROM routes
            ORDER BY id
            """;

    /** Estaciones intermedias de todas las rutas, en el orden en que se recorren. */
    private static final String SELECT_INTERMEDIATE_STATIONS = """
            SELECT route_id, station_id
            FROM route_stations
            ORDER BY route_id, position
            """;

    /**
     * Devuelve todas las rutas.
     *
     * @param stationsById estaciones existentes, indexadas por su id
     * @return rutas con origen, destino y estaciones intermedias
     * @throws SQLException si falla la consulta o una ruta apunta a una
     *                      estación que no está en el mapa recibido
     */
    public List<Route> findAll(Map<Integer, Station> stationsById) throws SQLException {
        List<Route> routes = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection()) {
            Map<Integer, List<Station>> intermediatesByRoute =
                    findIntermediateStations(connection, stationsById);
            try (PreparedStatement statement = connection.prepareStatement(SELECT_ROUTES);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    routes.add(mapRow(resultSet, stationsById, intermediatesByRoute));
                }
            }
        }
        return routes;
    }

    /**
     * Lee las estaciones intermedias de todas las rutas en una sola consulta.
     *
     * @param connection   conexión abierta por findAll
     * @param stationsById estaciones existentes, indexadas por su id
     * @return estaciones intermedias en orden, agrupadas por id de ruta
     * @throws SQLException si falla la consulta
     */
    private Map<Integer, List<Station>> findIntermediateStations(Connection connection,
            Map<Integer, Station> stationsById) throws SQLException {
        Map<Integer, List<Station>> intermediatesByRoute = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_INTERMEDIATE_STATIONS);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Station station = requireStation(stationsById, resultSet.getInt("station_id"));
                intermediatesByRoute
                        .computeIfAbsent(resultSet.getInt("route_id"), id -> new ArrayList<>())
                        .add(station);
            }
        }
        return intermediatesByRoute;
    }

    /**
     * Convierte la fila actual en una ruta.
     *
     * @param resultSet            resultado posicionado en una ruta
     * @param stationsById         estaciones existentes, indexadas por su id
     * @param intermediatesByRoute estaciones intermedias agrupadas por ruta
     * @return ruta leída
     * @throws SQLException si falta una columna o una estación
     */
    private Route mapRow(ResultSet resultSet, Map<Integer, Station> stationsById,
            Map<Integer, List<Station>> intermediatesByRoute) throws SQLException {
        int id = resultSet.getInt("id");
        return new Route(
                id,
                requireStation(stationsById, resultSet.getInt("origin_station_id")),
                requireStation(stationsById, resultSet.getInt("destination_station_id")),
                intermediatesByRoute.get(id),
                LineColor.fromCode(resultSet.getString("line_color")),
                resultSet.getDouble("travel_minutes"),
                resultSet.getDouble("kilometers"),
                resultSet.getInt("transfers"));
    }

    /**
     * Busca una estación por id y falla con un mensaje claro si no está.
     *
     * @param stationsById estaciones existentes, indexadas por su id
     * @param stationId    id de la estación buscada
     * @return estación encontrada
     * @throws SQLException si la estación no existe
     */
    private Station requireStation(Map<Integer, Station> stationsById, int stationId) throws SQLException {
        Station station = stationsById.get(stationId);
        if (station == null) {
            throw new SQLException("Una ruta apunta a la estación " + stationId + ", que no existe.");
        }
        return station;
    }
}
