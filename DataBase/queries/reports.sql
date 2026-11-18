-- =========================================================
-- VEKTRA - Consultas de reporte
--
-- Consultas de solo lectura para revisar la base y para la sustentación
-- de Bases de Datos. Ninguna modifica datos.
-- Ejecutar: psql -U postgres -d Vektra -f DataBase/queries/reports.sql
-- =========================================================

-- 1. Recorrido completo de cada ruta, en orden (JOIN de 4 tablas + UNION).
--    Origen = posición 0, intermedias = su posición, destino = última.
SELECT r.id AS ruta, r.line_color AS linea, stop.position AS orden, s.name AS estacion
FROM routes r
JOIN LATERAL (
    SELECT r.origin_station_id AS station_id, 0 AS position
    UNION ALL
    SELECT rs.station_id, rs.position FROM route_stations rs WHERE rs.route_id = r.id
    UNION ALL
    SELECT r.destination_station_id, 32767
) stop ON TRUE
JOIN stations s ON s.id = stop.station_id
ORDER BY r.id, stop.position;

-- 2. Resumen de rutas (vista).
SELECT * FROM v_route_summary ORDER BY route_id;

-- 3. Conductores con su ruta y número de vehículos (vista).
SELECT * FROM v_driver_assignments ORDER BY last_name, name;

-- 4. Conductores activos que no tienen ningún vehículo (NOT EXISTS).
SELECT dr.national_id, dr.name, dr.last_name
FROM drivers dr
WHERE dr.status = 'ACTIVE'
  AND NOT EXISTS (SELECT 1 FROM vehicles v WHERE v.driver_id = dr.id);

-- 5. Capacidad total de pasajeros por línea (GROUP BY).
SELECT line AS linea, COUNT(*) AS vehiculos, SUM(capacity) AS capacidad_total
FROM vehicles
GROUP BY line
ORDER BY capacidad_total DESC;

-- 6. Estaciones por las que pasa más de una línea (GROUP BY + HAVING).
SELECT s.name AS estacion, COUNT(DISTINCT x.line_color) AS lineas
FROM stations s
JOIN (
    SELECT origin_station_id AS station_id, line_color FROM routes
    UNION SELECT destination_station_id, line_color FROM routes
    UNION SELECT rs.station_id, r.line_color
          FROM route_stations rs JOIN routes r ON r.id = rs.route_id
) x ON x.station_id = s.id
GROUP BY s.name
HAVING COUNT(DISTINCT x.line_color) > 1
ORDER BY lineas DESC, estacion;

-- 7. Ventas por estación de origen (vista).
SELECT * FROM v_ticket_sales_by_origin ORDER BY tickets_sold DESC, station_name;

-- 8. Ventas por día de los últimos 30 días.
SELECT purchase_date::date AS dia, COUNT(*) AS tickets, SUM(price) AS recaudo
FROM tickets
WHERE purchase_date >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY purchase_date::date
ORDER BY dia;

-- 9. Ranking de pasajeros por número de compras (función de ventana).
SELECT u.name || ' ' || u.last_name AS pasajero,
       COUNT(t.id) AS compras,
       RANK() OVER (ORDER BY COUNT(t.id) DESC) AS puesto
FROM users u
LEFT JOIN tickets t ON t.passenger_id = u.id
WHERE u.role = 'PASSENGER'
GROUP BY u.id, u.name, u.last_name
ORDER BY puesto;

-- 10. Comprobación de la regla de la línea del vehículo: vehículos cuya
--     línea no es la de la ruta de su conductor (debe devolver 0 filas).
SELECT v.plate, v.line AS linea_vehiculo, r.line_color AS linea_ruta_conductor
FROM vehicles v
JOIN drivers dr ON dr.id = v.driver_id
JOIN routes r ON r.id = dr.route_id
WHERE v.line <> r.line_color;

-- 11. Plan de ejecución de la consulta de "Mis tickets": debe usar
--     idx_tickets_passenger_date (con pocas filas PostgreSQL puede
--     preferir Seq Scan; es normal con datos de prueba).
EXPLAIN
SELECT t.id, t.ticket_code, t.purchase_date
FROM tickets t
WHERE t.passenger_id = 1
ORDER BY t.purchase_date DESC;