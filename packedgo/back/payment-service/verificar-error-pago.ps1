# ========================================
# Script de Diagnóstico - Error de Pago
# ========================================

Write-Host "========================================" -ForegroundColor Red
Write-Host "🔍 DIAGNÓSTICO DE ERROR DE PAGO" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
Write-Host ""

$baseUrl = "http://localhost:8085/api"  # Nota: puerto 8085 según application.properties

# ========================================
# 1. Verificar que el servicio esté corriendo
# ========================================
Write-Host "1️⃣  Verificando estado del servicio..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/payments/health" -Method Get
    Write-Host "✅ Servicio respondiendo" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ ERROR: Servicio no responde" -ForegroundColor Red
    Write-Host "El servicio debería estar en puerto 8085 (verifica application.properties)" -ForegroundColor Yellow
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Soluciones:" -ForegroundColor Yellow
    Write-Host "1. Verifica que el servicio esté corriendo: mvn spring-boot:run" -ForegroundColor Gray
    Write-Host "2. Verifica el puerto en application.properties" -ForegroundColor Gray
    exit
}

# ========================================
# 2. Verificar credenciales
# ========================================
Write-Host "2️⃣  Verificando credenciales..." -ForegroundColor Yellow

Write-Host "Ingresa el Admin ID a verificar [1]: " -NoNewline -ForegroundColor Cyan
$adminInput = Read-Host
$adminId = if ($adminInput) { $adminInput } else { 1 }

try {
    $credCheck = Invoke-RestMethod -Uri "$baseUrl/admin/credentials/check/$adminId" -Method Get
    
    if ($credCheck.hasCredentials) {
        Write-Host "✅ Admin $adminId tiene credenciales" -ForegroundColor Green
    } else {
        Write-Host "❌ Admin $adminId NO tiene credenciales configuradas" -ForegroundColor Red
        Write-Host ""
        Write-Host "SOLUCIÓN: Configura las credenciales primero:" -ForegroundColor Yellow
        Write-Host @"
`$cred = @{
    adminId = $adminId
    accessToken = "APP_USR-1160956444149133-101721-055aec8c374959f568654aeda79ccd31-2932397372"
    publicKey = "APP_USR-704e26b4-2405-4401-8cd9-fe981e4f70ae"
    isSandbox = `$true
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/admin/credentials" ``
    -Method Post ``
    -ContentType "application/json" ``
    -Body `$cred
"@ -ForegroundColor Gray
        exit
    }
} catch {
    Write-Host "❌ Error verificando credenciales: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    exit
}
Write-Host ""

# ========================================
# 3. Verificar últimos pagos en BD
# ========================================
Write-Host "3️⃣  Verificando últimos pagos..." -ForegroundColor Yellow

try {
    # Intentar obtener lista de pagos
    $payments = Invoke-RestMethod -Uri "$baseUrl/payments/admin/$adminId" -Method Get -ErrorAction SilentlyContinue
    
    if ($payments -and $payments.Count -gt 0) {
        Write-Host "📊 Últimos 3 pagos:" -ForegroundColor Cyan
        $payments | Select-Object -First 3 | ForEach-Object {
            Write-Host "  - ID: $($_.paymentId) | Order: $($_.orderId) | Status: $($_.status) | Amount: $$($_.amount)" -ForegroundColor White
        }
    } else {
        Write-Host "ℹ️  No hay pagos registrados aún" -ForegroundColor Gray
    }
} catch {
    Write-Host "⚠️  No se pudo obtener lista de pagos (endpoint puede no existir)" -ForegroundColor Yellow
}
Write-Host ""

# ========================================
# 4. Crear pago de prueba
# ========================================
Write-Host "4️⃣  Creando pago de prueba..." -ForegroundColor Yellow

$payment = @{
    adminId = [int]$adminId
    orderId = "ORDER-TEST-$(Get-Date -Format 'yyyyMMddHHmmss')"
    amount = 1500.00
    description = "Test - Diagnóstico de Error"
    payerEmail = "test@test.com"
    payerName = "Test User"
    externalReference = "REF-TEST-$(Get-Date -Format 'yyyyMMddHHmmss')"
    successUrl = "http://localhost:3000/payment/success"
    failureUrl = "http://localhost:3000/payment/failure"
    pendingUrl = "http://localhost:3000/payment/pending"
} | ConvertTo-Json

Write-Host "Payload enviado:" -ForegroundColor Gray
Write-Host $payment -ForegroundColor DarkGray
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/payments/create" `
        -Method Post `
        -ContentType "application/json" `
        -Body $payment
    
    Write-Host "✅ Preferencia creada exitosamente" -ForegroundColor Green
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host "📋 RESPUESTA DEL SERVICIO" -ForegroundColor Green
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host -ForegroundColor White
    Write-Host ""
    
    Write-Host "🌐 Link de pago (SANDBOX):" -ForegroundColor Cyan
    Write-Host $response.sandboxInitPoint -ForegroundColor Yellow
    Write-Host ""
    
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host "💳 INFORMACIÓN IMPORTANTE" -ForegroundColor Magenta
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host ""
    Write-Host "⚠️  SI VES ERROR 'No pudimos procesar tu pago':" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Causas comunes:" -ForegroundColor White
    Write-Host "1. ❌ Credenciales inválidas o expiradas" -ForegroundColor Red
    Write-Host "2. ❌ Access Token incorrecto" -ForegroundColor Red
    Write-Host "3. ❌ Cuenta de MercadoPago no verificada" -ForegroundColor Red
    Write-Host "4. ❌ Límites de sandbox excedidos" -ForegroundColor Red
    Write-Host "5. ❌ Webhook URL incorrecta (menos común)" -ForegroundColor Red
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "🔧 SOLUCIONES" -ForegroundColor Cyan
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "Solución 1: Verificar Credenciales" -ForegroundColor Yellow
    Write-Host "  1. Ve a: https://www.mercadopago.com.ar/developers/panel/credentials" -ForegroundColor White
    Write-Host "  2. Asegúrate de estar en modo 'Pruebas' (TEST)" -ForegroundColor White
    Write-Host "  3. Copia NUEVAMENTE tu Access Token de TEST" -ForegroundColor White
    Write-Host "  4. Debe empezar con: APP_USR- o TEST-" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Solución 2: Actualizar Credenciales" -ForegroundColor Yellow
    Write-Host @"
`$newCred = @{
    adminId = $adminId
    accessToken = "TU_NUEVO_ACCESS_TOKEN_AQUI"
    publicKey = "TU_NUEVO_PUBLIC_KEY_AQUI"
    isSandbox = `$true
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/admin/credentials" ``
    -Method Post ``
    -ContentType "application/json" ``
    -Body `$newCred
"@ -ForegroundColor Gray
    Write-Host ""
    
    Write-Host "Solución 3: Verificar cuenta de MercadoPago" -ForegroundColor Yellow
    Write-Host "  1. Inicia sesión en https://www.mercadopago.com.ar" -ForegroundColor White
    Write-Host "  2. Verifica que tu cuenta esté activa" -ForegroundColor White
    Write-Host "  3. Ve a Desarrolladores > Credenciales" -ForegroundColor White
    Write-Host "  4. Asegúrate que las credenciales de TEST estén habilitadas" -ForegroundColor White
    Write-Host ""
    
    Write-Host "Solución 4: Probar con credenciales nuevas" -ForegroundColor Yellow
    Write-Host "  Si las credenciales son muy antiguas, genera nuevas:" -ForegroundColor White
    Write-Host "  1. Panel de Desarrolladores > Credenciales" -ForegroundColor White
    Write-Host "  2. Click en 'Generar nuevas credenciales de prueba'" -ForegroundColor White
    Write-Host ""
    
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host "🧪 CÓMO PROBAR EL PAGO" -ForegroundColor Green
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host ""
    Write-Host "1. Abre el link de pago en el navegador" -ForegroundColor White
    Write-Host "2. Selecciona 'Pagar como invitado'" -ForegroundColor White
    Write-Host "3. Tarjeta: 5031 7557 3453 0604" -ForegroundColor White
    Write-Host "4. CVV: 123 | Vencimiento: 11/25" -ForegroundColor White
    Write-Host "5. Nombre: APRO" -ForegroundColor White
    Write-Host "6. Email: test@test.com" -ForegroundColor White
    Write-Host ""
    
    Write-Host "¿Abrir link en navegador? (S/N): " -NoNewline -ForegroundColor Yellow
    $open = Read-Host
    if ($open -eq "S" -or $open -eq "s") {
        Start-Process $response.sandboxInitPoint
        Write-Host "✅ Navegador abierto" -ForegroundColor Green
    }
    
} catch {
    Write-Host "❌ ERROR AL CREAR PAGO" -ForegroundColor Red
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.ErrorDetails) {
        Write-Host ""
        Write-Host "Detalles del error:" -ForegroundColor Yellow
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Red
    Write-Host "🔍 ANÁLISIS DEL ERROR" -ForegroundColor Red
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Red
    Write-Host ""
    
    $errorMsg = $_.Exception.Message
    
    if ($errorMsg -match "401|Unauthorized|credentials") {
        Write-Host "❌ PROBLEMA: Credenciales inválidas" -ForegroundColor Red
        Write-Host "SOLUCIÓN: Regenera las credenciales en MercadoPago" -ForegroundColor Yellow
    }
    elseif ($errorMsg -match "404|Not Found") {
        Write-Host "❌ PROBLEMA: Endpoint no encontrado" -ForegroundColor Red
        Write-Host "SOLUCIÓN: Verifica que el servicio esté corriendo en puerto 8085" -ForegroundColor Yellow
    }
    elseif ($errorMsg -match "500|Internal Server Error") {
        Write-Host "❌ PROBLEMA: Error en el servidor" -ForegroundColor Red
        Write-Host "SOLUCIÓN: Revisa los logs del servicio" -ForegroundColor Yellow
        Write-Host "Logs en: target/logs o consola donde corriste mvn spring-boot:run" -ForegroundColor Gray
    }
    else {
        Write-Host "❌ PROBLEMA: Error desconocido" -ForegroundColor Red
        Write-Host "SOLUCIÓN: Revisa los logs completos" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ Diagnóstico completado" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
