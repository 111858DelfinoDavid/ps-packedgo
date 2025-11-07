# =====================================================
# SCRIPT DE DEPLOYMENT - ANALYTICS SERVICE
# =====================================================
# Descripción: Automatiza el proceso completo de verificación,
#              compilación y deployment del Analytics-Service
# Autor: David Delfino & Agustín Luparia
# Fecha: 2025-11-07
# =====================================================

param(
    [Parameter(HelpMessage="Modo de deployment: 'dev', 'docker' o 'prod'")]
    [ValidateSet('dev', 'docker', 'prod')]
    [string]$Mode = 'dev',
    
    [Parameter(HelpMessage="Crear base de datos si no existe")]
    [switch]$CreateDb,
    
    [Parameter(HelpMessage="Compilar sin ejecutar tests")]
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"
$baseDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "╔═══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║        PACKEDGO - ANALYTICS SERVICE DEPLOYMENT        ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# =====================================================
# FUNCIONES AUXILIARES
# =====================================================

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "► $Message" -ForegroundColor Yellow
    Write-Host ("─" * 60) -ForegroundColor DarkGray
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  $Message" -ForegroundColor Gray
}

function Test-Command {
    param([string]$Command)
    try {
        if (Get-Command $Command -ErrorAction SilentlyContinue) {
            return $true
        }
        return $false
    } catch {
        return $false
    }
}

function Test-Port {
    param([int]$Port)
    try {
        $connection = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue
        return $connection.TcpTestSucceeded
    } catch {
        return $false
    }
}

function Get-ServiceStatus {
    param([string]$ServiceName, [int]$Port, [string]$HealthUrl)
    
    $status = @{
        Name = $ServiceName
        Port = $Port
        Running = $false
        Health = "Unknown"
    }
    
    # Verificar puerto
    if (Test-Port -Port $Port) {
        $status.Running = $true
        
        # Verificar health endpoint
        if ($HealthUrl) {
            try {
                $response = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 5 -ErrorAction SilentlyContinue
                $status.Health = "UP"
            } catch {
                $status.Health = "DOWN"
            }
        }
    }
    
    return $status
}

# =====================================================
# PASO 1: PRE-REQUISITOS
# =====================================================

Write-Step "PASO 1: Verificando Pre-requisitos"

# Verificar Java
Write-Info "Verificando Java..."
if (-not (Test-Command "java")) {
    Write-Error "Java no está instalado"
    Write-Info "Instalar Java 17 desde: https://adoptium.net/"
    exit 1
}

$javaVersion = (java -version 2>&1)[0]
Write-Success "Java encontrado: $javaVersion"

# Verificar Maven
Write-Info "Verificando Maven..."
$mvnwPath = Join-Path $baseDir "packedgo\back\analytics-service\mvnw.cmd"
if (Test-Path $mvnwPath) {
    Write-Success "Maven Wrapper encontrado"
} else {
    Write-Error "Maven Wrapper no encontrado en: $mvnwPath"
    exit 1
}

# Verificar PostgreSQL
Write-Info "Verificando PostgreSQL..."
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if ($pgService) {
    if ($pgService.Status -eq "Running") {
        Write-Success "PostgreSQL está corriendo"
    } else {
        Write-Error "PostgreSQL está instalado pero no está corriendo"
        Write-Info "Iniciar con: Start-Service $($pgService.Name)"
        exit 1
    }
} else {
    Write-Error "PostgreSQL no está instalado"
    exit 1
}

# Verificar Docker (si modo docker)
if ($Mode -eq 'docker') {
    Write-Info "Verificando Docker..."
    if (-not (Test-Command "docker")) {
        Write-Error "Docker no está instalado"
        Write-Info "Instalar desde: https://www.docker.com/products/docker-desktop"
        exit 1
    }
    
    $dockerVersion = docker --version
    Write-Success "Docker encontrado: $dockerVersion"
}

# =====================================================
# PASO 2: VERIFICAR SERVICIOS DEPENDIENTES
# =====================================================

Write-Step "PASO 2: Verificando Servicios Dependientes"

$services = @(
    @{Name="Auth Service"; Port=8081; Url="http://localhost:8081/api/auth/health"},
    @{Name="Users Service"; Port=8082; Url="http://localhost:8082/api/users/health"},
    @{Name="Event Service"; Port=8086; Url="http://localhost:8086/api/events/health"},
    @{Name="Order Service"; Port=8084; Url="http://localhost:8084/api/orders/health"},
    @{Name="Payment Service"; Port=8085; Url="http://localhost:8085/api/payments/health"}
)

$allServicesUp = $true

foreach ($svc in $services) {
    $status = Get-ServiceStatus -ServiceName $svc.Name -Port $svc.Port -HealthUrl $svc.Url
    
    if ($status.Running) {
        Write-Success "$($svc.Name) - Puerto $($svc.Port) [UP]"
    } else {
        Write-Error "$($svc.Name) - Puerto $($svc.Port) [DOWN]"
        $allServicesUp = $false
    }
}

if (-not $allServicesUp) {
    Write-Host ""
    Write-Host "⚠️  ADVERTENCIA: No todos los servicios están activos" -ForegroundColor Yellow
    Write-Host "   El Analytics Service necesita estos servicios para funcionar" -ForegroundColor Yellow
    Write-Host ""
    
    $continue = Read-Host "¿Continuar de todas formas? (s/n)"
    if ($continue -ne 's' -and $continue -ne 'S') {
        Write-Info "Deployment cancelado"
        exit 0
    }
}

# =====================================================
# PASO 3: VERIFICAR/CREAR BASE DE DATOS
# =====================================================

Write-Step "PASO 3: Verificando Base de Datos"

$dbExists = $false

try {
    # Intentar conectar a la base de datos
    $env:PGPASSWORD = "analytics_password"
    $result = psql -U analytics_user -h localhost -d analytics_db -c "SELECT 1;" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Base de datos 'analytics_db' ya existe"
        $dbExists = $true
    }
} catch {
    Write-Info "Base de datos 'analytics_db' no existe"
}

if (-not $dbExists -and $CreateDb) {
    Write-Info "Creando base de datos..."
    
    $createDbScript = @"
CREATE DATABASE analytics_db;
CREATE USER analytics_user WITH PASSWORD 'analytics_password';
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO analytics_user;
"@
    
    try {
        $env:PGPASSWORD = "postgres"
        $createDbScript | psql -U postgres -h localhost
        Write-Success "Base de datos creada exitosamente"
    } catch {
        Write-Error "Error al crear base de datos: $_"
        exit 1
    }
} elseif (-not $dbExists) {
    Write-Error "Base de datos no existe"
    Write-Info "Ejecutar nuevamente con -CreateDb para crearla"
    exit 1
}

# =====================================================
# PASO 4: VERIFICAR CONFIGURACIÓN
# =====================================================

Write-Step "PASO 4: Verificando Configuración"

$envPath = Join-Path $baseDir "packedgo\back\analytics-service\.env"

if (Test-Path $envPath) {
    Write-Success "Archivo .env encontrado"
    
    # Leer JWT_SECRET
    $envContent = Get-Content $envPath -Raw
    if ($envContent -match "JWT_SECRET=(.+)") {
        $analyticsJwtSecret = $matches[1].Trim()
        Write-Info "JWT_SECRET configurado (${analyticsJwtSecret.Substring(0,20)}...)"
        
        # Comparar con auth-service
        $authEnvPath = Join-Path $baseDir "packedgo\back\auth-service\.env"
        if (Test-Path $authEnvPath) {
            $authEnvContent = Get-Content $authEnvPath -Raw
            if ($authEnvContent -match "JWT_SECRET=(.+)") {
                $authJwtSecret = $matches[1].Trim()
                
                if ($analyticsJwtSecret -eq $authJwtSecret) {
                    Write-Success "JWT_SECRET coincide con auth-service ✓"
                } else {
                    Write-Error "JWT_SECRET NO coincide con auth-service"
                    Write-Info "Analytics: ${analyticsJwtSecret.Substring(0,20)}..."
                    Write-Info "Auth:      ${authJwtSecret.Substring(0,20)}..."
                    exit 1
                }
            }
        }
    }
} else {
    Write-Error "Archivo .env no encontrado"
    Write-Info "Crear en: $envPath"
    exit 1
}

# =====================================================
# PASO 5: COMPILACIÓN
# =====================================================

Write-Step "PASO 5: Compilando Analytics Service"

$analyticsPath = Join-Path $baseDir "packedgo\back\analytics-service"
Push-Location $analyticsPath

try {
    Write-Info "Limpiando compilaciones anteriores..."
    & .\mvnw.cmd clean | Out-Null
    
    Write-Info "Compilando proyecto..."
    
    if ($SkipTests) {
        & .\mvnw.cmd install -DskipTests
    } else {
        & .\mvnw.cmd install
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Compilación exitosa"
        
        $jarPath = Join-Path $analyticsPath "target\analytics-service-0.0.1-SNAPSHOT.jar"
        if (Test-Path $jarPath) {
            $jarSize = (Get-Item $jarPath).Length / 1MB
            Write-Info "JAR generado: $([Math]::Round($jarSize, 2)) MB"
        }
    } else {
        Write-Error "Error en la compilación"
        Pop-Location
        exit 1
    }
    
} catch {
    Write-Error "Error durante la compilación: $_"
    Pop-Location
    exit 1
} finally {
    Pop-Location
}

# =====================================================
# PASO 6: DEPLOYMENT
# =====================================================

Write-Step "PASO 6: Iniciando Deployment (Modo: $Mode)"

switch ($Mode) {
    'dev' {
        Write-Info "Modo Desarrollo - Iniciando con Maven..."
        
        Push-Location $analyticsPath
        
        Write-Host ""
        Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  ANALYTICS SERVICE - INICIANDO" -ForegroundColor Cyan
        Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  URL:    http://localhost:8087" -ForegroundColor White
        Write-Host "  Health: http://localhost:8087/api/dashboard/health" -ForegroundColor White
        Write-Host "  API:    http://localhost:8087/api/dashboard" -ForegroundColor White
        Write-Host ""
        Write-Host "  Presiona Ctrl+C para detener" -ForegroundColor Yellow
        Write-Host ""
        
        & .\mvnw.cmd spring-boot:run
        
        Pop-Location
    }
    
    'docker' {
        Write-Info "Modo Docker - Construyendo imagen..."
        
        Push-Location (Join-Path $baseDir "packedgo\back")
        
        try {
            # Construir imagen
            docker-compose build analytics-service
            
            if ($LASTEXITCODE -eq 0) {
                Write-Success "Imagen construida exitosamente"
                
                Write-Info "Iniciando contenedor..."
                docker-compose up analytics-service -d
                
                if ($LASTEXITCODE -eq 0) {
                    Write-Success "Contenedor iniciado"
                    
                    Write-Info "Esperando que el servicio esté listo..."
                    Start-Sleep -Seconds 10
                    
                    # Verificar logs
                    Write-Host ""
                    Write-Host "Últimas líneas del log:" -ForegroundColor Cyan
                    docker-compose logs --tail=20 analytics-service
                    
                    Write-Host ""
                    Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
                    Write-Host "  ANALYTICS SERVICE - DOCKER" -ForegroundColor Cyan
                    Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
                    Write-Host ""
                    Write-Host "  Container: analytics-service" -ForegroundColor White
                    Write-Host "  URL:       http://localhost:8087" -ForegroundColor White
                    Write-Host ""
                    Write-Host "  Comandos útiles:" -ForegroundColor Yellow
                    Write-Host "    docker-compose logs -f analytics-service  # Ver logs" -ForegroundColor Gray
                    Write-Host "    docker-compose stop analytics-service     # Detener" -ForegroundColor Gray
                    Write-Host "    docker-compose restart analytics-service  # Reiniciar" -ForegroundColor Gray
                    Write-Host ""
                } else {
                    Write-Error "Error al iniciar contenedor"
                }
            } else {
                Write-Error "Error al construir imagen"
            }
            
        } finally {
            Pop-Location
        }
    }
    
    'prod' {
        Write-Info "Modo Producción - Ejecutando JAR..."
        
        $jarPath = Join-Path $analyticsPath "target\analytics-service-0.0.1-SNAPSHOT.jar"
        
        if (Test-Path $jarPath) {
            Write-Host ""
            Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
            Write-Host "  ANALYTICS SERVICE - PRODUCCIÓN" -ForegroundColor Cyan
            Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
            Write-Host ""
            Write-Host "  JAR:    $jarPath" -ForegroundColor White
            Write-Host "  URL:    http://localhost:8087" -ForegroundColor White
            Write-Host ""
            Write-Host "  Presiona Ctrl+C para detener" -ForegroundColor Yellow
            Write-Host ""
            
            Push-Location $analyticsPath
            java -jar $jarPath
            Pop-Location
        } else {
            Write-Error "JAR no encontrado en: $jarPath"
            exit 1
        }
    }
}

# =====================================================
# PASO 7: VERIFICACIÓN POST-DEPLOYMENT
# =====================================================

if ($Mode -eq 'docker') {
    Write-Step "PASO 7: Verificando Deployment"
    
    Start-Sleep -Seconds 5
    
    try {
        $healthUrl = "http://localhost:8087/api/dashboard/health"
        $response = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 10
        
        Write-Success "Health check exitoso: $response"
        
        Write-Host ""
        Write-Host "═══════════════════════════════════════" -ForegroundColor Green
        Write-Host "  ✓ DEPLOYMENT COMPLETADO EXITOSAMENTE" -ForegroundColor Green
        Write-Host "═══════════════════════════════════════" -ForegroundColor Green
        Write-Host ""
        
    } catch {
        Write-Error "Health check falló: $_"
        Write-Info "Verificar logs con: docker-compose logs analytics-service"
    }
}

# =====================================================
# FIN DEL SCRIPT
# =====================================================
