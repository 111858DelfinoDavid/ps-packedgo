# Script para verificar credenciales de MercadoPago
param(
    [string]$AccessToken = "APP_USR-1160956444149133-101721-055aec8c374959f568654aeda79ccd31-2932397372"
)

Write-Host "🔍 Verificando credenciales de MercadoPago..." -ForegroundColor Cyan
Write-Host "Access Token: $($AccessToken.Substring(0, 30))..." -ForegroundColor Yellow
Write-Host ""

# Test 1: Verificar que el token sea válido
Write-Host "📋 Test 1: Consultando información de la cuenta..." -ForegroundColor Cyan

try {
    $headers = @{
        "Authorization" = "Bearer $AccessToken"
        "Content-Type" = "application/json"
    }
    
    $response = Invoke-RestMethod -Uri "https://api.mercadopago.com/users/me" -Method Get -Headers $headers
    
    Write-Host "✅ Token válido!" -ForegroundColor Green
    Write-Host "   User ID: $($response.id)" -ForegroundColor White
    Write-Host "   Email: $($response.email)" -ForegroundColor White
    Write-Host "   Nickname: $($response.nickname)" -ForegroundColor White
    Write-Host "   Site ID: $($response.site_id)" -ForegroundColor White
    Write-Host ""
    
    # Test 2: Crear una preferencia de prueba
    Write-Host "📋 Test 2: Creando preferencia de pago de prueba..." -ForegroundColor Cyan
    
    $preferenceData = @{
        items = @(
            @{
                title = "Test Item"
                quantity = 1
                unit_price = 100
                currency_id = "ARS"
            }
        )
        back_urls = @{
            success = "http://localhost:4200/success"
            failure = "http://localhost:4200/failure"
            pending = "http://localhost:4200/pending"
        }
        external_reference = "TEST-$(Get-Date -Format 'yyyyMMddHHmmss')"
    } | ConvertTo-Json -Depth 10
    
    $prefResponse = Invoke-RestMethod -Uri "https://api.mercadopago.com/checkout/preferences" -Method Post -Headers $headers -Body $preferenceData
    
    Write-Host "✅ Preferencia creada exitosamente!" -ForegroundColor Green
    Write-Host "   Preference ID: $($prefResponse.id)" -ForegroundColor White
    Write-Host "   Init Point: $($prefResponse.init_point)" -ForegroundColor White
    Write-Host "   Sandbox Init Point: $($prefResponse.sandbox_init_point)" -ForegroundColor White
    Write-Host ""
    Write-Host "🎯 Prueba este link en tu navegador:" -ForegroundColor Yellow
    Write-Host "   $($prefResponse.sandbox_init_point)" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📝 Si al abrir el link anterior ves el checkout de MercadoPago SIN errores," -ForegroundColor Yellow
    Write-Host "   entonces tus credenciales son VÁLIDAS." -ForegroundColor Yellow
    Write-Host ""
    
} catch {
    Write-Host "❌ Error al verificar credenciales:" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "   Detalles: $responseBody" -ForegroundColor Red
    }
    
    Write-Host ""
    Write-Host "💡 Posibles causas:" -ForegroundColor Yellow
    Write-Host "   1. El Access Token ha expirado" -ForegroundColor White
    Write-Host "   2. El Access Token no es de prueba (sandbox)" -ForegroundColor White
    Write-Host "   3. El Access Token no es válido" -ForegroundColor White
    Write-Host ""
    Write-Host "🔧 Solución: Ve a https://www.mercadopago.com.ar/developers/panel/app" -ForegroundColor Yellow
    Write-Host "   y obtén nuevas credenciales de PRUEBA" -ForegroundColor Yellow
}
