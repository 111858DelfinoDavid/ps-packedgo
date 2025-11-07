# ========================================
# 🚀 SCRIPT DE INICIO COMPLETO - PACKEDGO
# ========================================
# Inicia TODOS los microservicios incluyendo Analytics-Service

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  INICIANDO SISTEMA PACKEDGO COMPLETO  " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Name="Auth-Service"; Port=8081; Path="packedgo\back\auth-service"},
    @{Name="Users-Service"; Port=8082; Path="packedgo\back\users-service"},
    @{Name="Event-Service"; Port=8086; Path="packedgo\back\event-service"},
    @{Name="Order-Service"; Port=8084; Path="packedgo\back\order-service"},
    @{Name="Payment-Service"; Port=8085; Path="packedgo\back\payment-service"},
    @{Name="Analytics-Service"; Port=8087; Path="packedgo\back\analytics-service"}
)

# Verificar PostgreSQL
Write-Host "🔍 Verificando PostgreSQL..." -ForegroundColor Yellow
try {
    $pgService = Get-Service postgresql* -ErrorAction SilentlyContinue
    if ($pgService) {
        if ($pgService.Status -eq 'Running') {
            Write-Host "✅ PostgreSQL está corriendo" -ForegroundColor Green
        } else {
            Write-Host "⚠️  PostgreSQL está detenido. Intentando iniciar..." -ForegroundColor Yellow
            Start-Service $pgService.Name
            Start-Sleep -Seconds 3
            Write-Host "✅ PostgreSQL iniciado" -ForegroundColor Green
        }
    } else {
        Write-Host "❌ PostgreSQL no está instalado o no se encuentra" -ForegroundColor Red
        Write-Host "   Instala PostgreSQL o usa Docker: docker run --name postgres-packedgo -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:15-alpine" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "❌ Error verificando PostgreSQL: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📊 Creando bases de datos necesarias..." -ForegroundColor Yellow
$dbScript = @"
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS users_db;
CREATE DATABASE IF NOT EXISTS event_db;
CREATE DATABASE IF NOT EXISTS order_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS analytics_db;
"@

# Nota: Este script asume que las bases de datos ya existen
# Si no existen, ejecutar manualmente:
# psql -U postgres -h localhost -c "CREATE DATABASE analytics_db;"

Write-Host ""
Write-Host "🚀 Iniciando microservicios..." -ForegroundColor Cyan
Write-Host "   Se abrirán $($services.Count) ventanas de PowerShell" -ForegroundColor Gray
Write-Host ""

foreach ($service in $services) {
    Write-Host "🔹 Iniciando $($service.Name) en puerto $($service.Port)..." -ForegroundColor Cyan
    
    $servicePath = Join-Path $PSScriptRoot $service.Path
    
    if (Test-Path $servicePath) {
        # Abrir nueva ventana de PowerShell para cada servicio
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$servicePath'; Write-Host '🚀 $($service.Name)' -ForegroundColor Green; .\mvnw spring-boot:run"
        Start-Sleep -Seconds 2
        Write-Host "   ✅ Terminal abierto para $($service.Name)" -ForegroundColor Green
    } else {
        Write-Host "   ❌ No se encontró el directorio: $servicePath" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "⏳ Esperando 30 segundos para que los servicios inicien..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "🔍 Verificando servicios..." -ForegroundColor Cyan
Write-Host ""

foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$($service.Port)/api/health" -Method GET -TimeoutSec 5 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "✅ $($service.Name) - ACTIVO (puerto $($service.Port))" -ForegroundColor Green
        } else {
            Write-Host "⚠️  $($service.Name) - Respuesta inesperada (puerto $($service.Port))" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "❌ $($service.Name) - NO RESPONDE (puerto $($service.Port))" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SERVICIOS INICIADOS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📍 URLs de los servicios:" -ForegroundColor Yellow
Write-Host "   • Auth Service:      http://localhost:8081" -ForegroundColor Gray
Write-Host "   • Users Service:     http://localhost:8082" -ForegroundColor Gray
Write-Host "   • Event Service:     http://localhost:8086" -ForegroundColor Gray
Write-Host "   • Order Service:     http://localhost:8084" -ForegroundColor Gray
Write-Host "   • Payment Service:   http://localhost:8085" -ForegroundColor Gray
Write-Host "   • Analytics Service: http://localhost:8087" -ForegroundColor Gray
Write-Host ""
Write-Host "📊 Dashboard de Analytics:" -ForegroundColor Yellow
Write-Host "   • Endpoint: GET http://localhost:8087/api/dashboard" -ForegroundColor Gray
Write-Host "   • Requiere: Bearer Token de un usuario ADMIN" -ForegroundColor Gray
Write-Host "   • Health: GET http://localhost:8087/api/dashboard/health" -ForegroundColor Gray
Write-Host ""
Write-Host "🎨 Para iniciar el frontend:" -ForegroundColor Yellow
Write-Host "   cd packedgo\front-angular" -ForegroundColor Gray
Write-Host "   ng serve" -ForegroundColor Gray
Write-Host "   http://localhost:4200" -ForegroundColor Gray
Write-Host ""
Write-Host "💡 Para detener todos los servicios:" -ForegroundColor Yellow
Write-Host "   Cierra todas las ventanas de PowerShell que se abrieron" -ForegroundColor Gray
Write-Host ""
Write-Host "✨ Sistema listo para usar! ✨" -ForegroundColor Green
Write-Host ""

# Preguntar si desea iniciar el frontend
$startFrontend = Read-Host "¿Deseas iniciar el frontend Angular ahora? (s/n)"
if ($startFrontend -eq "s" -or $startFrontend -eq "S") {
    $frontendPath = Join-Path $PSScriptRoot "packedgo\front-angular"
    if (Test-Path $frontendPath) {
        Write-Host ""
        Write-Host "🎨 Iniciando frontend Angular..." -ForegroundColor Cyan
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; Write-Host '🎨 Frontend Angular' -ForegroundColor Magenta; ng serve"
        Start-Sleep -Seconds 3
        Write-Host "✅ Frontend iniciado en http://localhost:4200" -ForegroundColor Green
    } else {
        Write-Host "❌ No se encontró el directorio del frontend: $frontendPath" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Presiona cualquier tecla para cerrar esta ventana..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
