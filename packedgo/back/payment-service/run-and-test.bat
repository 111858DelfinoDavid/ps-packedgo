@echo off
echo Iniciando Payment Service...
cd /d "%~dp0"
start "Payment Service" cmd /k "mvn spring-boot:run"
echo.
echo Esperando 30 segundos para que la aplicacion inicie...
timeout /t 30 /nobreak
echo.
echo Ejecutando pruebas...
powershell -ExecutionPolicy Bypass -File test-api.ps1
pause
