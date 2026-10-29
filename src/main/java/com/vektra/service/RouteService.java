package com.vektra.service;

import com.vektra.dao.RouteDao;
import com.vektra.dao.StationDao;
import com.vektra.model.Route;
import com.vektra.model.Station;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entrega a las vistas las estaciones y las rutas del metro.
 *
 * El mapa, la compra de tickets y el registro de conductores solo hablan con
 * esta clase y nunca con los DAO. Si la base de datos falla se propaga la
 * SQLException para que la vista muestre el error.
 */
public class RouteService {

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
     * @throws SQLException si falla la consulta
     */
    public List<Station> getStations() throws SQLException {
        return stationDao.findAll();
    }

    /**
     * Todas las rutas con sus estaciones.
     *
     * @return rutas registradas
     * @throws SQLException si falla la consulta
     */
    public List<Route> getRoutes() throws SQLException {
        return routeDao.findAll(indexById(stationDao.findAll()));
    }

    /**
     * Busca la ruta que une dos estaciones, en cualquier sentido, para
     * mostrar su tiempo, distancia y transbordos antes de comprar.
     *
     * @param originId      id de la estación de origen
     * @param destinationId id de la estación de destino
     * @return la ruta, o vacío si ninguna ruta une esas estaciones
     * @throws IllegalArgumentException si el origen y el destino son la misma estación
     * @throws SQLException             si falla la consulta
     */
    public Optional<Route> findRoute(int originId, int destinationId) throws SQLException {
        if (originId == destinationId) {
            throw new IllegalArgumentException("El origen y el destino deben ser estaciones distintas.");
        }
        for (Route route : getRoutes()) {
            if (route.connects(originId, destinationId)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    /**
     * Indexa las estaciones por su id para que RouteDao las encuentre rápido.
     *
     * @param stations estaciones a indexar
     * @return mapa de id a estación
     */
    private Map<Integer, Station> indexById(List<Station> stations) {
        Map<Integer, Station> stationsById = new HashMap<>();
        for (Station station : stations) {
            stationsById.put(station.getId(), station);
        }
        return stationsById;
    }
}