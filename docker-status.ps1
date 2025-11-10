# 🐳 Script para verificar el estado de Docker Compose

param(
    [switch]$ShowLogs = $false,
    [string]$Service = ""
)

$backPath = "C:\Users\david\Documents\ps-packedgo\packedgo\back"

Write-Host "`n🐳 ESTADO DE LOS SERVICIOS DOCKER COMPOSE" -ForegroundColor Cyan
Write-Host ("=" * 70) -ForegroundColor Gray
Write-Host ""

# Cambiar al directorio correcto
Set-Location $backPath

# Mostrar contenedores
Write-Host "📦 Contenedores en ejecución:" -ForegroundColor Yellow
Write-Host ""
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | ForEach-Object {
    if ($_ -match "healthy") {
        Write-Host $_ -ForegroundColor Green
    } elseif ($_ -match "starting|unhealthy") {
        Write-Host $_ -ForegroundColor Yellow
    } elseif ($_ -match "NAMES|STATUS") {
        Write-Host $_ -ForegroundColor Cyan
    } else {
        Write-Host $_ -ForegroundColor White
    }
}

Write-Host ""
Write-Host ("=" * 70) -ForegroundColor Gray
Write-Host ""

# Contar servicios
$totalContainers = (docker ps -q | Measure-Object).Count
$serviceContainers = (docker ps --filter "name=service" -q | Measure-Object).Count
$dbContainers = (docker ps --filter "name=db" -q | Measure-Object).Count

Write-Host "📊 Resumen:" -ForegroundColor Cyan
Write-Host "   Total contenedores: $totalContainers" -ForegroundColor White
Write-Host "   Servicios backend: $serviceContainers / 6" -ForegroundColor $(if ($serviceContainers -eq 6) { "Green" } else { "Yellow" })
Write-Host "   Bases de datos: $dbContainers / 5" -ForegroundColor $(if ($dbContainers -eq 5) { "Green" } else { "Yellow" })
Write-Host ""

# Verificar puertos
Write-Host "🔌 Verificando puertos:" -ForegroundColor Cyan
$ports = @{
    "Auth Service" = 8081
    "Users Service" = 8082
    "Order Service" = 8084
    "Payment Service" = 8085
    "Event Service" = 8086
    "Consumption Service" = 8088
}

foreach ($service in $ports.Keys) {
    $port = $ports[$service]
    $check = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue -ErrorAction SilentlyContinue -InformationLevel Quiet
    
    if ($check) {
        Write-Host "   ✅ $service (puerto $port)" -ForegroundColor Green
    } else {
        Write-Host "   ❌ $service (puerto $port)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host ("=" * 70) -ForegroundColor Gray

# Mostrar logs si se solicita
if ($ShowLogs) {
    Write-Host ""
    if ($Service) {
        Write-Host "📋 Logs de $Service (últimas 50 líneas):" -ForegroundColor Yellow
        Write-Host ""
        docker compose logs --tail=50 $Service
    } else {
        Write-Host "📋 Logs de todos los servicios (últimas 20 líneas cada uno):" -ForegroundColor Yellow
        Write-Host ""
        docker compose logs --tail=20
    }
}

Write-Host ""
Write-Host "💡 Comandos útiles:" -ForegroundColor Cyan
Write-Host "   Ver logs de un servicio: .\docker-status.ps1 -ShowLogs -Service payment-service" -ForegroundColor Gray
Write-Host "   Ver todos los logs: docker compose logs -f" -ForegroundColor Gray
Write-Host "   Detener todo: docker compose down" -ForegroundColor Gray
Write-Host "   Reiniciar un servicio: docker compose restart payment-service" -ForegroundColor Gray
Write-Host ""
