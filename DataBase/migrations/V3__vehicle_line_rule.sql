-- VEKTRA - Migración V3: la línea del vehículo debe ser la línea de la
-- ruta de su conductor.
--
-- Regla decidida por el equipo. En una base nueva no hace falta: ya viene
-- dentro de DataBase/vektra.sql. Ejecútela solo en bases que ya existían,
-- DESPUÉS de V2.
--
-- Por qué importa en Bases de Datos: en vehicles existe la dependencia
-- transitiva driver_id -> drivers.route_id -> routes.line_color. La
-- columna vehicles.line repite un dato que ya se puede obtener y podría
-- contradecirlo (anomalía de actualización). Estos triggers conservan la
-- columna (no rompen el código) e impiden que se contradiga.
--
-- Si ya hay vehículos que no cumplen la regla, la migración se detiene sin
-- cambiar nada y dice cuántos son.
-- =========================================================

BEGIN;

DO $$
DECLARE
    bad_rows INTEGER;
BEGIN
    SELECT COUNT(*) INTO bad_rows
    FROM vehicles v
    JOIN drivers dr ON dr.id = v.driver_id
    JOIN routes r ON r.id = dr.route_id
    WHERE v.line <> r.line_color;
    IF bad_rows > 0 THEN
        RAISE EXCEPTION 'Hay % vehículos cuya línea no coincide con la ruta de su conductor. Corríjalos antes de migrar.', bad_rows;
    END IF;
END $$;

-- Al guardar un vehículo, su línea debe ser la de la ruta del conductor.
CREATE OR REPLACE FUNCTION fn_check_vehicle_line()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    driver_line VARCHAR(20);
BEGIN
    SELECT r.line_color INTO driver_line
    FROM drivers dr
    JOIN routes r ON r.id = dr.route_id
    WHERE dr.id = NEW.driver_id;

    IF driver_line IS NOT NULL AND NEW.line <> driver_line THEN
        RAISE EXCEPTION 'El vehículo % debe circular en la línea % de su conductor.', NEW.plate, driver_line
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_vehicles_check_line ON vehicles;
CREATE TRIGGER trg_vehicles_check_line
    BEFORE INSERT OR UPDATE OF line, driver_id ON vehicles
    FOR EACH ROW EXECUTE FUNCTION fn_check_vehicle_line();

-- Al cambiar la ruta de un conductor, sus vehículos deben seguir en la misma línea.
CREATE OR REPLACE FUNCTION fn_check_driver_route_line()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (SELECT 1
               FROM vehicles v
               JOIN routes r ON r.id = NEW.route_id
               WHERE v.driver_id = NEW.id AND v.line <> r.line_color) THEN
        RAISE EXCEPTION 'El conductor % tiene vehículos en otra línea; reasígnelos primero.', NEW.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_drivers_check_route_line ON drivers;
CREATE TRIGGER trg_drivers_check_route_line
    BEFORE UPDATE OF route_id ON drivers
    FOR EACH ROW EXECUTE FUNCTION fn_check_driver_route_line();

COMMIT;
