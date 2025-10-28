# Script de prueba completo para Payment Service
# Este script prueba todos los endpoints del servicio

Write-Host "==================================" -ForegroundColor Cyan
Write-Host "  PAYMENT SERVICE - TEST SCRIPT  " -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8082"

# Función para mostrar resultados
function Show-Result {
    param($title, $result)
    Write-Host "`n--- $title ---" -ForegroundColor Yellow
    $result | ConvertTo-Json -Depth 5
    Write-Host ""
}

# 1. Health Check
Write-Host "[1/5] Probando endpoint de salud..." -ForegroundColor Green
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/payments/health" -Method Get
    Show-Result "Health Check" $health
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Actuator Health
Write-Host "[2/5] Probando Actuator Health..." -ForegroundColor Green
try {
    $actuator = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get
    Show-Result "Actuator Health" $actuator
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Guardar credenciales de Admin
Write-Host "[3/5] Guardando credenciales del Admin..." -ForegroundColor Green
try {
    $credBody = @{
        adminId = 1
        accessToken = "TEST-1234567890-ACCESS-TOKEN-DEMO"
        publicKey = "TEST-PUBLIC-KEY-1234567890-DEMO"
        isSandbox = $true
    } | ConvertTo-Json

    $credResponse = Invoke-RestMethod -Uri "$baseUrl/api/admin/credentials" `
        -Method Post `
        -Body $credBody `
        -ContentType "application/json"
    
    Show-Result "Credenciales Guardadas" $credResponse
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Verificar credenciales
Write-Host "[4/5] Verificando credenciales del Admin..." -ForegroundColor Green
try {
    $checkCred = Invoke-RestMethod -Uri "$baseUrl/api/admin/credentials/check/1" -Method Get
    Show-Result "Verificación de Credenciales" $checkCred
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Crear un pago
Write-Host "[5/5] Intentando crear un pago..." -ForegroundColor Green
try {
    $paymentBody = @{
        adminId = 1
        orderId = "ORDER-TEST-" + (Get-Date -Format "yyyyMMddHHmmss")
        amount = 150.75
        description = "Compra de producto de prueba - Testing Payment Service"
        payerEmail = "comprador@test.com"
        payerName = "Juan Perez"
        externalReference = "REF-TEST-001"
        successUrl = "https://tusitio.com/success"
        failureUrl = "https://tusitio.com/failure"
        pendingUrl = "https://tusitio.com/pending"
    } | ConvertTo-Json

    $paymentResponse = Invoke-RestMethod -Uri "$baseUrl/api/payments/create" `
        -Method Post `
        -Body $paymentBody `
        -ContentType "application/json"
    
    Show-Result "Pago Creado" $paymentResponse
    
    Write-Host "`nNOTA: Este pago fallará con MercadoPago porque el token es de prueba." -ForegroundColor Yellow
    Write-Host "Para probar con tokens reales, reemplaza el accessToken en el paso 3." -ForegroundColor Yellow
    
} catch {
    $errorResponse = $_.Exception.Response
    
    if ($null -ne $errorResponse) {
        $reader = New-Object System.IO.StreamReader($errorResponse.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        
        Write-Host "`nStatus Code: $($errorResponse.StatusCode)" -ForegroundColor Red
        Write-Host "Response:" -ForegroundColor Red
        
        try {
            $errorJson = $responseBody | ConvertFrom-Json
            $errorJson | ConvertTo-Json -Depth 5
        } catch {
            Write-Host $responseBody -ForegroundColor Red
        }
    } else {
        Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n==================================" -ForegroundColor Cyan
Write-Host "      PRUEBAS COMPLETADAS        " -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
