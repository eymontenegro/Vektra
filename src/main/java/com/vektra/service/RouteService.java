package com.vektra.service;

import com.vektra.dao.RouteDao;
import com.vektra.dao.StationDao;
import com.vektra.exception.DataAccessException;
import com.vektra.exception.ValidationException;
import com.vektra.model.Route;
import com.vektra.model.Station;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entrega las estaciones y las rutas del metro.
 *
 * El mapa, la compra de tickets y el registro de conductores solo hablan con
 * esta clase y nunca con los DAO. Si la base de datos falla lanza
 * DataAccessException con un mensaje en español; el detalle queda en el registro.
 */
public class RouteService {

    private static final Logger LOGGER = Logger.getLogger(RouteService.class.getName());

    private static final String LOAD_ERROR =
            "No se pudieron cargar las estaciones y rutas. Intenta de nuevo en unos minutos.";

    /** Acceso a las rutas en la base de datos. */
    private final RouteDao routeDao;

    /** Acceso a las estaciones en la base de datos. */
    private final StationDao stationDao;

    /**
     * Crea el servicio con los DAO que usará.
     *
     * @param routeDao   DAO de las rutas
     * @param stationDao DAO de las estaciones
     */
    public RouteService(RouteDao routeDao, StationDao stationDao) {
        this.routeDao = routeDao;
        this.stationDao = stationDao;
    }

    /**
     * Todas las estaciones, ordenadas por nombre.
     *
     * @return estaciones con coordenadas y líneas
     * @throws DataAccessException si falla la base de datos
     */
    public List<Station> getStations() {
        try {
            return stationDao.findAll();
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Busca una estación por su id.
     *
     * @param stationId id de la estación
     * @return la estación, o vacío si no existe
     * @throws DataAccessException si falla la base de datos
     */
    public Optional<Station> findStation(int stationId) {
        return getStations().stream().filter(station -> station.getId() == stationId).findFirst();
    }

    /**
     * Todas las rutas con sus estaciones.
     *
     * @return rutas registradas
     * @throws DataAccessException si falla la base de datos
     */
    public List<Route> getRoutes() {
        try {
            return routeDao.findAll(indexById(stationDao.findAll()));
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Indica si existe una ruta con ese id.
     *
     * @param routeId id de la ruta
     * @return true si existe
     * @throws DataAccessException si falla la base de datos
     */
    public boolean routeExists(int routeId) {
        try {
            return routeDao.existsById(routeId);
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Busca la ruta que une dos estaciones, en cualquier sentido, para
     * mostrar su tiempo, distancia y transbordos antes de comprar.
     *
     * @param originId      id de la estación de origen
     * @param destinationId id de la estación de destino
     * @return la ruta, o vacío si ninguna ruta une esas estaciones
     * @throws ValidationException si el origen y el destino son la misma estación
     * @throws DataAccessException si falla la base de datos
     */
    public Optional<Route> findRoute(int originId, int destinationId) {
        if (originId == destinationId) {
            throw new ValidationException("El origen y el destino deben ser estaciones distintas.");
        }
        return findRoute(getRoutes(), originId, destinationId);
    }

    /**
     * Busca, dentro de una lista ya cargada, la ruta que une dos estaciones.
     * Sirve para no consultar la base una vez por cada ticket.
     *
     * @param routes        rutas ya cargadas
     * @param originId      id de una estación
     * @param destinationId id de la otra
     * @return la ruta, o vacío si ninguna las une
     */
    public static Optional<Route> findRoute(List<Route> routes, int originId, int destinationId) {
        return routes.stream().filter(route -> route.connects(originId, destinationId)).findFirst();
    }

    /** Indexa las estaciones por su id para que RouteDao las encuentre rápido. */
    private Map<Integer, Station> indexById(List<Station> stations) {
        Map<Integer, Station> stationsById = new HashMap<>();
        for (Station station : stations) {
            stationsById.put(station.getId(), station);
        }
        return stationsById;
    }

    /** Registra el error técnico y devuelve uno con mensaje para el usuario. */
    private DataAccessException failure(SQLException e) {
        LOGGER.log(Level.SEVERE, "Fallo de base de datos en RouteService", e);
        return new DataAccessException(LOAD_ERROR, e);
    }
}
