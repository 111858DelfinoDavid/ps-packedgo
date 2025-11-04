# Test directo del endpoint de payment

Write-Host "🧪 TEST DIRECTO: Crear Payment Preference (sin autenticación)" -ForegroundColor Cyan
Write-Host "================================================================`n" -ForegroundColor Cyan

$paymentBody = @{
    orderId = "ORD-20241117829089422"
    adminId = 4
    amount = 50000.00
    description = "Compra de tickets - Nina Kraviz"
    sessionId = "test-session-123"
    payerEmail = "customer@test.com"
    payerName = "Test Customer"
    externalReference = "ORD-20241117829089422"
    successUrl = "http://localhost:4200/customer/checkout?status=approved"
    failureUrl = "http://localhost:4200/customer/checkout?status=failure"
    pendingUrl = "http://localhost:4200/customer/checkout?status=pending"
} | ConvertTo-Json

Write-Host "Request body:" -ForegroundColor Gray
Write-Host $paymentBody -ForegroundColor Gray
Write-Host ""

try {
    $paymentResponse = Invoke-RestMethod -Uri "http://localhost:8085/api/payments/create" `
        -Method POST `
        -ContentType "application/json" `
        -Body $paymentBody `
        -TimeoutSec 30
    
    Write-Host "✅ Payment Preference creada exitosamente!" -ForegroundColor Green
    Write-Host "`nRespuesta:" -ForegroundColor Cyan
    $paymentResponse | ConvertTo-Json -Depth 3
    
} catch {
    Write-Host "❌ Error creando payment:" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        Write-Host "`nDetalles del error:" -ForegroundColor Yellow
        Write-Host $_.ErrorDetails.Message -ForegroundColor Red
    }
}

Write-Host "`n================================================================`n" -ForegroundColor Cyan
