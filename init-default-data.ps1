# 🎯 Script para inicializar datos por defecto en la base de datos

Write-Host "`n🎯 INICIALIZANDO DATOS POR DEFECTO" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""

# Verificar que los contenedores estén corriendo
Write-Host "📦 Verificando contenedores..." -ForegroundColor Yellow

$eventDbRunning = docker ps | Select-String "back-event-db-1"
if (-not $eventDbRunning) {
    Write-Host "❌ Event DB no está corriendo" -ForegroundColor Red
    Write-Host "💡 Ejecuta: docker compose up -d" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Event DB está corriendo" -ForegroundColor Green
Write-Host ""

# Crear categorías de eventos
Write-Host "📝 Creando categorías de eventos..." -ForegroundColor Cyan

$checkCategories = docker exec back-event-db-1 psql -U event_user -d event_db -t -c "SELECT COUNT(*) FROM event_categories;"
$categoryCount = [int]$checkCategories.Trim()

if ($categoryCount -eq 0) {
    Write-Host "   Insertando 6 categorías..." -ForegroundColor White
    
    docker exec back-event-db-1 psql -U event_user -d event_db -c @"
INSERT INTO event_categories (name, active, created_by) VALUES 
    ('Música', true, 1),
    ('Deportes', true, 1),
    ('Teatro', true, 1),
    ('Conferencias', true, 1),
    ('Festivales', true, 1),
    ('Otros', true, 1);
"@
    
    Write-Host "   ✅ Categorías creadas" -ForegroundColor Green
} else {
    Write-Host "   ✅ Ya existen $categoryCount categorías" -ForegroundColor Green
}

Write-Host ""

# Mostrar categorías
Write-Host "📋 Categorías disponibles:" -ForegroundColor Cyan
docker exec back-event-db-1 psql -U event_user -d event_db -c "SELECT id, name, active FROM event_categories ORDER BY id;"

Write-Host ""

# Crear categorías de consumiciones
Write-Host "📝 Creando categorías de consumiciones..." -ForegroundColor Cyan

$checkConsumptionCategories = docker exec back-event-db-1 psql -U event_user -d event_db -t -c "SELECT COUNT(*) FROM consumption_categories;"
$consumptionCount = [int]$checkConsumptionCategories.Trim()

if ($consumptionCount -eq 0) {
    Write-Host "   Insertando 5 categorías de consumición..." -ForegroundColor White
    
    docker exec back-event-db-1 psql -U event_user -d event_db -c @"
INSERT INTO consumption_categories (name, active, created_by) VALUES 
    ('Bebidas', true, 1),
    ('Comida', true, 1),
    ('Snacks', true, 1),
    ('Bebidas Alcohólicas', true, 1),
    ('Otros', true, 1);
"@
    
    Write-Host "   ✅ Categorías de consumición creadas" -ForegroundColor Green
} else {
    Write-Host "   ✅ Ya existen $consumptionCount categorías de consumición" -ForegroundColor Green
}

Write-Host ""

# Mostrar categorías de consumición
Write-Host "📋 Categorías de consumición disponibles:" -ForegroundColor Cyan
docker exec back-event-db-1 psql -U event_user -d event_db -c "SELECT id, name, active FROM consumption_categories ORDER BY id;"

Write-Host ""
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""
Write-Host "✅ Inicialización completada" -ForegroundColor Green
Write-Host ""
Write-Host "🔍 Endpoints disponibles:" -ForegroundColor Cyan
Write-Host "   Categorías de eventos (activas): http://localhost:8086/api/event-service/category/active" -ForegroundColor White
Write-Host "   Categorías de eventos (todas): http://localhost:8086/api/event-service/category" -ForegroundColor White
Write-Host "   Categorías de consumición: http://localhost:8086/api/event-service/consumption-category/active" -ForegroundColor White
Write-Host ""
