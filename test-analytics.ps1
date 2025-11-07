# =====================================================
# SCRIPT DE TESTING - ANALYTICS SERVICE
# =====================================================
# Descripción: Automatiza las pruebas del Analytics Service
# Autor: David Delfino & Agustín Luparia
# Fecha: 2025-11-07
# =====================================================

param(
    [Parameter(HelpMessage="Email del usuario para autenticación")]
    [string]$UserEmail = "admin@example.com",
    
    [Parameter(HelpMessage="Password del usuario")]
    [string]$UserPassword = "admin123",
    
    [Parameter(HelpMessage="URL del Auth Service")]
    [string]$AuthServiceUrl = "http://localhost:8081",
    
    [Parameter(HelpMessage="URL del Analytics Service")]
    [string]$AnalyticsServiceUrl = "http://localhost:8087",
    
    [Parameter(HelpMessage="Ejecutar solo test específico")]
    [ValidateSet('all', 'health', 'auth', 'dashboard', 'metrics', 'performance')]
    [string]$TestType = 'all'
)

$ErrorActionPreference = "Stop"

Write-Host "╔═══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║       PACKEDGO - ANALYTICS SERVICE TESTING            ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# =====================================================
# FUNCIONES AUXILIARES
# =====================================================

function Write-TestStep {
    param([string]$Message)
    Write-Host ""
    Write-Host "🧪 TEST: $Message" -ForegroundColor Magenta
    Write-Host ("─" * 60) -ForegroundColor DarkGray
}

function Write-Success {
    param([string]$Message)
    Write-Host "  ✓ $Message" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    Write-Host "  ✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "  ℹ $Message" -ForegroundColor Gray
}

function Write-TestResult {
    param(
        [bool]$Success,
        [string]$Message,
        [object]$Data = $null
    )
    
    if ($Success) {
        Write-Success $Message
        if ($Data) {
            Write-Host "    Datos: $($Data | ConvertTo-Json -Depth 2 -Compress)" -ForegroundColor DarkGray
        }
    } else {
        Write-Fail $Message
        if ($Data) {
            Write-Host "    Error: $Data" -ForegroundColor DarkRed
        }
    }
}

# Variables globales para resultados
$global:TestResults = @()
$global:TotalTests = 0
$global:PassedTests = 0
$global:FailedTests = 0

function Add-TestResult {
    param(
        [string]$TestName,
        [bool]$Success,
        [string]$Message,
        [object]$Details = $null
    )
    
    $global:TotalTests++
    
    if ($Success) {
        $global:PassedTests++
    } else {
        $global:FailedTests++
    }
    
    $global:TestResults += @{
        Test = $TestName
        Success = $Success
        Message = $Message
        Details = $Details
        Timestamp = Get-Date
    }
}

# =====================================================
# TEST 1: HEALTH CHECK
# =====================================================

function Test-HealthCheck {
    Write-TestStep "Health Check"
    
    try {
        $url = "$AnalyticsServiceUrl/api/dashboard/health"
        Write-Info "URL: $url"
        
        $response = Invoke-RestMethod -Uri $url -Method GET -TimeoutSec 10
        
        if ($response -like "*UP*" -or $response -like "*running*") {
            Write-TestResult -Success $true -Message "Health check OK" -Data $response
            Add-TestResult -TestName "Health Check" -Success $true -Message "Service is UP"
            return $true
        } else {
            Write-TestResult -Success $false -Message "Health check devolvió respuesta inesperada" -Data $response
            Add-TestResult -TestName "Health Check" -Success $false -Message "Unexpected response"
            return $false
        }
        
    } catch {
        Write-TestResult -Success $false -Message "Error en health check" -Data $_.Exception.Message
        Add-TestResult -TestName "Health Check" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 2: AUTENTICACIÓN
# =====================================================

function Test-Authentication {
    Write-TestStep "Autenticación (Login)"
    
    try {
        $url = "$AuthServiceUrl/api/auth/login"
        Write-Info "URL: $url"
        Write-Info "User: $UserEmail"
        
        $body = @{
            email = $UserEmail
            password = $UserPassword
        } | ConvertTo-Json
        
        $response = Invoke-RestMethod `
            -Uri $url `
            -Method POST `
            -ContentType "application/json" `
            -Body $body `
            -TimeoutSec 10
        
        if ($response.access_token) {
            $script:Token = $response.access_token
            $script:UserId = $response.user_id
            $script:UserRole = $response.role
            
            Write-TestResult -Success $true -Message "Login exitoso"
            Write-Info "User ID: $script:UserId"
            Write-Info "Role: $script:UserRole"
            Write-Info "Token: $($script:Token.Substring(0, 20))..."
            
            Add-TestResult -TestName "Authentication" -Success $true -Message "Login successful" -Details @{
                UserId = $script:UserId
                Role = $script:UserRole
            }
            return $true
        } else {
            Write-TestResult -Success $false -Message "No se recibió token"
            Add-TestResult -TestName "Authentication" -Success $false -Message "No token received"
            return $false
        }
        
    } catch {
        Write-TestResult -Success $false -Message "Error en login" -Data $_.Exception.Message
        Add-TestResult -TestName "Authentication" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 3: DASHBOARD - SIN AUTENTICACIÓN (debe fallar)
# =====================================================

function Test-DashboardWithoutAuth {
    Write-TestStep "Dashboard sin autenticación (debe retornar 401)"
    
    try {
        $url = "$AnalyticsServiceUrl/api/dashboard"
        Write-Info "URL: $url"
        
        $response = Invoke-RestMethod -Uri $url -Method GET -TimeoutSec 10
        
        # Si llega acá, es un error (debería dar 401)
        Write-TestResult -Success $false -Message "Dashboard accesible sin autenticación (ERROR DE SEGURIDAD)"
        Add-TestResult -TestName "Dashboard Without Auth" -Success $false -Message "Security error: No authentication required"
        return $false
        
    } catch {
        if ($_.Exception.Response.StatusCode -eq 401) {
            Write-TestResult -Success $true -Message "401 Unauthorized recibido correctamente"
            Add-TestResult -TestName "Dashboard Without Auth" -Success $true -Message "401 Unauthorized as expected"
            return $true
        } else {
            Write-TestResult -Success $false -Message "Error inesperado: $($_.Exception.Message)"
            Add-TestResult -TestName "Dashboard Without Auth" -Success $false -Message $_.Exception.Message
            return $false
        }
    }
}

# =====================================================
# TEST 4: DASHBOARD - CON AUTENTICACIÓN
# =====================================================

function Test-DashboardWithAuth {
    Write-TestStep "Dashboard con autenticación"
    
    if (-not $script:Token) {
        Write-TestResult -Success $false -Message "No hay token disponible (ejecutar test de autenticación primero)"
        Add-TestResult -TestName "Dashboard With Auth" -Success $false -Message "No token available"
        return $false
    }
    
    try {
        $url = "$AnalyticsServiceUrl/api/dashboard"
        Write-Info "URL: $url"
        
        $headers = @{
            "Authorization" = "Bearer $script:Token"
        }
        
        $response = Invoke-RestMethod `
            -Uri $url `
            -Method GET `
            -Headers $headers `
            -TimeoutSec 20
        
        Write-TestResult -Success $true -Message "Dashboard obtenido exitosamente"
        
        # Guardar respuesta para otros tests
        $script:DashboardData = $response
        
        # Mostrar resumen
        Write-Info "Organizer ID: $($response.organizerId)"
        Write-Info "Organizer Name: $($response.organizerName)"
        Write-Info "Last Updated: $($response.lastUpdated)"
        
        Add-TestResult -TestName "Dashboard With Auth" -Success $true -Message "Dashboard retrieved" -Details @{
            OrganizerId = $response.organizerId
            OrganizerName = $response.organizerName
        }
        
        return $true
        
    } catch {
        Write-TestResult -Success $false -Message "Error al obtener dashboard" -Data $_.Exception.Message
        Add-TestResult -TestName "Dashboard With Auth" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 5: MÉTRICAS DE VENTAS
# =====================================================

function Test-SalesMetrics {
    Write-TestStep "Métricas de Ventas"
    
    if (-not $script:DashboardData) {
        Write-TestResult -Success $false -Message "No hay datos de dashboard (ejecutar test anterior primero)"
        Add-TestResult -TestName "Sales Metrics" -Success $false -Message "No dashboard data"
        return $false
    }
    
    try {
        $salesMetrics = $script:DashboardData.salesMetrics
        
        if ($salesMetrics) {
            Write-TestResult -Success $true -Message "Sales Metrics presentes"
            
            Write-Info "Total Tickets Sold: $($salesMetrics.totalTicketsSold)"
            Write-Info "Total Orders: $($salesMetrics.totalOrders)"
            Write-Info "Average Tickets per Order: $($salesMetrics.averageTicketsPerOrder)"
            Write-Info "Conversion Rate: $($salesMetrics.conversionRate)%"
            
            # Validar que los valores sean consistentes
            $valid = $true
            $errors = @()
            
            if ($salesMetrics.totalTicketsSold -lt 0) {
                $valid = $false
                $errors += "Total tickets sold no puede ser negativo"
            }
            
            if ($salesMetrics.totalOrders -lt 0) {
                $valid = $false
                $errors += "Total orders no puede ser negativo"
            }
            
            if ($salesMetrics.conversionRate -lt 0 -or $salesMetrics.conversionRate -gt 100) {
                $valid = $false
                $errors += "Conversion rate debe estar entre 0-100"
            }
            
            if ($valid) {
                Write-Success "Validación de datos OK"
                Add-TestResult -TestName "Sales Metrics" -Success $true -Message "Metrics valid" -Details $salesMetrics
            } else {
                Write-Fail "Errores de validación:"
                $errors | ForEach-Object { Write-Info $_ }
                Add-TestResult -TestName "Sales Metrics" -Success $false -Message "Validation errors" -Details $errors
            }
            
            return $valid
        } else {
            Write-TestResult -Success $false -Message "Sales Metrics no encontrado en respuesta"
            Add-TestResult -TestName "Sales Metrics" -Success $false -Message "Metrics not found"
            return $false
        }
        
    } catch {
        Write-TestResult -Success $false -Message "Error al validar Sales Metrics" -Data $_.Exception.Message
        Add-TestResult -TestName "Sales Metrics" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 6: MÉTRICAS DE EVENTOS
# =====================================================

function Test-EventMetrics {
    Write-TestStep "Métricas de Eventos"
    
    if (-not $script:DashboardData) {
        Write-TestResult -Success $false -Message "No hay datos de dashboard"
        Add-TestResult -TestName "Event Metrics" -Success $false -Message "No dashboard data"
        return $false
    }
    
    try {
        $eventMetrics = $script:DashboardData.eventMetrics
        
        if ($eventMetrics) {
            Write-TestResult -Success $true -Message "Event Metrics presentes"
            
            Write-Info "Total Events: $($eventMetrics.totalEvents)"
            Write-Info "Active Events: $($eventMetrics.activeEvents)"
            Write-Info "Past Events: $($eventMetrics.pastEvents)"
            Write-Info "Total Capacity: $($eventMetrics.totalCapacity)"
            Write-Info "Average Occupancy: $($eventMetrics.averageOccupancyRate)%"
            
            Add-TestResult -TestName "Event Metrics" -Success $true -Message "Metrics valid" -Details $eventMetrics
            return $true
        } else {
            Write-TestResult -Success $false -Message "Event Metrics no encontrado"
            Add-TestResult -TestName "Event Metrics" -Success $false -Message "Metrics not found"
            return $false
        }
        
    } catch {
        Write-TestResult -Success $false -Message "Error al validar Event Metrics" -Data $_.Exception.Message
        Add-TestResult -TestName "Event Metrics" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 7: MÉTRICAS DE REVENUE
# =====================================================

function Test-RevenueMetrics {
    Write-TestStep "Métricas de Revenue"
    
    if (-not $script:DashboardData) {
        Write-TestResult -Success $false -Message "No hay datos de dashboard"
        Add-TestResult -TestName "Revenue Metrics" -Success $false -Message "No dashboard data"
        return $false
    }
    
    try {
        $revenueMetrics = $script:DashboardData.revenueMetrics
        
        if ($revenueMetrics) {
            Write-TestResult -Success $true -Message "Revenue Metrics presentes"
            
            Write-Info "Total Revenue: $$$($revenueMetrics.totalRevenue)"
            Write-Info "Tickets Revenue: $$$($revenueMetrics.ticketsRevenue)"
            Write-Info "Consumptions Revenue: $$$($revenueMetrics.consumptionsRevenue)"
            Write-Info "This Month: $$$($revenueMetrics.thisMonthRevenue)"
            Write-Info "Last Month: $$$($revenueMetrics.lastMonthRevenue)"
            
            if ($revenueMetrics.growthRate) {
                Write-Info "Growth Rate: $($revenueMetrics.growthRate)%"
            }
            
            Add-TestResult -TestName "Revenue Metrics" -Success $true -Message "Metrics valid" -Details $revenueMetrics
            return $true
        } else {
            Write-TestResult -Success $false -Message "Revenue Metrics no encontrado"
            Add-TestResult -TestName "Revenue Metrics" -Success $false -Message "Metrics not found"
            return $false
        }
        
    } catch {
        Write-TestResult -Success $false -Message "Error al validar Revenue Metrics" -Data $_.Exception.Message
        Add-TestResult -TestName "Revenue Metrics" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 8: TOP PERFORMERS
# =====================================================

function Test-TopPerformers {
    Write-TestStep "Top Performers"
    
    if (-not $script:DashboardData) {
        Write-TestResult -Success $false -Message "No hay datos de dashboard"
        Add-TestResult -TestName "Top Performers" -Success $false -Message "No dashboard data"
        return $false
    }
    
    try {
        $topPerformers = $script:DashboardData.topPerformers
        
        if ($topPerformers) {
            Write-TestResult -Success $true -Message "Top Performers presentes"
            
            # Top Events
            if ($topPerformers.topEvents -and $topPerformers.topEvents.Count -gt 0) {
                Write-Info "Top Events:"
                $topPerformers.topEvents | ForEach-Object {
                    Write-Host "      - $($_.eventName): $($_.ticketsSold) tickets" -ForegroundColor Gray
                }
            } else {
                Write-Info "No hay eventos en top performers"
            }
            
            # Top Consumptions
            if ($topPerformers.topConsumptions -and $topPerformers.topConsumptions.Count -gt 0) {
                Write-Info "Top Consumptions:"
                $topPerformers.topConsumptions | ForEach-Object {
                    Write-Host "      - $($_.consumptionName): $($_.totalSold) vendidos" -ForegroundColor Gray
                }
            } else {
                Write-Info "No hay consumibles en top performers"
            }
            
            Add-TestResult -TestName "Top Performers" -Success $true -Message "Data retrieved" -Details $topPerformers
            return $true
        } else {
            Write-TestResult -Success $false -Message "Top Performers no encontrado"
            Add-TestResult -TestName "Top Performers" -Success $false -Message "Data not found"
            return $false
        }
        
    } catch {
        Write-TestResult -Success $false -Message "Error al validar Top Performers" -Data $_.Exception.Message
        Add-TestResult -TestName "Top Performers" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 9: TRENDS
# =====================================================

function Test-Trends {
    Write-TestStep "Trends (Tendencias)"
    
    if (-not $script:DashboardData) {
        Write-TestResult -Success $false -Message "No hay datos de dashboard"
        Add-TestResult -TestName "Trends" -Success $false -Message "No dashboard data"
        return $false
    }
    
    try {
        $trends = $script:DashboardData.trends
        
        if ($trends) {
            Write-TestResult -Success $true -Message "Trends presentes"
            
            # Daily Trends
            if ($trends.dailyTrends) {
                Write-Info "Daily Trends: $($trends.dailyTrends.Count) días"
            }
            
            # Monthly Trends
            if ($trends.monthlyTrends) {
                Write-Info "Monthly Trends: $($trends.monthlyTrends.Count) meses"
            }
            
            Add-TestResult -TestName "Trends" -Success $true -Message "Data retrieved" -Details $trends
            return $true
        } else {
            Write-TestResult -Success $false -Message "Trends no encontrado"
            Add-TestResult -TestName "Trends" -Success $false -Message "Data not found"
            return $false
        }
        
    } catch {
        Write-TestResult -Success $false -Message "Error al validar Trends" -Data $_.Exception.Message
        Add-TestResult -TestName "Trends" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# TEST 10: PERFORMANCE (Tiempo de respuesta)
# =====================================================

function Test-Performance {
    Write-TestStep "Performance (Tiempo de Respuesta)"
    
    if (-not $script:Token) {
        Write-TestResult -Success $false -Message "No hay token disponible"
        Add-TestResult -TestName "Performance" -Success $false -Message "No token"
        return $false
    }
    
    try {
        $url = "$AnalyticsServiceUrl/api/dashboard"
        $headers = @{
            "Authorization" = "Bearer $script:Token"
        }
        
        Write-Info "Realizando 5 requests..."
        $times = @()
        
        for ($i = 1; $i -le 5; $i++) {
            $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
            
            $response = Invoke-RestMethod `
                -Uri $url `
                -Method GET `
                -Headers $headers `
                -TimeoutSec 30
            
            $stopwatch.Stop()
            $times += $stopwatch.ElapsedMilliseconds
            
            Write-Info "Request $i: $($stopwatch.ElapsedMilliseconds) ms"
        }
        
        $avgTime = ($times | Measure-Object -Average).Average
        $minTime = ($times | Measure-Object -Minimum).Minimum
        $maxTime = ($times | Measure-Object -Maximum).Maximum
        
        Write-Info "Promedio: $([Math]::Round($avgTime, 2)) ms"
        Write-Info "Mínimo: $minTime ms"
        Write-Info "Máximo: $maxTime ms"
        
        # Considerar exitoso si promedio < 5000ms
        $success = $avgTime -lt 5000
        
        if ($success) {
            Write-TestResult -Success $true -Message "Performance aceptable"
        } else {
            Write-TestResult -Success $false -Message "Performance lenta (promedio > 5 segundos)"
        }
        
        Add-TestResult -TestName "Performance" -Success $success -Message "Avg: $([Math]::Round($avgTime, 2))ms" -Details @{
            Average = $avgTime
            Min = $minTime
            Max = $maxTime
        }
        
        return $success
        
    } catch {
        Write-TestResult -Success $false -Message "Error en test de performance" -Data $_.Exception.Message
        Add-TestResult -TestName "Performance" -Success $false -Message $_.Exception.Message
        return $false
    }
}

# =====================================================
# EJECUTAR TESTS
# =====================================================

Write-Info "Configuración:"
Write-Info "  Auth Service:      $AuthServiceUrl"
Write-Info "  Analytics Service: $AnalyticsServiceUrl"
Write-Info "  User:              $UserEmail"
Write-Info "  Test Type:         $TestType"
Write-Host ""

$startTime = Get-Date

# Ejecutar tests según el tipo
switch ($TestType) {
    'all' {
        Test-HealthCheck
        Test-Authentication
        Test-DashboardWithoutAuth
        Test-DashboardWithAuth
        Test-SalesMetrics
        Test-EventMetrics
        Test-RevenueMetrics
        Test-TopPerformers
        Test-Trends
        Test-Performance
    }
    'health' {
        Test-HealthCheck
    }
    'auth' {
        Test-Authentication
        Test-DashboardWithoutAuth
    }
    'dashboard' {
        Test-Authentication
        Test-DashboardWithAuth
    }
    'metrics' {
        Test-Authentication
        Test-DashboardWithAuth
        Test-SalesMetrics
        Test-EventMetrics
        Test-RevenueMetrics
        Test-TopPerformers
        Test-Trends
    }
    'performance' {
        Test-Authentication
        Test-Performance
    }
}

$endTime = Get-Date
$duration = ($endTime - $startTime).TotalSeconds

# =====================================================
# RESUMEN FINAL
# =====================================================

Write-Host ""
Write-Host "╔═══════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                  TEST RESULTS                         ║" -ForegroundColor Cyan
Write-Host "╚═══════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

Write-Host "Total Tests:   $global:TotalTests" -ForegroundColor White
Write-Host "Passed:        $global:PassedTests" -ForegroundColor Green
Write-Host "Failed:        $global:FailedTests" -ForegroundColor Red
Write-Host "Duration:      $([Math]::Round($duration, 2)) seconds" -ForegroundColor Gray

$successRate = if ($global:TotalTests -gt 0) { 
    [Math]::Round(($global:PassedTests / $global:TotalTests) * 100, 2) 
} else { 
    0 
}

Write-Host "Success Rate:  $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 50) { "Yellow" } else { "Red" })

Write-Host ""

if ($global:FailedTests -gt 0) {
    Write-Host "Failed Tests:" -ForegroundColor Red
    $global:TestResults | Where-Object { -not $_.Success } | ForEach-Object {
        Write-Host "  - $($_.Test): $($_.Message)" -ForegroundColor DarkRed
    }
    Write-Host ""
}

# Exportar resultados a JSON
$resultsPath = "test-results-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
$global:TestResults | ConvertTo-Json -Depth 5 | Out-File $resultsPath
Write-Info "Resultados guardados en: $resultsPath"

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════" -ForegroundColor Cyan

# Exit code
if ($global:FailedTests -eq 0) {
    exit 0
} else {
    exit 1
}
