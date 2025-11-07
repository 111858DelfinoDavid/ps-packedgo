# 🎯 Script para crear usuario ADMIN y datos de prueba

Write-Host "`n🎯 INICIALIZANDO USUARIO ADMIN Y DATOS DE PRUEBA" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""

# Paso 1: Registrar usuario ADMIN
Write-Host "👤 Paso 1: Registrando usuario ADMIN..." -ForegroundColor Yellow

$registerBody = @{
    first_name = "Admin"
    last_name = "PackedGo"
    email = "admin@packedgo.com"
    password = "Admin123!"
    role = "ADMIN"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" `
        -Method POST `
        -Body $registerBody `
        -ContentType "application/json"
    
    Write-Host "✅ Usuario ADMIN registrado exitosamente" -ForegroundColor Green
    Write-Host "   Email: admin@packedgo.com" -ForegroundColor White
    Write-Host "   Password: Admin123!" -ForegroundColor White
    Write-Host "   Role: ADMIN" -ForegroundColor White
    Write-Host ""
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "ℹ️  Usuario ADMIN ya existe" -ForegroundColor Cyan
        Write-Host "   Email: admin@packedgo.com" -ForegroundColor White
        Write-Host "   Password: Admin123!" -ForegroundColor White
        Write-Host ""
    } else {
        Write-Host "❌ Error registrando usuario: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "   ¿Está corriendo el auth-service?" -ForegroundColor Yellow
        Write-Host "   Ejecuta: docker ps | Select-String 'auth-service'" -ForegroundColor Yellow
        exit 1
    }
}

# Paso 2: Obtener token
Write-Host "🔑 Paso 2: Obteniendo token de autenticación..." -ForegroundColor Yellow

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
    Write-Host "✅ Token obtenido exitosamente" -ForegroundColor Green
    Write-Host "   Token: $($token.Substring(0, 50))..." -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "❌ Error obteniendo token: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Paso 3: Verificar acceso a Analytics
Write-Host "📊 Paso 3: Verificando acceso al Analytics Dashboard..." -ForegroundColor Yellow

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    $dashboardResponse = Invoke-RestMethod -Uri "http://localhost:8087/api/api/dashboard" `
        -Method GET `
        -Headers $headers
    
    Write-Host "✅ Analytics Dashboard accesible" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Datos actuales del dashboard:" -ForegroundColor Cyan
    Write-Host "   Ingresos Totales: $($dashboardResponse.salesMetrics.totalRevenue) ARS" -ForegroundColor White
    Write-Host "   Tickets Vendidos: $($dashboardResponse.salesMetrics.totalTicketsSold)" -ForegroundColor White
    Write-Host "   Total de Eventos: $($dashboardResponse.eventMetrics.totalEvents)" -ForegroundColor White
    Write-Host "   Eventos Activos: $($dashboardResponse.eventMetrics.activeEvents)" -ForegroundColor White
    Write-Host ""
} catch {
    Write-Host "❌ Error accediendo a Analytics: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "   Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    Write-Host "   ¿Está corriendo el analytics-service?" -ForegroundColor Yellow
    Write-Host "   Ejecuta: docker ps | Select-String 'analytics-service'" -ForegroundColor Yellow
    Write-Host ""
}

# Paso 4: Ejecutar init-default-data.ps1 para categorías
Write-Host "📝 Paso 4: Inicializando categorías..." -ForegroundColor Yellow
if (Test-Path ".\init-default-data.ps1") {
    .\init-default-data.ps1
} else {
    Write-Host "⚠️  Script init-default-data.ps1 no encontrado" -ForegroundColor Yellow
    Write-Host ""
}

# Resumen final
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""
Write-Host "✅ INICIALIZACIÓN COMPLETADA" -ForegroundColor Green
Write-Host ""
Write-Host "🎉 Ahora puedes probar el sistema completo:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1️⃣  Abre tu navegador en: http://localhost:3000/admin/login" -ForegroundColor White
Write-Host ""
Write-Host "2️⃣  Inicia sesión con:" -ForegroundColor White
Write-Host "    📧 Email: admin@packedgo.com" -ForegroundColor Yellow
Write-Host "    🔒 Password: Admin123!" -ForegroundColor Yellow
Write-Host ""
Write-Host "3️⃣  En el Dashboard de Admin, haz clic en el botón 'Analíticas'" -ForegroundColor White
Write-Host ""
Write-Host "4️⃣  Deberías ver el Dashboard completo con:" -ForegroundColor White
Write-Host "    ✔ 4 KPI Cards (Ingresos, Tickets, Eventos, Ocupación)" -ForegroundColor Gray
Write-Host "    ✔ Desglose de Ingresos" -ForegroundColor Gray
Write-Host "    ✔ Crecimiento Mensual" -ForegroundColor Gray
Write-Host "    ✔ Top 5 Eventos" -ForegroundColor Gray
Write-Host "    ✔ Top 5 Consumos" -ForegroundColor Gray
Write-Host "    ✔ Tendencias Diarias" -ForegroundColor Gray
Write-Host ""
Write-Host "📋 Estado actual: Sin datos históricos (valores en 0)" -ForegroundColor Cyan
Write-Host "💡 Para ver métricas reales, crea eventos y procesa pagos" -ForegroundColor Yellow
Write-Host ""
