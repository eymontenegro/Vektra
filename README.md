# Vektra
Sistema de gestión de un metro urbano (datos de prueba de Valledupar): mapa de
estaciones y rutas, compra de tickets con envío por correo, gestión de
conductores y vehículos, y dashboard para el administrador.
Aplicación web en Java 25 con arquitectura MVC por capas:
**Javalin** (servidor HTTP con Jetty embebido) + **Thymeleaf** (vistas HTML) +
**JDBC/PostgreSQL** (DAO) + **JavaMail** (correo) + **Leaflet** (mapa).
## Requisitos- JDK 25 (la misma versión en todo el equipo).- Maven 3.9 o superior.- PostgreSQL 14 o superior, con la base `Vektra` creada.- Internet en el navegador para el mapa (Leaflet y OpenStreetMap).
## Configuración
1. Copie `config.properties.example` a `config.properties` en la raíz del
proyecto (junto a `pom.xml`). Ese archivo está en `.gitignore`: jamás se sube.
2. Llene los datos. Cada uno también se puede dar como variable de entorno
(`db.url` → `DB_URL`, `mail.password` → `MAIL_PASSWORD`...); la variable
de entorno tiene prioridad sobre el archivo.
| Dato            
| Para qué sirve                                              
|
|-----------------|-------------------------------------------------------------|
| `db.url`        
| `jdbc:postgresql://localhost:5432/Vektra`                   
| `db.user`       
|
| Usuario de la base (recomendado: `vektra_app`, ver abajo)   |
| `db.password`   | Clave de ese usuario                                        
|
| `mail.sender`   | Correo Gmail que envía los tickets (opcional)               
| `mail.password` | Contraseña de aplicación de Gmail (opcional)                
|
|
| `server.port`   | Puerto web, por defecto `7070`                              
| `server.dev`    
| `true` para ver cambios del HTML sin reiniciar              
Si no hay datos de correo, la compra funciona igual y avisa que no se envió.
## Base de datos
Base nueva (borra y recrea todo, con datos de prueba):
```bash
psql -U postgres -d Vektra -v ON_ERROR_STOP=1 -f DataBase/vektra.sql
```
Base que ya tiene la versión 1 y datos que se quieren conservar:
|
|
```bash
pg_dump -U postgres -d Vektra -F c -f respaldo_vektra.dump
psql -U postgres -d Vektra -v ON_ERROR_STOP=1 -f DataBase/migrations/V2__integrity_indexes_views.sql
```
Usuario de la aplicación con permisos mínimos (cambie la clave dentro del
script antes de ejecutarlo):
```bash
psql -U postgres -d Vektra -v ON_ERROR_STOP=1 -f DataBase/security/create_app_role.sql
```
Pruebas de integridad (no dejan cambios):
```bash
psql -U postgres -d Vektra -v ON_ERROR_STOP=1 -f DataBase/tests/integrity_tests.sql
```
La cuenta del administrador es `admin@vektra.com`; la contraseña la comparte el
líder por otro medio.
## Ejecutar
```bash
mvn clean compile exec:java
```
Abra http://localhost:7070 en el navegador.
## Pruebas
```bash
mvn test                    
# pruebas unitarias (no necesitan base de datos)
mvn test -Pdatabase-tests   # pruebas de integración contra PostgreSQL
```
## Estructura
```
src/main/java/com/vektra/
Main.java            
arma las dependencias y arranca el servidor
config/              
connection/          
exception/           
model/               
dao/                 
service/             
security/            
AppConfig: lectura única de la configuración
DatabaseConnection
excepciones del negocio (mensajes en español)
entidades y enums (Route, Station, Driver, LineColor...)
acceso a datos con PreparedStatement
reglas de negocio y permisos
SessionUser y AccessControl
util/                
web/                 
web/controller/      
src/main/resources/
templates/           
static/              
DataBase/              
```
validaciones y formato de textos
servidor, sesión, CSRF, plantillas
un controlador por parte de la aplicación
vistas HTML (Thymeleaf)
CSS, JavaScript, imágenes y fuentes
scripts SQL, migraciones, pruebas y reportes
## Flujo de trabajo con Git- `main`: versión estable y entregable. Nadie trabaja ni hace push directo aquí.- `develop`: integración. Todos los Pull Request van a `develop`.- `feature/*`: una rama por tarea, creada desde `develop`.
Commits y PR en inglés con prefijo (`feat:`, `fix:`, `refactor:`, `docs:`,
`test:`, `chore:`). Mensajes de pantalla y comentarios del código en español.
La versión de escritorio anterior (Swing) quedó guardada en la etiqueta
`v1.0-swing`.