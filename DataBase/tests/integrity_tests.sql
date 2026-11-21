-- =========================================================
-- VEKTRA - Pruebas de integridad de la base de datos
--
-- Intenta guardar datos inválidos y comprueba que la base los rechaza
-- con el error esperado. No deja cambios: todo termina en ROLLBACK.
--
-- Ejecutar (después de vektra.sql, o de V2 y V3 si la base ya existía):
--   psql -U postgres -d Vektra -v ON_ERROR_STOP=1 -f DataBase/tests/integrity_tests.sql
-- Resultado esperado: una línea "OK" por prueba y al final
-- "TODAS LAS PRUEBAS DE INTEGRIDAD PASARON". Si una falla, psql se
-- detiene con "FALLÓ: ...".
-- =========================================================

\pset tuples_only on
BEGIN;

-- Ejecuta una sentencia que debe fallar con el código SQLSTATE indicado.
CREATE FUNCTION pg_temp.expect_error(statement TEXT, expected_state TEXT, label TEXT)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        IF SQLSTATE = expected_state THEN
            RAISE NOTICE 'OK: %', label;
            RETURN;
        END IF;
        RAISE EXCEPTION 'FALLÓ: % (se esperaba %, llegó %: %)', label, expected_state, SQLSTATE, SQLERRM;
    END;
    RAISE EXCEPTION 'FALLÓ: % (la base aceptó un dato inválido)', label;
END;
$$;

-- Códigos SQLSTATE de PostgreSQL:
--   23505 unique_violation   23503 foreign_key_violation
--   23514 check_violation    23502 not_null_violation

SELECT pg_temp.expect_error(
    $q$INSERT INTO users (name, email, password, role) VALUES ('A', 'ADMIN@VEKTRA.COM', 'h', 'ADMIN')$q$,
    '23514', 'correo en mayúsculas rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO users (name, email, password, role) VALUES ('A', 'admin@vektra.com', 'h', 'ADMIN')$q$,
    '23505', 'correo repetido rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO users (name, email, password, role) VALUES ('A', 'sin-arroba', 'h', 'ADMIN')$q$,
    '23514', 'correo sin formato rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO users (name, email, password, role) VALUES ('A', 'x@x.com', 'h', 'DRIVER')$q$,
    '23514', 'rol DRIVER en users rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO users (name, email, password, role) VALUES ('A', 'p@x.com', 'h', 'PASSENGER')$q$,
    '23514', 'pasajero sin cédula ni fecha de nacimiento rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO users (national_id, name, last_name, email, password, role, birth_date)
       VALUES ('12AB56', 'A', 'B', 'q@x.com', 'h', 'PASSENGER', '2000-01-01')$q$,
    '23514', 'cédula con letras rechazada');

SELECT pg_temp.expect_error(
    $q$INSERT INTO drivers (national_id, name, last_name, phone, route_id)
       SELECT '1234567', 'A', 'B', 'abc', id FROM routes LIMIT 1$q$,
    '23514', 'teléfono con letras rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO drivers (national_id, name, last_name, route_id) VALUES ('1234567', 'A', 'B', 999999)$q$,
    '23503', 'conductor con ruta inexistente rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO drivers (national_id, name, last_name, status, route_id)
       SELECT '1234567', 'A', 'B', 'VACATION', id FROM routes LIMIT 1$q$,
    '23514', 'estado de conductor inválido rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO vehicles (plate, year, capacity, line, driver_id)
       SELECT 'VKT-001', 2020, 10, 'RED', id FROM drivers LIMIT 1$q$,
    '23505', 'placa repetida rechazada');

SELECT pg_temp.expect_error(
    $q$INSERT INTO vehicles (plate, year, capacity, line, driver_id)
       SELECT 'T-1', 2020, 0, 'RED', id FROM drivers LIMIT 1$q$,
    '23514', 'capacidad cero rechazada');

SELECT pg_temp.expect_error(
    $q$INSERT INTO vehicles (plate, year, capacity, line, driver_id)
       SELECT 'T-2', 2020, 10, 'PURPLE', id FROM drivers LIMIT 1$q$,
    '23514', 'línea inexistente rechazada');

SELECT pg_temp.expect_error(
    $q$DELETE FROM drivers WHERE license_number = 'LIC-0001'$q$,
    '23503', 'borrar conductor con vehículo rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO routes (origin_station_id, destination_station_id, line_color, travel_minutes, kilometers)
       SELECT origin_station_id, destination_station_id, line_color, 5, 5 FROM routes LIMIT 1$q$,
    '23505', 'ruta repetida rechazada');

SELECT pg_temp.expect_error(
    $q$INSERT INTO routes (origin_station_id, destination_station_id, line_color, travel_minutes, kilometers)
       SELECT id, id, 'RED', 5, 5 FROM stations LIMIT 1$q$,
    '23514', 'ruta con origen igual al destino rechazada');

SELECT pg_temp.expect_error(
    $q$INSERT INTO route_stations (route_id, station_id, position)
       SELECT route_id, station_id, 99 FROM route_stations LIMIT 1$q$,
    '23505', 'parada repetida en la misma ruta rechazada');

SELECT pg_temp.expect_error(
    $q$INSERT INTO route_stations (route_id, station_id, position)
       SELECT id, origin_station_id, 98 FROM routes LIMIT 1$q$,
    '23514', 'parada intermedia igual al origen rechazada (trigger)');

SELECT pg_temp.expect_error(
    $q$INSERT INTO tickets (ticket_code, passenger_id, origin_station_id, destination_station_id, price)
       SELECT 'VK-T1', 999999, id, id + 1, 2500 FROM stations LIMIT 1$q$,
    '23503', 'ticket de pasajero inexistente rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO tickets (ticket_code, passenger_id, origin_station_id, destination_station_id, price)
       SELECT 'VK-T2', u.id, s.id, s.id, 2500 FROM users u, stations s LIMIT 1$q$,
    '23514', 'ticket con origen igual al destino rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO tickets (ticket_code, passenger_id, origin_station_id, destination_station_id, price)
       SELECT 'VK-T3', u.id, s.id, s.id + 1, -1 FROM users u, stations s LIMIT 1$q$,
    '23514', 'ticket con precio negativo rechazado');

SELECT pg_temp.expect_error(
    $q$INSERT INTO stations (name, latitude, longitude) VALUES ('Fuera', 95, 0)$q$,
    '23514', 'estación con latitud fuera de rango rechazada');

SELECT pg_temp.expect_error(
    $q$INSERT INTO vehicles (plate, year, capacity, line, driver_id)
       SELECT 'T-3', 2020, 10, 'YELLOW', id FROM drivers WHERE license_number = 'LIC-0001'$q$,
    '23514', 'vehículo en una línea distinta a la ruta de su conductor rechazado (trigger)');

-- Un dato válido sí debe guardarse.
INSERT INTO users (national_id, name, last_name, email, password, role, birth_date)
VALUES ('1122334455', 'Ana', 'Ruiz', 'ana@prueba.com', 'hash', 'PASSENGER', '2001-05-10');
DO $$ BEGIN RAISE NOTICE 'OK: pasajero válido guardado'; END $$;

DO $$ BEGIN RAISE NOTICE 'TODAS LAS PRUEBAS DE INTEGRIDAD PASARON'; END $$;

ROLLBACK;
