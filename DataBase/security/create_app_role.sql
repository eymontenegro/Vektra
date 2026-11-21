-- =========================================================
-- VEKTRA - Usuario de base de datos para la aplicación
--
-- La aplicación no debe conectarse como postgres (superusuario): si
-- alguien lograra ejecutar SQL a través de ella, podría borrar la base.
-- Este script crea el usuario vektra_app con lo mínimo que necesita:
-- leer y escribir datos, sin poder crear, alterar ni borrar tablas.
--
-- Cómo usarlo:
--   1. Cambie CAMBIAR_ESTA_CLAVE por una clave real ANTES de ejecutarlo.
--      No suba el archivo con la clave real a GitHub.
--   2. Ejecútelo conectado a la base Vektra con un superusuario.
--   3. En config.properties ponga db.user=vektra_app y la clave elegida.
-- Requiere haber ejecutado vektra.sql (o V2 y V3 si la base ya existía).
-- =========================================================

BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'vektra_app') THEN
        CREATE ROLE vektra_app LOGIN PASSWORD 'CAMBIAR_ESTA_CLAVE';
    END IF;
END $$;

GRANT CONNECT ON DATABASE "Vektra" TO vektra_app;
GRANT USAGE ON SCHEMA public TO vektra_app;

-- Datos que la aplicación lee y modifica.
GRANT SELECT, INSERT, UPDATE, DELETE ON users, drivers, vehicles, tickets TO vektra_app;
-- Estaciones y rutas solo se leen desde la aplicación (se cargan por SQL).
GRANT SELECT ON stations, routes, route_stations TO vektra_app;
-- Vistas del dashboard y los reportes.
GRANT SELECT ON v_route_summary, v_driver_assignments,
                v_ticket_sales_by_origin, v_dashboard_totals TO vektra_app;
-- Las columnas id son IDENTITY: hace falta poder usar sus secuencias.
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO vektra_app;

COMMIT;
