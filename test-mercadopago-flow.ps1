# Script para probar el flujo completo de pago con MercadoPago
# Este script verifica que todos los servicios estén funcionando correctamente

Write-Host "🚀 INICIANDO PRUEBA DEL FLUJO DE PAGO CON MERCADOPAGO" -ForegroundColor Cyan
Write-Host "====================================================`n" -ForegroundColor Cyan

# Colores para mensajes
$successColor = "Green"
$errorColor = "Red"
$warningColor = "Yellow"
$infoColor = "Cyan"

# URLs de los servicios
$authService = "http://localhost:8081/api"
$usersService = "http://localhost:8082/api"
$orderService = "http://localhost:8084/api"
$paymentService = "http://localhost:8085/api"
$eventService = "http://localhost:8086/api"

Write-Host "📋 PASO 1: Verificando servicios..." -ForegroundColor $infoColor

# Función para verificar servicio
function Test-Service {
    param (
        [string]$Name,
        [string]$Url
    )
    
    try {
        $response = Invoke-WebRequest -Uri "$Url/health" -Method GET -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✅ $Name - OK" -ForegroundColor $successColor
            return $true
        }
    }
    catch {
        Write-Host "  ❌ $Name - NO DISPONIBLE" -ForegroundColor $errorColor
        Write-Host "     URL: $Url/health" -ForegroundColor $warningColor
        return $false
    }
}

# Verificar servicios
$services = @(
    @{Name="Auth Service"; Url=$authService},
    @{Name="Users Service"; Url=$usersService},
    @{Name="Order Service"; Url=$orderService},
    @{Name="Payment Service"; Url=$paymentService},
    @{Name="Event Service"; Url=$eventService}
)

$allServicesUp = $true
foreach ($service in $services) {
    $result = Test-Service -Name $service.Name -Url $service.Url
    if (-not $result) {
        $allServicesUp = $false
    }
    Start-Sleep -Milliseconds 500
}

Write-Host ""

if (-not $allServicesUp) {
    Write-Host "⚠️  ADVERTENCIA: Algunos servicios no están disponibles" -ForegroundColor $warningColor
    Write-Host "   Por favor, inicia todos los servicios antes de continuar`n" -ForegroundColor $warningColor
    Write-Host "   Comandos para iniciar cada servicio:" -ForegroundColor $infoColor
    Write-Host "   cd packedgo\back\auth-service && .\mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "   cd packedgo\back\users-service && .\mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "   cd packedgo\back\order-service && .\mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "   cd packedgo\back\payment-service && .\mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "   cd packedgo\back\event-service && .\mvnw spring-boot:run" -ForegroundColor Gray
    exit 1
}

Write-Host "📋 PASO 2: Verificando configuración de MercadoPago..." -ForegroundColor $infoColor

# Leer archivo .env del payment-service
$envFile = ".\packedgo\back\payment-service\.env"
if (Test-Path $envFile) {
    $envContent = Get-Content $envFile -Raw
    
    if ($envContent -match "MERCADOPAGO_ACCESS_TOKEN=APP_USR-\d+") {
        Write-Host "  ✅ Access Token configurado" -ForegroundColor $successColor
    } else {
        Write-Host "  ❌ Access Token NO configurado o inválido" -ForegroundColor $errorColor
    }
    
    if ($envContent -match "MERCADOPAGO_PUBLIC_KEY=APP_USR-") {
        Write-Host "  ✅ Public Key configurado" -ForegroundColor $successColor
    } else {
        Write-Host "  ❌ Public Key NO configurado o inválido" -ForegroundColor $errorColor
    }
    
    # Verificar webhook
    if ($envContent -match "WEBHOOK_URL=\s*$") {
        Write-Host "  ✅ Webhook deshabilitado (usando polling manual)" -ForegroundColor $successColor
    } elseif ($envContent -match "WEBHOOK_URL=https://") {
        Write-Host "  ✅ Webhook configurado con HTTPS" -ForegroundColor $successColor
    } else {
        Write-Host "  ⚠️  Webhook configurado con HTTP (solo funciona en sandbox)" -ForegroundColor $warningColor
    }
} else {
    Write-Host "  ❌ Archivo .env no encontrado" -ForegroundColor $errorColor
}

Write-Host ""

Write-Host "📋 PASO 3: Instrucciones para probar el flujo completo" -ForegroundColor $infoColor
Write-Host ""
Write-Host "  1️⃣  Abre el navegador en: http://localhost:4200" -ForegroundColor White
Write-Host "  2️⃣  Inicia sesión como CUSTOMER o regístrate" -ForegroundColor White
Write-Host "  3️⃣  Agrega eventos al carrito" -ForegroundColor White
Write-Host "  4️⃣  Haz checkout y paga con cuenta de prueba:" -ForegroundColor White
Write-Host ""
Write-Host "      💳 DATOS DE TARJETA DE PRUEBA:" -ForegroundColor $infoColor
Write-Host "      Número: 5031 7557 3453 0604" -ForegroundColor Yellow
Write-Host "      CVV: 123" -ForegroundColor Yellow
Write-Host "      Fecha: Cualquier fecha futura (ej: 11/25)" -ForegroundColor Yellow
Write-Host "      Nombre: APRO (para aprobar) o OTROC (para rechazar)" -ForegroundColor Yellow
Write-Host ""
Write-Host "  5️⃣  Espera la redirección automática" -ForegroundColor White
Write-Host "  6️⃣  El sistema verificará automáticamente el pago (2 segundos)" -ForegroundColor White
Write-Host "  7️⃣  Verás tus tickets con códigos QR" -ForegroundColor White
Write-Host ""

Write-Host "🔍 PASO 4: Monitoreo de logs" -ForegroundColor $infoColor
Write-Host ""
Write-Host "  Para ver los logs en tiempo real, ejecuta en terminales separadas:" -ForegroundColor White
Write-Host ""
Write-Host "  # Terminal 1 - Payment Service" -ForegroundColor Gray
Write-Host "  cd packedgo\back\payment-service" -ForegroundColor Gray
Write-Host "  Get-Content .\logs\payment-service.log -Wait -Tail 20" -ForegroundColor Gray
Write-Host ""
Write-Host "  # Terminal 2 - Order Service" -ForegroundColor Gray
Write-Host "  cd packedgo\back\order-service" -ForegroundColor Gray
Write-Host "  Get-Content .\logs\order-service.log -Wait -Tail 20" -ForegroundColor Gray
Write-Host ""

Write-Host "✅ LOGS A BUSCAR:" -ForegroundColor $infoColor
Write-Host "  Payment Service:" -ForegroundColor White
Write-Host "    - 'Preferencia creada exitosamente'" -ForegroundColor Gray
Write-Host "    - 'POST /api/payments/verify/{orderId}'" -ForegroundColor Gray
Write-Host "    - 'Verificando estado del pago en MercadoPago'" -ForegroundColor Gray
Write-Host "    - 'Notificando aprobación de pago a order-service'" -ForegroundColor Gray
Write-Host ""
Write-Host "  Order Service:" -ForegroundColor White
Write-Host "    - 'Order ... marked as PAID'" -ForegroundColor Gray
Write-Host "    - '🎟️ Generating tickets for order'" -ForegroundColor Gray
Write-Host "    - '✅ Ticket #1 generated'" -ForegroundColor Gray
Write-Host ""

Write-Host "🐛 TROUBLESHOOTING:" -ForegroundColor $warningColor
Write-Host ""
Write-Host "  Si los tickets NO se generan:" -ForegroundColor White
Write-Host "    1. Verifica que payment-service esté en puerto 8085" -ForegroundColor Gray
Write-Host "    2. Verifica los logs del payment-service" -ForegroundColor Gray
Write-Host "    3. Espera al menos 2 segundos después del pago" -ForegroundColor Gray
Write-Host "    4. Recarga la página de order-success" -ForegroundColor Gray
Write-Host "    5. Revisa la consola del navegador (F12)" -ForegroundColor Gray
Write-Host ""

Write-Host "📞 SOPORTE:" -ForegroundColor $infoColor
Write-Host "  Si sigues teniendo problemas, revisa el archivo:" -ForegroundColor White
Write-Host "  DIAGNOSTICO_FLUJO_PAGO_Y_QR.md" -ForegroundColor Yellow
Write-Host ""

Write-Host "====================================================`n" -ForegroundColor Cyan
Write-Host "✨ ¡Todo listo para probar! Presiona cualquier tecla para abrir el frontend..." -ForegroundColor Green
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

# Abrir navegador
Start-Process "http://localhost:4200"

Write-Host "🎉 ¡Navegador abierto! Sigue las instrucciones arriba." -ForegroundColor Green
