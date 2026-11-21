/*
 * Mapa de estaciones y rutas de Vektra (página /map).
 *
 * Pide los datos a /api/map y los dibuja con Leaflet sobre OpenStreetMap.
 * Reemplaza al map.html del WebView de JavaFX: ya no hacen falta los
 * ajustes para el WebView (L_DISABLE_3D, sin animaciones), porque ahora
 * el mapa corre en un navegador real.
 *
 * Todo texto que viene del servidor se pone con textContent, así nunca se
 * interpreta como HTML.
 */
(function () {
    'use strict';

    /** Centro de Valledupar, donde empieza el mapa antes de tener datos. */
    var VALLEDUPAR = [10.4631, -73.2532];
    var INITIAL_ZOOM = 13;

    function showNotice(message) {
        var notice = document.getElementById('mapNotice');
        notice.textContent = message;
        notice.hidden = false;
    }

    /** Contenido de una ventana emergente, armado sin innerHTML. */
    function popupContent(title, detail) {
        var container = document.createElement('div');
        var titleElement = document.createElement('strong');
        titleElement.textContent = title;
        container.appendChild(titleElement);
        if (detail) {
            var detailElement = document.createElement('div');
            detailElement.textContent = detail;
            container.appendChild(detailElement);
        }
        return container;
    }

    function drawRoutes(layer, routes) {
        routes.forEach(function (route) {
            L.polyline(route.path, {color: route.color, weight: 5, opacity: 0.85})
                .bindPopup(popupContent(route.label))
                .addTo(layer);
        });
    }

    function drawStations(layer, stations) {
        stations.forEach(function (station) {
            var lines = station.lines.length > 0
                ? 'Líneas: ' + station.lines.join(', ')
                : 'Sin rutas asignadas';
            L.circleMarker([station.lat, station.lng], {
                radius: 7, color: '#191717', weight: 2, fillColor: '#FFFFFF', fillOpacity: 1
            })
                .bindPopup(popupContent(station.name, lines))
                .addTo(layer);
        });
    }

    /** Leyenda con un punto de color por cada línea que aparece en el mapa. */
    function drawLegend(routes) {
        var legend = document.getElementById('lineLegend');
        var seen = {};
        routes.forEach(function (route) {
            if (seen[route.color]) {
                return;
            }
            seen[route.color] = true;
            var item = document.createElement('li');
            var dot = document.createElement('span');
            dot.className = 'line-dot';
            dot.style.background = route.color;
            item.appendChild(dot);
            // La etiqueta trae "... | Línea Roja | ...": se muestra solo el nombre de la línea.
            var lineText = route.label.split('|').map(function (part) { return part.trim(); })
                .filter(function (part) { return part.indexOf('Línea') === 0; })[0] || route.label;
            item.appendChild(document.createTextNode(lineText));
            legend.appendChild(item);
        });
    }

    function start() {
        if (typeof L === 'undefined') {
            showNotice('El mapa necesita conexión a internet. Revisa tu conexión y recarga la página.');
            return;
        }
        var map = L.map('map').setView(VALLEDUPAR, INITIAL_ZOOM);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; colaboradores de OpenStreetMap'
        }).addTo(map);
        var layer = L.layerGroup().addTo(map);

        fetch('/api/map', {headers: {'Accept': 'application/json'}})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.json();
            })
            .then(function (data) {
                drawRoutes(layer, data.routes);
                drawStations(layer, data.stations);
                drawLegend(data.routes);
                if (data.stations.length > 0) {
                    map.fitBounds(data.stations.map(function (station) {
                        return [station.lat, station.lng];
                    }), {padding: [30, 30]});
                } else {
                    showNotice('Todavía no hay estaciones registradas.');
                }
            })
            .catch(function () {
                showNotice('No se pudieron cargar las estaciones y rutas. Intenta de nuevo en unos minutos.');
            });
    }

    document.addEventListener('DOMContentLoaded', start);
})();
