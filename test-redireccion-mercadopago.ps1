# Script de Prueba Rápida - Redirección MercadoPago
# Ejecutar desde: C:\Users\david\Documents\ps-packedgo

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   PRUEBA DE REDIRECCIÓN MERCADOPAGO   " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Verificar servicios
Write-Host "1️⃣  Verificando servicios Docker..." -ForegroundColor Yellow
Write-Host ""

$services = docker compose -f packedgo/back/docker-compose.yml ps --format "table {{.Name}}\t{{.Status}}" 2>$null
Write-Host $services

Write-Host ""
Write-Host "2️⃣  Verificando Payment Service..." -ForegroundColor Yellow

$paymentHealth = docker compose -f packedgo/back/docker-compose.yml ps payment-service --format "{{.Status}}"
if ($paymentHealth -like "*Up*" -or $paymentHealth -like "*healthy*") {
    Write-Host "✅ Payment Service: RUNNING" -ForegroundColor Green
} else {
    Write-Host "❌ Payment Service: NOT RUNNING" -ForegroundColor Red
    Write-Host "Ejecuta: docker compose -f packedgo/back/docker-compose.yml up -d payment-service"
    exit 1
}

Write-Host ""
Write-Host "3️⃣  Verificando Frontend Angular..." -ForegroundColor Yellow

$nodeProcess = Get-Process node -ErrorAction SilentlyContinue | Select-Object -First 1
if ($nodeProcess) {
    Write-Host "✅ Angular Dev Server: RUNNING (PID: $($nodeProcess.Id))" -ForegroundColor Green
} else {
    Write-Host "⚠️  Angular Dev Server: NO DETECTADO" -ForegroundColor Yellow
    Write-Host "Si no está corriendo, ejecuta:" -ForegroundColor Yellow
    Write-Host "cd packedgo/front-angular && npm start" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   INSTRUCCIONES DE PRUEBA              " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Abrir navegador en MODO INCÓGNITO" -ForegroundColor White
Write-Host "   Presiona: Ctrl + Shift + N (Chrome) o Ctrl + Shift + P (Firefox)" -ForegroundColor Gray
Write-Host ""

Write-Host "2. Navegar a:" -ForegroundColor White
Write-Host "   http://localhost:4200/customer/dashboard" -ForegroundColor Cyan
Write-Host ""

Write-Host "3. Iniciar sesión con un usuario customer" -ForegroundColor White
Write-Host ""

Write-Host "4. Agregar un evento al carrito y hacer checkout" -ForegroundColor White
Write-Host ""

Write-Host "5. Hacer clic en 'Pagar con MercadoPago'" -ForegroundColor White
Write-Host "   → El sistema iniciará polling automático en segundo plano 🔄" -ForegroundColor Gray
Write-Host ""

Write-Host "6. En MercadoPago, usar estos datos:" -ForegroundColor White
Write-Host "   Número:      5031 7557 3453 0604" -ForegroundColor Cyan
Write-Host "   CVV:         123" -ForegroundColor Cyan
Write-Host "   Vencimiento: 11/25" -ForegroundColor Cyan
Write-Host "   Nombre:      APRO" -ForegroundColor Cyan
Write-Host ""

Write-Host "7. Completar el pago y ESPERAR 2-5 segundos" -ForegroundColor White
Write-Host ""

Write-Host "✨ RESULTADO ESPERADO:" -ForegroundColor Green
Write-Host "   • Redirección automática O detección por polling" -ForegroundColor Green
Write-Host "   • Mensaje: '✅ ¡Pago aprobado! Tu orden ha sido confirmada.'" -ForegroundColor Green
Write-Host "   • Tickets con códigos QR visibles" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   MONITOREO (OPCIONAL)                 " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Para ver logs del Payment Service:" -ForegroundColor Yellow
Write-Host "docker compose -f packedgo/back/docker-compose.yml logs payment-service -f --tail=30" -ForegroundColor Cyan
Write-Host ""

Write-Host "Para ver logs del polling (en consola del navegador F12):" -ForegroundColor Yellow
Write-Host "Buscar mensajes que empiecen con 🔄 🔍 ✅" -ForegroundColor Cyan
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   ¡TODO LISTO! 🚀                      " -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Opción para abrir el navegador automáticamente
$openBrowser = Read-Host "¿Deseas abrir el navegador ahora? (S/N)"
if ($openBrowser -eq "S" -or $openBrowser -eq "s") {
    Write-Host ""
    Write-Host "Abriendo navegador en modo incógnito..." -ForegroundColor Green
    
    # Detectar navegador predeterminado y abrir en modo incógnito
    try {
        # Intentar con Chrome
        Start-Process "chrome.exe" -ArgumentList "--incognito", "http://localhost:4200/customer/dashboard"
        Write-Host "✅ Chrome abierto en modo incógnito" -ForegroundColor Green
    } catch {
        try {
            # Intentar con Edge
            Start-Process "msedge.exe" -ArgumentList "--inprivate", "http://localhost:4200/customer/dashboard"
            Write-Host "✅ Edge abierto en modo incógnito" -ForegroundColor Green
        } catch {
            Write-Host "⚠️  No se pudo abrir automáticamente. Abre manualmente:" -ForegroundColor Yellow
            Write-Host "   http://localhost:4200/customer/dashboard" -ForegroundColor Cyan
        }
    }
}

Write-Host ""
Write-Host "📖 Documentación completa en:" -ForegroundColor White
Write-Host "   • README_REDIRECCION_MERCADOPAGO.md (resumen)" -ForegroundColor Cyan
Write-Host "   • SOLUCION_REDIRECCION_MERCADOPAGO.md (detallado)" -ForegroundColor Cyan
Write-Host ""
