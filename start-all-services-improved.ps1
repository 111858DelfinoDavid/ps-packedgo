# 🚀 Script mejorado para iniciar todos los servicios de PackedGo

param(
    [switch]$WaitForServices = $false
)

Write-Host "🎯 INICIANDO TODOS LOS SERVICIOS DE PACKEDGO" -ForegroundColor Cyan
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""

# Verificar PostgreSQL
Write-Host "📦 Verificando PostgreSQL..." -ForegroundColor Yellow
try {
    $pgCheck = Test-NetConnection -ComputerName localhost -Port 5432 -WarningAction SilentlyContinue -ErrorAction SilentlyContinue -InformationLevel Quiet
    
    if ($pgCheck) {
        Write-Host "✅ PostgreSQL está corriendo" -ForegroundColor Green
    } else {
        Write-Host "❌ PostgreSQL no está corriendo" -ForegroundColor Red
        Write-Host "💡 Ejecuta: docker start postgres-packedgo" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "❌ No se pudo verificar PostgreSQL" -ForegroundColor Red
    exit 1
}
Write-Host ""

# Definir servicios
$services = @(
    @{
        Name = "Auth Service"
        Port = 8081
        Path = "C:\Users\david\Documents\ps-packedgo\packedgo\back\auth-service"
        Color = "Cyan"
    },
    @{
        Name = "Users Service"
        Port = 8082
        Path = "C:\Users\david\Documents\ps-packedgo\packedgo\back\users-service"
        Color = "Green"
    },
    @{
        Name = "Event Service"
        Port = 8086
        Path = "C:\Users\david\Documents\ps-packedgo\packedgo\back\event-service"
        Color = "Yellow"
    },
    @{
        Name = "Order Service"
        Port = 8084
        Path = "C:\Users\david\Documents\ps-packedgo\packedgo\back\order-service"
        Color = "Magenta"
    },
    @{
        Name = "Payment Service ⭐"
        Port = 8085
        Path = "C:\Users\david\Documents\ps-packedgo\packedgo\back\payment-service"
        Color = "Red"
    }
)

Write-Host "🚀 Iniciando servicios backend..." -ForegroundColor Cyan
Write-Host ""

foreach ($service in $services) {
    Write-Host "▶️  Iniciando $($service.Name) en puerto $($service.Port)..." -ForegroundColor $service.Color
    
    $title = "$($service.Name) - Puerto $($service.Port)"
    $command = "cd '$($service.Path)'; Write-Host '🔹 $title' -ForegroundColor $($service.Color); Write-Host ''; .\mvnw.cmd spring-boot:run"
    
    Start-Process pwsh -ArgumentList "-NoExit", "-Command", $command -WindowStyle Normal
    
    Start-Sleep -Seconds 3
}

Write-Host ""
Write-Host "🌐 Iniciando Angular Frontend..." -ForegroundColor Cyan
$frontendPath = "C:\Users\david\Documents\ps-packedgo\packedgo\front-angular"
$frontendCommand = "cd '$frontendPath'; Write-Host '🔹 Angular Frontend - Puerto 4200' -ForegroundColor Blue; Write-Host ''; ng serve"

Start-Process pwsh -ArgumentList "-NoExit", "-Command", $frontendCommand -WindowStyle Normal

Write-Host ""
Write-Host "✅ Todas las terminales han sido abiertas!" -ForegroundColor Green
Write-Host ""
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""
Write-Host "⏱️  Los servicios están arrancando. Esto puede tomar 1-2 minutos..." -ForegroundColor Yellow
Write-Host ""

if ($WaitForServices) {
    Write-Host "⏳ Esperando 90 segundos para que los servicios terminen de arrancar..." -ForegroundColor Yellow
    for ($i = 90; $i -gt 0; $i--) {
        Write-Host "`r⏱️  Tiempo restante: $i segundos  " -NoNewline -ForegroundColor Yellow
        Start-Sleep -Seconds 1
    }
    Write-Host ""
    Write-Host ""
    Write-Host "🔍 Verificando estado de los servicios..." -ForegroundColor Cyan
    Write-Host ""
    & ".\verify-services.ps1"
} else {
    Write-Host "📋 Para verificar el estado cuando terminen de arrancar:" -ForegroundColor Cyan
    Write-Host "   .\verify-services.ps1" -ForegroundColor Green
    Write-Host ""
    Write-Host "O ejecuta este script con el flag -WaitForServices:" -ForegroundColor Cyan
    Write-Host "   .\start-all-services-improved.ps1 -WaitForServices" -ForegroundColor Green
}

Write-Host ""
Write-Host "🎯 DESPUÉS DE QUE TODO ESTÉ CORRIENDO:" -ForegroundColor Cyan
Write-Host "1. Verifica el estado: .\verify-services.ps1" -ForegroundColor White
Write-Host "2. Abre http://localhost:4200 en tu navegador" -ForegroundColor White
Write-Host "3. Sigue las instrucciones de INICIAR_TODOS_LOS_SERVICIOS.md" -ForegroundColor White
Write-Host ""
Write-Host ("=" * 60) -ForegroundColor Gray
Write-Host ""
Write-Host "✨ ¡Listo! Las terminales están ejecutándose en segundo plano" -ForegroundColor Green
Write-Host ""
