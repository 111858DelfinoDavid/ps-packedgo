# Script para probar la creación de payment preference

Write-Host "🧪 TEST: Crear Payment Preference" -ForegroundColor Cyan
Write-Host "=========================================`n" -ForegroundColor Cyan

# Primero hacer login como admin para obtener el token
Write-Host "1️⃣ Login como Admin (ID: 1)..." -ForegroundColor Yellow
$loginBody = @{
    email = "admin@packedgo.com"
    password = "Admin123!"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/admin/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
    
    $token = $loginResponse.token
    Write-Host "✅ Login exitoso" -ForegroundColor Green
    Write-Host "Token: $($token.Substring(0, 30))...`n" -ForegroundColor Gray
} catch {
    Write-Host "❌ Error en login:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

# Ahora crear la payment preference
Write-Host "2️⃣ Crear Payment Preference..." -ForegroundColor Yellow

$paymentBody = @{
    orderId = "ORD-20241117829089422"
    adminId = 1
    amount = 50000.00
    description = "Compra de tickets - Nina Kraviz"
    sessionId = "test-session-123"
} | ConvertTo-Json

Write-Host "Request body:" -ForegroundColor Gray
Write-Host $paymentBody -ForegroundColor Gray
Write-Host ""

try {
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    $paymentResponse = Invoke-RestMethod -Uri "http://localhost:8085/api/payments/create" `
        -Method POST `
        -Headers $headers `
        -Body $paymentBody
    
    Write-Host "✅ Payment Preference creada exitosamente!" -ForegroundColor Green
    Write-Host "`nRespuesta:" -ForegroundColor Cyan
    Write-Host "Preference ID: $($paymentResponse.preferenceId)" -ForegroundColor Green
    Write-Host "Init Point: $($paymentResponse.initPoint)" -ForegroundColor Green
    Write-Host "QR URL: $($paymentResponse.qrUrl)" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Error creando payment:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        Write-Host "`nDetalles del error:" -ForegroundColor Yellow
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
}

Write-Host "`n=========================================`n" -ForegroundColor Cyan
