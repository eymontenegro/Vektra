# Vektra
Sistema de gestión de un metro urbano en Java: tickets, rutas, conductores y
vehículos, con mapa interactivo. Java 25, Maven, Swing (JavaFX solo en el mapa),
PostgreSQL y JavaMail.

## Requisitos
- JDK 25 (la misma versión en todo el equipo).
- PostgreSQL con la base `Vektra` creada.

## Configuración
1. Copia `config.properties.example` a `config.properties` (en la raíz, junto a
   pom.xml). Ese archivo está en .gitignore: jamás se sube.
2. Llena `db.url`, `db.user`, `db.password`, `mail.sender` y `mail.password`
   (Gmail: contraseña de aplicación, no la normal).

## Base de datos
Ejecuta `DataBase/vektra.sql` en PostgreSQL. Recrea todas las tablas con datos
de prueba de Valledupar y la cuenta del administrador.

## Ejecutar
mvn exec:java
El mapa necesita internet (Leaflet y OpenStreetMap).
