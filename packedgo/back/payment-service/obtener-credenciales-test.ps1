# ========================================
# Guía: Obtener Credenciales de TEST de MercadoPago
# ========================================

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "📋 CÓMO OBTENER CREDENCIALES DE TEST" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

Write-Host "Pasos:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Abre tu navegador e ingresa a:" -ForegroundColor White
Write-Host "   https://www.mercadopago.com.ar/developers/panel/app" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. Inicia sesión con tu cuenta de MercadoPago" -ForegroundColor White
Write-Host ""
Write-Host "3. Selecciona tu aplicación o crea una nueva" -ForegroundColor White
Write-Host ""
Write-Host "4. En el menú lateral, busca:" -ForegroundColor White
Write-Host "   'Credenciales de prueba' o 'Test credentials'" -ForegroundColor Cyan
Write-Host ""
Write-Host "5. Copia las credenciales que empiezan con TEST-" -ForegroundColor White
Write-Host "   - Access Token: TEST-123456..." -ForegroundColor Gray
Write-Host "   - Public Key: TEST-abc123..." -ForegroundColor Gray
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host "⚠️  IMPORTANTE" -ForegroundColor Yellow
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Yellow
Write-Host ""
Write-Host "Las credenciales deben EMPEZAR con:" -ForegroundColor White
Write-Host "  ✅ TEST-12345... (CORRECTO para pruebas)" -ForegroundColor Green
Write-Host "  ❌ APP_USR-12345... (PRODUCCIÓN - pagos reales)" -ForegroundColor Red
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
Write-Host "🔧 CONFIGURAR CREDENCIALES" -ForegroundColor Magenta
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
Write-Host ""
Write-Host "Una vez que tengas tus credenciales de TEST, ejecuta:" -ForegroundColor White
Write-Host ""
Write-Host @"
`$credenciales = @{
    adminId = 1
    accessToken = "TEST-TU_ACCESS_TOKEN_AQUI"
    publicKey = "TEST-TU_PUBLIC_KEY_AQUI"
    isSandbox = `$true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8085/api/admin/credentials" ``
    -Method Post ``
    -ContentType "application/json" ``
    -Body `$credenciales
"@ -ForegroundColor Cyan
Write-Host ""

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "🚀 PROBAR PAGO" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host ""
Write-Host "Después de configurar, ejecuta:" -ForegroundColor White
Write-Host "  .\test-payment-sin-verificacion.ps1" -ForegroundColor Cyan
Write-Host ""

Write-Host "¿Abrir el panel de desarrolladores de MercadoPago? (S/N): " -NoNewline -ForegroundColor Yellow
$respuesta = Read-Host

if ($respuesta -eq "S" -or $respuesta -eq "s") {
    Start-Process "https://www.mercadopago.com.ar/developers/panel/app"
    Write-Host "✅ Navegador abierto" -ForegroundColor Green
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "📌 ALTERNATIVA: Usar Credenciales de Producción" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""
Write-Host "Si prefieres usar tus credenciales APP_USR- (PRODUCCIÓN):" -ForegroundColor White
Write-Host ""
Write-Host @"
`$credProd = @{
    adminId = 1
    accessToken = "APP_USR-1160956444149133-101721-055aec8c374959f568654aeda79ccd31-2932397372"
    publicKey = "APP_USR-704e26b4-2405-4401-8cd9-fe981e4f70ae"
    isSandbox = `$false  # ← IMPORTANTE: FALSE para producción
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8085/api/admin/credentials" ``
    -Method Post ``
    -ContentType "application/json" ``
    -Body `$credProd
"@ -ForegroundColor Yellow
Write-Host ""
Write-Host "⚠️  ADVERTENCIA: Los pagos serán REALES y se cobrarán comisiones" -ForegroundColor Red
Write-Host "   Solo usa esto si quieres hacer pagos de verdad" -ForegroundColor Red
Write-Host ""
