# Script para probar el flujo completo de Analytics
# 1. Login como ADMIN
# 2. Obtener JWT token
# 3. Consultar dashboard de analytics

Write-Host "=== PRUEBA COMPLETA DE ANALYTICS ===" -ForegroundColor Cyan
Write-Host ""

# Paso 1: Login
Write-Host "1. Realizando login como ADMIN..." -ForegroundColor Yellow
$loginBody = @{
    email = "admin@packedgo.com"
    password = "Admin123!"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json"
    
    $token = $loginResponse.access_token
    Write-Host "✓ Login exitoso" -ForegroundColor Green
    Write-Host "Token obtenido: $($token.Substring(0, 50))..." -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "✗ Error en login: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Paso 2: Consultar Analytics Dashboard
Write-Host "2. Consultando Analytics Dashboard..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    $dashboardResponse = Invoke-RestMethod -Uri "http://localhost:8087/api/api/dashboard" `
        -Method GET `
        -Headers $headers
    
    Write-Host "✓ Dashboard obtenido exitosamente" -ForegroundColor Green
    Write-Host ""
    Write-Host "=== MÉTRICAS DE VENTAS ===" -ForegroundColor Cyan
    Write-Host "Ingresos Totales: $($dashboardResponse.salesMetrics.totalRevenue) ARS"
    Write-Host "Tickets Vendidos: $($dashboardResponse.salesMetrics.totalTicketsSold)"
    Write-Host "Tasa de Ocupación: $($dashboardResponse.salesMetrics.averageOccupancyRate)%"
    Write-Host ""
    
    Write-Host "=== MÉTRICAS DE EVENTOS ===" -ForegroundColor Cyan
    Write-Host "Total de Eventos: $($dashboardResponse.eventMetrics.totalEvents)"
    Write-Host "Eventos Activos: $($dashboardResponse.eventMetrics.activeEvents)"
    Write-Host ""
    
    if ($dashboardResponse.topEvents -and $dashboardResponse.topEvents.Count -gt 0) {
        Write-Host "=== TOP 5 EVENTOS ===" -ForegroundColor Cyan
        $dashboardResponse.topEvents | ForEach-Object {
            Write-Host "- $($_.eventName): $($_.revenue) ARS ($($_.ticketsSold) tickets)"
        }
        Write-Host ""
    }
    
    if ($dashboardResponse.topConsumptions -and $dashboardResponse.topConsumptions.Count -gt 0) {
        Write-Host "=== TOP 5 CONSUMOS ===" -ForegroundColor Cyan
        $dashboardResponse.topConsumptions | ForEach-Object {
            Write-Host "- $($_.productName): $($_.totalRevenue) ARS ($($_.quantitySold) unidades)"
        }
        Write-Host ""
    }
    
    Write-Host "=== CRECIMIENTO MENSUAL ===" -ForegroundColor Cyan
    Write-Host "Crecimiento: $($dashboardResponse.monthlyGrowth.growthPercentage)%"
    Write-Host ""
    
} catch {
    Write-Host "✗ Error consultando dashboard: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit 1
}

Write-Host "=== PRUEBA COMPLETADA EXITOSAMENTE ===" -ForegroundColor Green
Write-Host ""
Write-Host "Ahora puedes probar el frontend:" -ForegroundColor Yellow
Write-Host "1. Abre http://localhost:3000/admin/login" -ForegroundColor White
Write-Host "2. Login con: admin@packedgo.com / Admin123!" -ForegroundColor White
Write-Host "3. Haz clic en el botón 'Analíticas' en el dashboard" -ForegroundColor White
Write-Host "4. Deberías ver el dashboard completo con todas las métricas" -ForegroundColor White
