package com.vektra.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estación del metro con su ubicación y las líneas que pasan por ella.
 *
 * Guarda la latitud y la longitud para que el mapa no tenga que consultarlas
 * aparte, y la lista de líneas siempre existe (puede estar vacía, nunca es
 * null). Dos estaciones son iguales si tienen el mismo id.
 */
public class Station {

    /** Identificador de la estación en la base de datos. */
    private final int id;

    /** Nombre de la estación, por ejemplo "Plaza Alfonso López". */
    private final String name;

    /** Latitud en grados, entre -90 y 90. */
    private final double latitude;

    /** Longitud en grados, entre -180 y 180. */
    private final double longitude;

    /** Líneas que pasan por la estación. */
    private final List<LineColor> lines = new ArrayList<>();

    /**
     * Crea una estación con todos sus datos.
     *
     * @param id        identificador en la base de datos
     * @param name      nombre de la estación, obligatorio
     * @param latitude  latitud en grados
     * @param longitude longitud en grados
     * @param lines     líneas que pasan por la estación; null se toma como ninguna
     * @throws IllegalArgumentException si el nombre está vacío o las
     *                                  coordenadas están fuera de rango
     */
    public Station(int id, String name, double latitude, double longitude, List<LineColor> lines) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("La estación debe tener nombre.");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Las coordenadas de la estación '" + name + "' no son válidas.");
        }
        this.id = id;
        this.name = name.trim();
        this.latitude = latitude;
        this.longitude = longitude;
        if (lines != null) {
            this.lines.addAll(lines);
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Líneas que pasan por la estación.
     *
     * @return lista de solo lectura, nunca null
     */
    public List<LineColor> getLines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Station station)) {
            return false;
        }
        return id == station.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /**
     * Devuelve el nombre, que es lo que muestran los combos de estaciones.
     */
    @Override
    public String toString() {
        return name;
    }
}