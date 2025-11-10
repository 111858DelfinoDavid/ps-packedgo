# 🚀 Script para iniciar todos los servicios de PackedGo

Write-Host "🎯 INICIANDO TODOS LOS SERVICIOS DE PACKEDGO" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Gray
Write-Host ""

# Verificar PostgreSQL primero
Write-Host "📦 Verificando PostgreSQL..." -ForegroundColor Yellow
$pgCheck = Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue -ErrorAction SilentlyContinue

if (-not $pgCheck.TcpTestSucceeded) {
    Write-Host "❌ PostgreSQL no está corriendo en puerto 5432" -ForegroundColor Red
    Write-Host "💡 Ejecuta: docker start postgres-packedgo" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ PostgreSQL está corriendo" -ForegroundColor Green
Write-Host ""

# Verificar que exista Node.js para el frontend
Write-Host "📦 Verificando Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>$null
    if ($nodeVersion) {
        Write-Host "✅ Node.js: $nodeVersion" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Node.js no encontrado - no se podrá iniciar el frontend" -ForegroundColor Yellow
}
Write-Host ""

# Verificar que exista Angular CLI
Write-Host "📦 Verificando Angular CLI..." -ForegroundColor Yellow
try {
    $ngVersion = ng version 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Angular CLI instalado" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Angular CLI no encontrado" -ForegroundColor Yellow
    Write-Host "💡 Instala con: npm install -g @angular/cli" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "🎯 INSTRUCCIONES PARA INICIAR SERVICIOS" -ForegroundColor Cyan
Write-Host "=" * 60 -ForegroundColor Gray
Write-Host ""

Write-Host "Debes abrir 6 terminales de PowerShell y ejecutar estos comandos:" -ForegroundColor White
Write-Host ""

Write-Host "🔹 TERMINAL 1 - Auth Service (Puerto 8081)" -ForegroundColor Cyan
Write-Host "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\auth-service" -ForegroundColor Gray
Write-Host ".\mvnw.cmd spring-boot:run" -ForegroundColor Green
Write-Host ""

Write-Host "🔹 TERMINAL 2 - Users Service (Puerto 8082)" -ForegroundColor Cyan
Write-Host "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\users-service" -ForegroundColor Gray
Write-Host ".\mvnw.cmd spring-boot:run" -ForegroundColor Green
Write-Host ""

Write-Host "🔹 TERMINAL 3 - Event Service (Puerto 8086)" -ForegroundColor Cyan
Write-Host "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\event-service" -ForegroundColor Gray
Write-Host ".\mvnw.cmd spring-boot:run" -ForegroundColor Green
Write-Host ""

Write-Host "🔹 TERMINAL 4 - Order Service (Puerto 8084)" -ForegroundColor Cyan
Write-Host "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\order-service" -ForegroundColor Gray
Write-Host ".\mvnw.cmd spring-boot:run" -ForegroundColor Green
Write-Host ""

Write-Host "🔹 TERMINAL 5 - Payment Service (Puerto 8085) ⭐ IMPORTANTE" -ForegroundColor Cyan
Write-Host "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\payment-service" -ForegroundColor Gray
Write-Host ".\mvnw.cmd spring-boot:run" -ForegroundColor Green
Write-Host ""

Write-Host "🔹 TERMINAL 6 - Angular Frontend (Puerto 4200)" -ForegroundColor Cyan
Write-Host "cd C:\Users\david\Documents\ps-packedgo\packedgo\front-angular" -ForegroundColor Gray
Write-Host "ng serve" -ForegroundColor Green
Write-Host ""

Write-Host "=" * 60 -ForegroundColor Gray
Write-Host ""
Write-Host "⏱️  Cada servicio tarda ~30-60 segundos en arrancar" -ForegroundColor Yellow
Write-Host "✅ Espera a ver el mensaje 'Started ...Application in X seconds'" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Para verificar el estado de todos los servicios:" -ForegroundColor Cyan
Write-Host ".\verify-services.ps1" -ForegroundColor Green
Write-Host ""

# Preguntar si quiere que abramos las terminales automáticamente
Write-Host "¿Quieres que abra las terminales automáticamente? (s/n): " -ForegroundColor Yellow -NoNewline
$respuesta = Read-Host

if ($respuesta -eq 's' -or $respuesta -eq 'S') {
    Write-Host ""
    Write-Host "🚀 Abriendo terminales..." -ForegroundColor Cyan
    
    # Abrir terminal para Auth Service
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\auth-service; Write-Host '🔹 AUTH SERVICE - Puerto 8081' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
    
    Start-Sleep -Seconds 2
    
    # Abrir terminal para Users Service
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\users-service; Write-Host '🔹 USERS SERVICE - Puerto 8082' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
    
    Start-Sleep -Seconds 2
    
    # Abrir terminal para Event Service
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\event-service; Write-Host '🔹 EVENT SERVICE - Puerto 8086' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
    
    Start-Sleep -Seconds 2
    
    # Abrir terminal para Order Service
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\order-service; Write-Host '🔹 ORDER SERVICE - Puerto 8084' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
    
    Start-Sleep -Seconds 2
    
    # Abrir terminal para Payment Service
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd C:\Users\david\Documents\ps-packedgo\packedgo\back\payment-service; Write-Host '🔹 PAYMENT SERVICE - Puerto 8085 ⭐' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"
    
    Start-Sleep -Seconds 2
    
    # Abrir terminal para Frontend
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", "cd C:\Users\david\Documents\ps-packedgo\packedgo\front-angular; Write-Host '🔹 ANGULAR FRONTEND - Puerto 4200' -ForegroundColor Cyan; ng serve"
    
    Write-Host ""
    Write-Host "✅ Terminales abiertas!" -ForegroundColor Green
    Write-Host "⏱️  Espera ~60 segundos y ejecuta: .\verify-services.ps1" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "👍 Abre las terminales manualmente con los comandos de arriba" -ForegroundColor Cyan
    Write-Host ""
}

Write-Host "🎯 DESPUÉS DE ARRANCAR TODO:" -ForegroundColor Cyan
Write-Host "1. Ejecuta .\verify-services.ps1 para verificar" -ForegroundColor White
Write-Host "2. Abre http://localhost:4200 en el navegador" -ForegroundColor White
Write-Host "3. Sigue las instrucciones de INICIAR_TODOS_LOS_SERVICIOS.md" -ForegroundColor White
Write-Host ""
