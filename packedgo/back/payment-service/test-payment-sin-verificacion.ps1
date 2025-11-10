# ========================================
# Test Payment - Sin Verificación
# ========================================
# Este script prueba pagos usando tarjetas directamente
# sin necesidad de usuarios de prueba que requieren verificación

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "🧪 TEST PAYMENT - SIN VERIFICACIÓN" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuración
$baseUrl = "http://localhost:8082"
$adminId = 1

# ========================================
# 1. Verificar Health
# ========================================
Write-Host "1️⃣  Verificando Health..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/payments/health" -Method Get
    Write-Host "✅ Servicio UP" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ Error: Servicio no disponible" -ForegroundColor Red
    Write-Host "Asegúrate de que el servicio esté corriendo en puerto 8082" -ForegroundColor Red
    exit
}

# ========================================
# 2. Verificar Credenciales
# ========================================
Write-Host "2️⃣  Verificando credenciales de admin..." -ForegroundColor Yellow
try {
    $credCheck = Invoke-RestMethod -Uri "$baseUrl/api/admin/credentials/check/$adminId" -Method Get
    
    if ($credCheck.hasCredentials) {
        Write-Host "✅ Admin $adminId tiene credenciales configuradas" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Admin $adminId NO tiene credenciales" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Ejecuta primero:" -ForegroundColor Yellow
        Write-Host '  $cred = @{ adminId=1; accessToken="TU_ACCESS_TOKEN"; publicKey="TU_PUBLIC_KEY"; isSandbox=$true } | ConvertTo-Json' -ForegroundColor Gray
        Write-Host '  Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials" -Method Post -ContentType "application/json" -Body $cred' -ForegroundColor Gray
        exit
    }
    Write-Host ""
} catch {
    Write-Host "❌ Error verificando credenciales: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# ========================================
# 3. Crear Preferencia de Pago
# ========================================
Write-Host "3️⃣  Creando preferencia de pago..." -ForegroundColor Yellow

$payment = @{
    adminId = $adminId
    orderId = "ORDER-TEST-$(Get-Date -Format 'yyyyMMddHHmmss')"
    amount = 1500.00
    description = "Paquete Premium - Prueba Sin Verificación"
    payerEmail = "comprador@test.com"  # Email genérico
    payerName = "Juan Pérez Test"
    externalReference = "REF-$(Get-Date -Format 'yyyyMMddHHmmss')"
    successUrl = "http://localhost:3000/payment/success"
    failureUrl = "http://localhost:3000/payment/failure"
    pendingUrl = "http://localhost:3000/payment/pending"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/payments/create" `
        -Method Post `
        -ContentType "application/json" `
        -Body $payment
    
    Write-Host "✅ Preferencia creada exitosamente" -ForegroundColor Green
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "📋 INFORMACIÓN DEL PAGO" -ForegroundColor Cyan
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "Payment ID    : $($response.paymentId)" -ForegroundColor White
    Write-Host "Order ID      : $($response.orderId)" -ForegroundColor White
    Write-Host "Status        : $($response.status)" -ForegroundColor White
    Write-Host "Amount        : $$($response.amount) $($response.currency)" -ForegroundColor White
    Write-Host "Preference ID : $($response.preferenceId)" -ForegroundColor White
    Write-Host ""
    
    # ========================================
    # 4. Mostrar Link de Pago
    # ========================================
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host "🌐 LINK DE PAGO (SANDBOX)" -ForegroundColor Green
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
    Write-Host ""
    Write-Host $response.sandboxInitPoint -ForegroundColor Yellow
    Write-Host ""
    
    # ========================================
    # 5. Instrucciones de Prueba
    # ========================================
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host "📝 INSTRUCCIONES PARA COMPLETAR EL PAGO" -ForegroundColor Magenta
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host ""
    Write-Host "1. Abre el link en tu navegador" -ForegroundColor White
    Write-Host "2. Selecciona 'Pagar como invitado' (NO uses login)" -ForegroundColor White
    Write-Host "3. Usa estos datos de tarjeta:" -ForegroundColor White
    Write-Host ""
    Write-Host "   ╔══════════════════════════════════╗" -ForegroundColor Green
    Write-Host "   ║  TARJETA DE PRUEBA (APROBADA)   ║" -ForegroundColor Green
    Write-Host "   ╠══════════════════════════════════╣" -ForegroundColor Green
    Write-Host "   ║  Número : 5031 7557 3453 0604   ║" -ForegroundColor White
    Write-Host "   ║  CVV    : 123                    ║" -ForegroundColor White
    Write-Host "   ║  Venc.  : 11/25                  ║" -ForegroundColor White
    Write-Host "   ║  Nombre : APRO                   ║" -ForegroundColor White
    Write-Host "   ║  DNI    : 12345678               ║" -ForegroundColor White
    Write-Host "   ╚══════════════════════════════════╝" -ForegroundColor Green
    Write-Host ""
    Write-Host "4. Email: cualquier@email.com" -ForegroundColor White
    Write-Host "5. Completa el pago" -ForegroundColor White
    Write-Host ""
    
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host "💡 OTRAS TARJETAS DE PRUEBA" -ForegroundColor Cyan
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "✅ APROBADA : 5031 7557 3453 0604 (Mastercard)" -ForegroundColor Green
    Write-Host "✅ APROBADA : 4509 9535 6623 3704 (Visa)" -ForegroundColor Green
    Write-Host "⏳ PENDIENTE: 4013 5406 8274 6260 (Visa)" -ForegroundColor Yellow
    Write-Host "❌ RECHAZADA: 4074 5957 4392 9691 (Visa)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Todas usan CVV: 123, Vencimiento: 11/25" -ForegroundColor Gray
    Write-Host ""
    
    # ========================================
    # 6. Abrir en Navegador (Opcional)
    # ========================================
    Write-Host "¿Deseas abrir el link automáticamente en el navegador? (S/N): " -ForegroundColor Yellow -NoNewline
    $respuesta = Read-Host
    
    if ($respuesta -eq "S" -or $respuesta -eq "s") {
        Start-Process $response.sandboxInitPoint
        Write-Host "✅ Navegador abierto" -ForegroundColor Green
    }
    Write-Host ""
    
    # ========================================
    # 7. Esperar Webhook (Opcional)
    # ========================================
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
    Write-Host "⏳ Esperando confirmación del pago..." -ForegroundColor Yellow
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Presiona cualquier tecla después de completar el pago para verificar el estado..." -ForegroundColor Gray
    $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
    
    # Verificar estado del pago
    Write-Host ""
    Write-Host "Verificando estado del pago..." -ForegroundColor Yellow
    try {
        $statusCheck = Invoke-RestMethod -Uri "$baseUrl/api/payments/status/$($response.orderId)" -Method Get
        Write-Host ""
        Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
        Write-Host "📊 ESTADO ACTUAL DEL PAGO" -ForegroundColor Cyan
        Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
        Write-Host "Order ID      : $($statusCheck.orderId)" -ForegroundColor White
        Write-Host "Status        : $($statusCheck.status)" -ForegroundColor $(
            switch ($statusCheck.status) {
                "APPROVED" { "Green" }
                "PENDING" { "Yellow" }
                "REJECTED" { "Red" }
                default { "White" }
            }
        )
        Write-Host "Amount        : $$($statusCheck.amount) $($statusCheck.currency)" -ForegroundColor White
        Write-Host "Created       : $($statusCheck.createdAt)" -ForegroundColor White
        if ($statusCheck.mercadoPagoId) {
            Write-Host "MP Payment ID : $($statusCheck.mercadoPagoId)" -ForegroundColor White
        }
        Write-Host ""
    } catch {
        Write-Host "⚠️  No se pudo verificar el estado automáticamente" -ForegroundColor Yellow
        Write-Host "Verifica manualmente en la base de datos o logs" -ForegroundColor Yellow
        Write-Host ""
    }
    
} catch {
    Write-Host "❌ Error creando preferencia: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "✅ Script completado" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
