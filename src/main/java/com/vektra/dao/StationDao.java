package com.vektra.dao;

import com.vektra.connection.DatabaseConnection;
import com.vektra.model.LineColor;
import com.vektra.model.Station;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consulta las estaciones del metro con sus coordenadas y sus líneas.
 *
 * Antes este SQL estaba dentro de MapaView; ahora la vista no habla con la
 * base de datos. Las líneas de cada estación no se guardan aparte: salen de
 * las rutas que empiezan, terminan o pasan por ella, así no se repite el dato.
 */
public class StationDao {

    /**
     * Una fila por cada estación y cada línea que pasa por ella. Las
     * estaciones sin rutas aparecen una vez, con line_color nulo.
     */
    private static final String SELECT_ALL = """
            SELECT s.id, s.name, s.latitude, s.longitude, station_line.line_color
            FROM stations s
            LEFT JOIN (
                SELECT origin_station_id AS station_id, line_color FROM routes
                UNION
                SELECT destination_station_id, line_color FROM routes
                UNION
                SELECT rs.station_id, r.line_color
                FROM route_stations rs
                JOIN routes r ON r.id = rs.route_id
            ) station_line ON station_line.station_id = s.id
            ORDER BY s.name, s.id, station_line.line_color
            """;

    /**
     * Devuelve todas las estaciones ordenadas por nombre.
     *
     * @return estaciones con coordenadas y líneas; vacía si no hay ninguna
     * @throws SQLException si falla la consulta
     */
    public List<Station> findAll() throws SQLException {
        Map<Integer, StationRow> rowsById = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                StationRow row = rowsById.get(id);
                if (row == null) {
                    row = mapRow(resultSet);
                    rowsById.put(id, row);
                }
                row.addLine(resultSet.getString("line_color"));
            }
        }
        List<Station> stations = new ArrayList<>();
        for (StationRow row : rowsById.values()) {
            stations.add(row.toStation());
        }
        return stations;
    }

    /**
     * Lee los datos de la estación de la fila actual, sin sus líneas.
     *
     * @param resultSet resultado posicionado en una fila de la estación
     * @return datos de la estación
     * @throws SQLException si falta alguna columna
     */
    private StationRow mapRow(ResultSet resultSet) throws SQLException {
        return new StationRow(resultSet.getInt("id"), resultSet.getString("name"),
                resultSet.getDouble("latitude"), resultSet.getDouble("longitude"));
    }

    /**
     * Datos de una estación mientras se juntan sus líneas, que llegan en
     * varias filas del resultado.
     */
    private static final class StationRow {

        private final int id;
        private final String name;
        private final double latitude;
        private final double longitude;
        private final List<LineColor> lines = new ArrayList<>();

        StationRow(int id, String name, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        /**
         * Agrega una línea si la fila trae una.
         *
         * @param code valor de line_color, o null si la estación no tiene rutas
         */
        void addLine(String code) {
            if (code != null) {
                lines.add(LineColor.fromCode(code));
            }
        }

        Station toStation() {
            return new Station(id, name, latitude, longitude, lines);
        }
    }
}
