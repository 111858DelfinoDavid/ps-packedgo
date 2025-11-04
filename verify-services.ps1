# ========================================
# SCRIPT DE VERIFICACIÓN - SERVICIOS PACKEDGO
# ========================================
# Este script verifica que todos los servicios estén corriendo
# y muestra el estado de cada uno

Write-Host "`n🚀 VERIFICACIÓN DE SERVICIOS PACKEDGO" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Colores
$successColor = "Green"
$errorColor = "Red"
$warningColor = "Yellow"
$infoColor = "Cyan"

# URLs de los servicios
$services = @(
    @{Name="Auth Service"; Port=8081; Url="http://localhost:8081/api/health"},
    @{Name="Users Service"; Port=8082; Url="http://localhost:8082/api/health"},
    @{Name="Order Service"; Port=8084; Url="http://localhost:8084/api/health"},
    @{Name="Payment Service"; Port=8085; Url="http://localhost:8085/api/health"},
    @{Name="Event Service"; Port=8086; Url="http://localhost:8086/api/health"}
)

# Función para verificar servicio
function Test-Service {
    param (
        [string]$Name,
        [int]$Port,
        [string]$Url
    )
    
    try {
        # Intentar conexión HTTP
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✅ $Name" -ForegroundColor $successColor -NoNewline
            Write-Host " - Puerto $Port" -ForegroundColor Gray
            return $true
        }
    }
    catch {
        # Si falla HTTP, verificar si el puerto está en uso
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $tcpClient.Connect("localhost", $Port)
            $tcpClient.Close()
            
            Write-Host "  ⚠️  $Name" -ForegroundColor $warningColor -NoNewline
            Write-Host " - Puerto $Port en uso pero no responde a /health" -ForegroundColor Gray
            return $false
        }
        catch {
            Write-Host "  ❌ $Name" -ForegroundColor $errorColor -NoNewline
            Write-Host " - Puerto $Port no disponible" -ForegroundColor Gray
            return $false
        }
    }
}

# Verificar servicios
Write-Host "📋 Estado de los servicios:`n" -ForegroundColor $infoColor

$allServicesUp = $true
foreach ($service in $services) {
    $isUp = Test-Service -Name $service.Name -Port $service.Port -Url $service.Url
    if (-not $isUp) {
        $allServicesUp = $false
    }
}

# Verificar PostgreSQL
Write-Host "`n📊 Base de datos:" -ForegroundColor $infoColor
try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $tcpClient.Connect("localhost", 5432)
    $tcpClient.Close()
    Write-Host "  ✅ PostgreSQL - Puerto 5432" -ForegroundColor $successColor
}
catch {
    Write-Host "  ❌ PostgreSQL - Puerto 5432 no disponible" -ForegroundColor $errorColor
    $allServicesUp = $false
}

# Verificar frontend
Write-Host "`n🌐 Frontend:" -ForegroundColor $infoColor
try {
    $response = Invoke-WebRequest -Uri "http://localhost:4200" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "  ✅ Angular Frontend - Puerto 4200" -ForegroundColor $successColor
}
catch {
    try {
        $tcpClient = New-Object System.Net.Sockets.TcpClient
        $tcpClient.Connect("localhost", 4200)
        $tcpClient.Close()
        Write-Host "  ⚠️  Angular Frontend - Puerto 4200 en uso pero no responde" -ForegroundColor $warningColor
    }
    catch {
        Write-Host "  ❌ Angular Frontend - Puerto 4200 no disponible" -ForegroundColor $errorColor
    }
}

# Resumen
Write-Host "`n========================================" -ForegroundColor Cyan
if ($allServicesUp) {
    Write-Host "✅ Todos los servicios backend están funcionando" -ForegroundColor $successColor
    Write-Host "`n💡 Puedes proceder con las pruebas" -ForegroundColor $infoColor
} else {
    Write-Host "⚠️  Algunos servicios no están disponibles" -ForegroundColor $warningColor
    Write-Host "`n📝 Para iniciar los servicios:" -ForegroundColor $infoColor
    Write-Host "   cd packedgo\back\[servicio]" -ForegroundColor Gray
    Write-Host "   .\mvnw spring-boot:run" -ForegroundColor Gray
}

# Verificar configuración de MercadoPago
Write-Host "`n🔧 Verificando configuración de MercadoPago..." -ForegroundColor $infoColor
$envFile = "packedgo\back\payment-service\.env"
if (Test-Path $envFile) {
    $webhookUrl = Get-Content $envFile | Select-String "WEBHOOK_URL=" | ForEach-Object { $_.Line.Split('=')[1] }
    if ($webhookUrl -and $webhookUrl.Trim() -ne "") {
        Write-Host "  ✅ Webhook configurado: $webhookUrl" -ForegroundColor $successColor
    } else {
        Write-Host "  ⚠️  Webhook NO configurado" -ForegroundColor $warningColor
        Write-Host "     Para configurar webhooks en desarrollo:" -ForegroundColor Gray
        Write-Host "     1. Instalar ngrok: winget install ngrok" -ForegroundColor Gray
        Write-Host "     2. Ejecutar: ngrok http 8085" -ForegroundColor Gray
        Write-Host "     3. Copiar URL HTTPS y configurar en .env" -ForegroundColor Gray
    }
} else {
    Write-Host "  ⚠️  Archivo .env no encontrado en payment-service" -ForegroundColor $warningColor
}

Write-Host "`n========================================`n" -ForegroundColor Cyan
