# ========================================
# TEST SCRIPT - CONSUMPTION ENDPOINTS
# ========================================
# Descripción: Script para probar endpoints de Consumption con JWT
# Requisitos: 
# - EVENT-SERVICE corriendo en http://localhost:8086
# - AUTH-SERVICE corriendo para generar JWT

param(
    [Parameter(Mandatory=$false)]
    [string]$JWT_TOKEN = "",
    
    [Parameter(Mandatory=$false)]
    [string]$BASE_URL = "http://localhost:8086/api/event-service/consumption"
)

# Colores para output
function Write-Success { Write-Host $args -ForegroundColor Green }
function Write-Error { Write-Host $args -ForegroundColor Red }
function Write-Info { Write-Host $args -ForegroundColor Cyan }

Write-Info "========================================="
Write-Info "🧪 TESTING CONSUMPTION ENDPOINTS"
Write-Info "========================================="

# Verificar JWT
if ($JWT_TOKEN -eq "") {
    Write-Error "❌ ERROR: JWT_TOKEN no proporcionado"
    Write-Info ""
    Write-Info "Uso: .\test-consumption-endpoints.ps1 -JWT_TOKEN '<tu_jwt_token>'"
    Write-Info ""
    Write-Info "Para obtener un JWT:"
    Write-Info "1. POST http://localhost:8081/api/auth-service/auth/login"
    Write-Info "2. Body: { ""email"": ""admin@example.com"", ""password"": ""password123"" }"
    Write-Info "3. Copiar el token de la respuesta"
    exit 1
}

Write-Success "✅ JWT Token proporcionado"
Write-Info "Base URL: $BASE_URL"
Write-Info ""

# Headers comunes
$headers = @{
    "Authorization" = "Bearer $JWT_TOKEN"
    "Content-Type" = "application/json"
}

# ========================================
# TEST 1: Listar todas las consumiciones
# ========================================
Write-Info "----------------------------------------"
Write-Info "TEST 1: GET /consumption (Listar todas)"
Write-Info "----------------------------------------"
try {
    $response = Invoke-RestMethod -Uri $BASE_URL -Method Get -Headers $headers
    Write-Success "✅ SUCCESS"
    Write-Info "Consumiciones encontradas: $($response.Count)"
    $response | ForEach-Object {
        Write-Info "  - ID: $($_.id) | Name: $($_.name) | CreatedBy: $($_.createdBy)"
    }
} catch {
    Write-Error "❌ FAILED: $($_.Exception.Message)"
}
Write-Info ""

# ========================================
# TEST 2: Crear una consumición
# ========================================
Write-Info "----------------------------------------"
Write-Info "TEST 2: POST /consumption (Crear)"
Write-Info "----------------------------------------"
$newConsumption = @{
    categoryId = 1
    name = "Test Consumption $(Get-Date -Format 'HHmmss')"
    description = "Consumición de prueba creada por script"
    price = 10.50
    imageUrl = "https://example.com/test.jpg"
    active = $true
} | ConvertTo-Json

Write-Info "Body:"
Write-Info $newConsumption

try {
    $response = Invoke-RestMethod -Uri $BASE_URL -Method Post -Headers $headers -Body $newConsumption
    Write-Success "✅ SUCCESS - Consumición creada"
    Write-Info "  - ID: $($response.id)"
    Write-Info "  - Name: $($response.name)"
    Write-Info "  - CreatedBy: $($response.createdBy)"
    $createdId = $response.id
} catch {
    Write-Error "❌ FAILED: $($_.Exception.Message)"
    $createdId = $null
}
Write-Info ""

# ========================================
# TEST 3: Actualizar la consumición
# ========================================
if ($createdId -ne $null) {
    Write-Info "----------------------------------------"
    Write-Info "TEST 3: PUT /consumption/$createdId (Actualizar)"
    Write-Info "----------------------------------------"
    $updatedConsumption = @{
        categoryId = 1
        name = "Test Consumption UPDATED"
        description = "Descripción actualizada"
        price = 15.99
        imageUrl = "https://example.com/updated.jpg"
        active = $true
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/$createdId" -Method Put -Headers $headers -Body $updatedConsumption
        Write-Success "✅ SUCCESS - Consumición actualizada"
        Write-Info "  - Name: $($response.name)"
        Write-Info "  - Price: $($response.price)"
    } catch {
        Write-Error "❌ FAILED: $($_.Exception.Message)"
    }
    Write-Info ""
}

# ========================================
# TEST 4: Desactivar la consumición
# ========================================
if ($createdId -ne $null) {
    Write-Info "----------------------------------------"
    Write-Info "TEST 4: DELETE /consumption/logical/$createdId (Desactivar)"
    Write-Info "----------------------------------------"
    try {
        $response = Invoke-RestMethod -Uri "$BASE_URL/logical/$createdId" -Method Delete -Headers $headers
        Write-Success "✅ SUCCESS - Consumición desactivada"
        Write-Info "  - Active: $($response.active)"
    } catch {
        Write-Error "❌ FAILED: $($_.Exception.Message)"
    }
    Write-Info ""
}

# ========================================
# TEST 5: Intentar acceder con JWT inválido
# ========================================
Write-Info "----------------------------------------"
Write-Info "TEST 5: GET /consumption (JWT inválido)"
Write-Info "----------------------------------------"
$invalidHeaders = @{
    "Authorization" = "Bearer invalid_token_12345"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri $BASE_URL -Method Get -Headers $invalidHeaders
    Write-Error "❌ UNEXPECTED: Debería haber fallado con JWT inválido"
} catch {
    Write-Success "✅ SUCCESS - JWT inválido rechazado correctamente"
    Write-Info "  - Error: $($_.Exception.Message)"
}
Write-Info ""

# ========================================
# RESUMEN
# ========================================
Write-Info "========================================="
Write-Info "🏁 TESTS COMPLETADOS"
Write-Info "========================================="
Write-Info "Revisa los resultados arriba para verificar la seguridad multi-tenant"
Write-Info ""
