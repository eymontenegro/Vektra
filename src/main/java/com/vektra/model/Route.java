package com.vektra.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ruta del metro entre una estación de origen y una de destino.
 *
 * Guarda las estaciones intermedias en orden, la línea, el tiempo de viaje,
 * los kilómetros y los transbordos tal como están en la base de datos.
 * No sabe cómo se muestra en pantalla: de eso se encarga la vista.
 */
public class Route {

    /** Identificador de la ruta en la base de datos. */
    private final int id;

    /** Estación donde empieza la ruta. */
    private final Station origin;

    /** Estación donde termina la ruta. */
    private final Station destination;

    /** Estaciones por las que pasa la ruta entre el origen y el destino, en orden. */
    private final List<Station> intermediateStations = new ArrayList<>();

    /** Línea a la que pertenece la ruta. */
    private final LineColor lineColor;

    /** Tiempo de viaje en minutos. */
    private final double travelMinutes;

    /** Distancia recorrida en kilómetros. */
    private final double kilometers;

    /** Número de cambios de línea durante el viaje. */
    private final int transfers;

    /**
     * Crea una ruta con todos sus datos.
     *
     * @param id                   identificador en la base de datos
     * @param origin               estación de origen, obligatoria
     * @param destination          estación de destino, obligatoria y distinta del origen
     * @param intermediateStations estaciones intermedias en orden; null se toma como ninguna
     * @param lineColor            línea de la ruta, obligatoria
     * @param travelMinutes        tiempo de viaje, mayor que cero
     * @param kilometers           distancia, mayor que cero
     * @param transfers            transbordos, cero o más
     * @throws IllegalArgumentException si algún dato no es válido
     */
    public Route(int id, Station origin, Station destination, List<Station> intermediateStations,
            LineColor lineColor, double travelMinutes, double kilometers, int transfers) {
        if (origin == null || destination == null) {
            throw new IllegalArgumentException("La ruta debe tener estación de origen y de destino.");
        }
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("El origen y el destino de la ruta deben ser distintos.");
        }
        if (lineColor == null) {
            throw new IllegalArgumentException("La ruta debe tener una línea.");
        }
        if (travelMinutes <= 0 || kilometers <= 0 || transfers < 0) {
            throw new IllegalArgumentException("El tiempo, la distancia o los transbordos de la ruta no son válidos.");
        }
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        if (intermediateStations != null) {
            this.intermediateStations.addAll(intermediateStations);
        }
        this.lineColor = lineColor;
        this.travelMinutes = travelMinutes;
        this.kilometers = kilometers;
        this.transfers = transfers;
    }

    public int getId() {
        return id;
    }

    public Station getOrigin() {
        return origin;
    }

    public Station getDestination() {
        return destination;
    }

    /**
     * Nombre de la estación de origen, para no encadenar
     * route.getOrigin().getName() en las vistas.
     *
     * @return nombre del origen
     */
    public String getOriginName() {
        return origin.getName();
    }

    /**
     * Nombre de la estación de destino.
     *
     * @return nombre del destino
     */
    public String getDestinationName() {
        return destination.getName();
    }

    /**
     * Estaciones intermedias, en el orden en que se recorren.
     *
     * @return lista de solo lectura, nunca null
     */
    public List<Station> getIntermediateStations() {
        return Collections.unmodifiableList(intermediateStations);
    }

    /**
     * Todas las paradas de la ruta en orden: origen, intermedias y destino.
     * El mapa la usa para dibujar el recorrido completo.
     *
     * @return lista de solo lectura con todas las paradas
     */
    public List<Station> getStops() {
        List<Station> stops = new ArrayList<>();
        stops.add(origin);
        stops.addAll(intermediateStations);
        stops.add(destination);
        return Collections.unmodifiableList(stops);
    }

    /**
     * Indica si la ruta une las dos estaciones, en cualquier sentido.
     *
     * @param firstStationId  id de una de las estaciones
     * @param secondStationId id de la otra estación
     * @return true si la ruta va de una a la otra o al revés
     */
    public boolean connects(int firstStationId, int secondStationId) {
        int originId = origin.getId();
        int destinationId = destination.getId();
        return (originId == firstStationId && destinationId == secondStationId)
                || (originId == secondStationId && destinationId == firstStationId);
    }

    public LineColor getLineColor() {
        return lineColor;
    }

    /**
     * Nombre de la línea en español, por ejemplo "Roja".
     *
     * @return nombre de la línea que ve el usuario
     */
    public String getLineName() {
        return lineColor.getDisplayName();
    }

    public double getTravelMinutes() {
        return travelMinutes;
    }

    public double getKilometers() {
        return kilometers;
    }

    public int getTransfers() {
        return transfers;
    }
}
