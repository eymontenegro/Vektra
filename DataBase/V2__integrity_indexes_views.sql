-- =========================================================
-- VEKTRA - Migración V2: integridad, índices, triggers y vistas
-- PostgreSQL 14 o superior.
--
-- Esta migración NO borra tablas ni datos. Se ejecuta sobre una base
-- que ya tiene el esquema de DataBase/vektra.sql (versión 1).
--
-- Orden:
--   0. Verificaciones previas: si hay datos que romperían una regla,
--      la migración se detiene y no cambia nada.
--   1. Restricciones nuevas (CHECK y UNIQUE).
--   2. Índices para las llaves foráneas.
--   3. Triggers de las paradas de ruta.
--   4. Vistas que usan el dashboard y los reportes.
--
-- Todo va dentro de una sola transacción: o se aplica completa o no se
-- aplica nada.
-- =========================================================

BEGIN;

-- =========================================================
-- 0. VERIFICACIONES PREVIAS
-- Cada bloque cuenta las filas que violarían la regla nueva y, si hay
-- alguna, detiene la migración con un mensaje que dice qué corregir.
-- =========================================================

DO $$
DECLARE
    bad_rows INTEGER;
BEGIN
    SELECT COUNT(*) INTO bad_rows FROM users WHERE role = 'DRIVER';
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % usuarios con rol DRIVER. Los conductores viven en la tabla drivers; corríjalos antes de migrar.', bad_rows;
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM users
    WHERE role = 'PASSENGER'
      AND (national_id IS NULL OR last_name IS NULL OR birth_date IS NULL);
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % pasajeros sin cédula, apellido o fecha de nacimiento.', bad_rows;
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM users
    WHERE national_id IS NOT NULL AND national_id !~ '^[0-9]{6,12}$';
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % usuarios con cédula que no tiene entre 6 y 12 dígitos.', bad_rows;
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM drivers WHERE national_id !~ '^[0-9]{6,12}$';
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % conductores con cédula que no tiene entre 6 y 12 dígitos.', bad_rows;
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM drivers
    WHERE phone IS NOT NULL AND phone !~ '^[0-9]{7,15}$';
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % conductores con teléfono que no tiene entre 7 y 15 dígitos.', bad_rows;
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM (
        SELECT 1 FROM routes
        GROUP BY origin_station_id, destination_station_id, line_color
        HAVING COUNT(*) > 1) duplicated;
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % rutas repetidas (mismo origen, destino y línea).', bad_rows;
    END IF;

    SELECT COUNT(*) INTO bad_rows FROM (
        SELECT 1 FROM route_stations
        GROUP BY route_id, station_id
        HAVING COUNT(*) > 1) duplicated;
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % estaciones repetidas dentro de una misma ruta.', bad_rows;
    END IF;

    SELECT COUNT(*) INTO bad_rows
    FROM route_stations rs
    JOIN routes r ON r.id = rs.route_id
    WHERE rs.station_id IN (r.origin_station_id, r.destination_station_id);
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % paradas intermedias iguales al origen o al destino de su ruta.', bad_rows;
    END IF;
END $$;

-- =========================================================
-- 1. RESTRICCIONES NUEVAS
-- =========================================================

-- Los conductores no inician sesión: el rol DRIVER no pertenece a users.
ALTER TABLE users DROP CONSTRAINT chk_users_role;
ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'PASSENGER'));

-- Un pasajero siempre tiene cédula, apellido y fecha de nacimiento.
-- El administrador puede no tenerlos.
ALTER TABLE users
    ADD CONSTRAINT chk_users_passenger_data CHECK (
        role <> 'PASSENGER'
        OR (national_id IS NOT NULL AND last_name IS NOT NULL AND birth_date IS NOT NULL));

-- La cédula son solo dígitos (misma regla que PassengerService).
ALTER TABLE users
    ADD CONSTRAINT chk_users_national_id CHECK (national_id ~ '^[0-9]{6,12}$');
ALTER TABLE drivers
    ADD CONSTRAINT chk_drivers_national_id CHECK (national_id ~ '^[0-9]{6,12}$');

-- El teléfono, si existe, son solo dígitos.
ALTER TABLE drivers
    ADD CONSTRAINT chk_drivers_phone CHECK (phone ~ '^[0-9]{7,15}$');

-- Formato mínimo de correo: algo@algo.algo
ALTER TABLE users
    ADD CONSTRAINT chk_users_email_format CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$');

-- La fecha de nacimiento no puede ser anterior a 1900. Que no sea futura lo
-- valida PassengerService: un CHECK no debe depender de la fecha actual.
ALTER TABLE users
    ADD CONSTRAINT chk_users_birth_date CHECK (birth_date IS NULL OR birth_date >= DATE '1900-01-01');

-- No puede haber dos rutas con el mismo origen, destino y línea.
ALTER TABLE routes
    ADD CONSTRAINT uq_routes_origin_destination_line
        UNIQUE (origin_station_id, destination_station_id, line_color);

-- Una estación aparece una sola vez como parada intermedia de cada ruta.
ALTER TABLE route_stations
    ADD CONSTRAINT uq_route_stations_route_station UNIQUE (route_id, station_id);

-- =========================================================
-- 2. ÍNDICES PARA LAS LLAVES FORÁNEAS
-- PostgreSQL no crea índices para las llaves foráneas. Sin ellos, cada
-- JOIN y cada borrado en la tabla padre recorre la tabla hija completa.
-- tickets.passenger_id ya está cubierto por idx_tickets_passenger_date.
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_drivers_route ON drivers (route_id);
CREATE INDEX IF NOT EXISTS idx_vehicles_driver ON vehicles (driver_id);
CREATE INDEX IF NOT EXISTS idx_routes_destination ON routes (destination_station_id);
-- routes.origin_station_id queda cubierto por el índice de
-- uq_routes_origin_destination_line, que empieza por esa columna.
CREATE INDEX IF NOT EXISTS idx_route_stations_station ON route_stations (station_id);
CREATE INDEX IF NOT EXISTS idx_tickets_origin ON tickets (origin_station_id);
CREATE INDEX IF NOT EXISTS idx_tickets_destination ON tickets (destination_station_id);
-- El dashboard cuenta los tickets por fecha de compra.
CREATE INDEX IF NOT EXISTS idx_tickets_purchase_date ON tickets (purchase_date);

-- =========================================================
-- 3. TRIGGERS
-- Una restricción CHECK no puede mirar otra tabla. Para impedir que una
-- parada intermedia sea el origen o el destino de su propia ruta se usa
-- un trigger.
-- =========================================================

CREATE OR REPLACE FUNCTION fn_check_route_station()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    route_origin      BIGINT;
    route_destination BIGINT;
BEGIN
    SELECT origin_station_id, destination_station_id
    INTO route_origin, route_destination
    FROM routes
    WHERE id = NEW.route_id;

    IF NEW.station_id IN (route_origin, route_destination) THEN
        RAISE EXCEPTION 'La parada intermedia no puede ser el origen ni el destino de la ruta %.', NEW.route_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_route_stations_check ON route_stations;
CREATE TRIGGER trg_route_stations_check
    BEFORE INSERT OR UPDATE ON route_stations
    FOR EACH ROW EXECUTE FUNCTION fn_check_route_station();

-- Si se cambia el origen o el destino de una ruta, sus paradas
-- intermedias tienen que seguir siendo válidas.
CREATE OR REPLACE FUNCTION fn_check_route_endpoints()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM route_stations
               WHERE route_id = NEW.id
                 AND station_id IN (NEW.origin_station_id, NEW.destination_station_id)) THEN
        RAISE EXCEPTION 'La ruta % ya tiene esa estación como parada intermedia.', NEW.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_routes_check_endpoints ON routes;
CREATE TRIGGER trg_routes_check_endpoints
    BEFORE UPDATE OF origin_station_id, destination_station_id ON routes
    FOR EACH ROW EXECUTE FUNCTION fn_check_route_endpoints();

-- =========================================================
-- 4. VISTAS
-- =========================================================

-- Resumen de cada ruta con los nombres de sus estaciones.
CREATE OR REPLACE VIEW v_route_summary AS
SELECT r.id                AS route_id,
       r.line_color,
       o.name              AS origin_name,
       d.name              AS destination_name,
       COUNT(rs.station_id) AS intermediate_stops,
       r.travel_minutes,
       r.kilometers,
       r.transfers
FROM routes r
JOIN stations o ON o.id = r.origin_station_id
JOIN stations d ON d.id = r.destination_station_id
LEFT JOIN route_stations rs ON rs.route_id = r.id
GROUP BY r.id, r.line_color, o.name, d.name, r.travel_minutes, r.kilometers, r.transfers;

-- Conductores con su ruta y cuántos vehículos tienen asignados.
CREATE OR REPLACE VIEW v_driver_assignments AS
SELECT dr.id               AS driver_id,
       dr.national_id,
       dr.name,
       dr.last_name,
       dr.status,
       r.id                AS route_id,
       r.line_color        AS route_line,
       COUNT(v.id)         AS vehicle_count
FROM drivers dr
JOIN routes r ON r.id = dr.route_id
LEFT JOIN vehicles v ON v.driver_id = dr.id
GROUP BY dr.id, dr.national_id, dr.name, dr.last_name, dr.status, r.id, r.line_color;

-- Tickets vendidos y dinero recaudado por estación de origen.
CREATE OR REPLACE VIEW v_ticket_sales_by_origin AS
SELECT s.id                       AS station_id,
       s.name                     AS station_name,
       COUNT(t.id)                AS tickets_sold,
       COALESCE(SUM(t.price), 0)  AS revenue
FROM stations s
LEFT JOIN tickets t ON t.origin_station_id = s.id
GROUP BY s.id, s.name;

-- Totales del sistema en una sola fila, para el dashboard.
CREATE OR REPLACE VIEW v_dashboard_totals AS
SELECT (SELECT COUNT(*) FROM drivers)                          AS total_drivers,
       (SELECT COUNT(*) FROM drivers WHERE status = 'ACTIVE')  AS active_drivers,
       (SELECT COUNT(*) FROM vehicles)                         AS total_vehicles,
       (SELECT COUNT(*) FROM stations)                         AS total_stations,
       (SELECT COUNT(*) FROM routes)                           AS total_routes,
       (SELECT COUNT(*) FROM users WHERE role = 'PASSENGER')   AS total_passengers,
       (SELECT COUNT(*) FROM tickets)                          AS total_tickets,
       (SELECT COALESCE(SUM(price), 0) FROM tickets)           AS total_revenue;

COMMIT;

-- =========================================================
-- VERIFICACIÓN (fuera de la transacción)
-- Resultado esperado: 9 llaves foráneas, 2 triggers, 4 vistas.
-- =========================================================

SELECT
    (SELECT COUNT(*) FROM pg_constraint
      WHERE contype = 'f' AND connamespace = 'public'::regnamespace) AS llaves_foraneas,
    (SELECT COUNT(*) FROM pg_trigger WHERE tgname LIKE 'trg_%' AND NOT tgisinternal) AS triggers,
    (SELECT COUNT(*) FROM pg_views WHERE schemaname = 'public' AND viewname LIKE 'v_%') AS vistas;
```

