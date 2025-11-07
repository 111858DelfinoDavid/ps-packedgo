# =====================================================
# DIAGNÓSTICO COMPLETO - PROBLEMA DE PERFIL CUSTOMER
# =====================================================

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "🔍 DIAGNÓSTICO: Problema de Perfil Customer" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Verificar servicios corriendo
Write-Host "`n📊 PASO 1: Verificando servicios..." -ForegroundColor Yellow

$services = @(
    @{Name="auth-service"; Port=8081; Endpoint="/auth/health"},
    @{Name="users-service"; Port=8082; Endpoint="/api/user-profiles/active"}
)

$allServicesOk = $true

foreach ($service in $services) {
    $url = "http://localhost:$($service.Port)$($service.Endpoint)"
    Write-Host "  Probando $($service.Name) en puerto $($service.Port)..." -NoNewline
    
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 3 -ErrorAction Stop
        if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 404) {
            Write-Host " ✅ CORRIENDO" -ForegroundColor Green
        } else {
            Write-Host " ⚠️ Responde pero con código $($response.StatusCode)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host " ❌ NO RESPONDE" -ForegroundColor Red
        $allServicesOk = $false
    }
}

# 2. Verificar si existe el token en el navegador (simulado)
Write-Host "`n📊 PASO 2: Verificar autenticación..." -ForegroundColor Yellow
Write-Host "  Abre la consola del navegador (F12) y ejecuta:" -ForegroundColor Cyan
Write-Host "    localStorage.getItem('userToken')" -ForegroundColor Gray
Write-Host "  Debe devolver un token JWT (no null)" -ForegroundColor Gray

# 3. Test directo al endpoint de perfil
Write-Host "`n📊 PASO 3: Test de endpoints..." -ForegroundColor Yellow

if ($allServicesOk) {
    Write-Host "  Ingresa tu userId (del JWT):" -NoNewline -ForegroundColor Cyan
    $userId = Read-Host
    
    if ($userId) {
        Write-Host "`n  Ingresa tu token JWT (completo):" -NoNewline -ForegroundColor Cyan
        $token = Read-Host
        
        if ($token) {
            # Test auth-service
            Write-Host "`n  🔐 Probando auth-service..." -ForegroundColor Yellow
            $authUrl = "http://localhost:8081/auth/user/$userId"
            Write-Host "  URL: $authUrl" -ForegroundColor Gray
            
            try {
                $headers = @{
                    "Authorization" = "Bearer $token"
                    "Content-Type" = "application/json"
                }
                $authResponse = Invoke-RestMethod -Uri $authUrl -Method GET -Headers $headers -ErrorAction Stop
                Write-Host "  ✅ auth-service responde correctamente" -ForegroundColor Green
                Write-Host "  Datos recibidos: username=$($authResponse.data.username), email=$($authResponse.data.email)" -ForegroundColor Gray
            } catch {
                Write-Host "  ❌ Error en auth-service: $($_.Exception.Message)" -ForegroundColor Red
            }
            
            # Test users-service
            Write-Host "`n  👤 Probando users-service..." -ForegroundColor Yellow
            $usersUrl = "http://localhost:8082/api/user-profiles/active/by-auth-user/$userId"
            Write-Host "  URL: $usersUrl" -ForegroundColor Gray
            
            try {
                $usersResponse = Invoke-RestMethod -Uri $usersUrl -Method GET -Headers $headers -ErrorAction Stop
                Write-Host "  ✅ users-service responde correctamente" -ForegroundColor Green
                Write-Host "  Perfil encontrado: name=$($usersResponse.name), lastName=$($usersResponse.lastName)" -ForegroundColor Gray
            } catch {
                if ($_.Exception.Response.StatusCode.Value__ -eq 404) {
                    Write-Host "  ⚠️ PERFIL NO ENCONTRADO (404)" -ForegroundColor Yellow
                    Write-Host "  Este es el problema: el perfil no se creó durante el registro" -ForegroundColor Yellow
                    Write-Host "`n  💡 SOLUCIÓN: Necesitas crear el perfil manualmente" -ForegroundColor Cyan
                } else {
                    Write-Host "  ❌ Error en users-service: $($_.Exception.Message)" -ForegroundColor Red
                }
            }
        }
    }
}

# 4. Verificar base de datos (opcional)
Write-Host "`n📊 PASO 4: Verificar base de datos (opcional)..." -ForegroundColor Yellow
Write-Host "  Conectar a PostgreSQL:" -ForegroundColor Cyan
Write-Host "    psql -U postgres -h localhost" -ForegroundColor Gray
Write-Host "    \c users_db" -ForegroundColor Gray
Write-Host "    SELECT * FROM user_profile WHERE auth_user_id = <tu_userId>;" -ForegroundColor Gray

# 5. Soluciones propuestas
Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "💡 SOLUCIONES PROPUESTAS" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

if (-not $allServicesOk) {
    Write-Host "`n❌ PROBLEMA: Servicios no están corriendo" -ForegroundColor Red
    Write-Host "  SOLUCIÓN 1: Inicia los servicios con Docker:" -ForegroundColor Yellow
    Write-Host "    cd packedgo\back" -ForegroundColor Gray
    Write-Host "    docker-compose up auth-service users-service" -ForegroundColor Gray
    Write-Host "`n  SOLUCIÓN 2: Inicia manualmente sin Docker:" -ForegroundColor Yellow
    Write-Host "    Terminal 1: cd packedgo\back\auth-service && .\mvnw spring-boot:run" -ForegroundColor Gray
    Write-Host "    Terminal 2: cd packedgo\back\users-service && .\mvnw spring-boot:run" -ForegroundColor Gray
} else {
    Write-Host "`n✅ Los servicios están corriendo correctamente" -ForegroundColor Green
    Write-Host "`n  Si el perfil no existe (404), las opciones son:" -ForegroundColor Yellow
    Write-Host "    1. Registrar un nuevo usuario (el perfil se crea automáticamente)" -ForegroundColor Gray
    Write-Host "    2. Crear el perfil manualmente vía API POST:" -ForegroundColor Gray
    Write-Host "       POST http://localhost:8082/api/user-profiles/from-auth" -ForegroundColor Gray
    Write-Host "       Body: { authUserId, document, name, lastName, bornDate, telephone, gender }" -ForegroundColor Gray
}

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "📝 LOGS ÚTILES" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  Logs de auth-service:" -ForegroundColor Yellow
Write-Host "    docker logs auth-service -f --tail=50" -ForegroundColor Gray
Write-Host "  Logs de users-service:" -ForegroundColor Yellow
Write-Host "    docker logs users-service -f --tail=50" -ForegroundColor Gray

Write-Host "`n✅ Diagnóstico completado`n" -ForegroundColor Green
