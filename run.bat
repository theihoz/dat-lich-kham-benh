@echo off
rem DatLichKhamBenh - do not terminate other projects occupying a port.
if not defined SERVER_PORT set SERVER_PORT=18080
echo [INFO] Starting clinic on port %SERVER_PORT%
cd /d "%~dp0backend"
mvn spring-boot:run -Dspring-boot.run.profiles=dev
