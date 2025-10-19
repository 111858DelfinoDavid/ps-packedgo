# Script de prueba completo de endpoints - Event Service
$baseUrl = "http://localhost:8086/api/event-service"
$testResults = @()

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [string]$Body = $null
    )
    
    Write-Host "`n=== TEST: $Name ===" -ForegroundColor Cyan
    try {
        if ($Body) {
            $result = Invoke-RestMethod -Uri $Url -Method $Method -Body ([System.Text.Encoding]::UTF8.GetBytes($Body)) -ContentType "application/json; charset=utf-8" -ErrorAction Stop
        } else {
            $result = Invoke-RestMethod -Uri $Url -Method $Method -ErrorAction Stop
        }
        Write-Host "[OK] SUCCESS" -ForegroundColor Green
        $testResults += [PSCustomObject]@{
            Test = $Name
            Status = "PASS"
            Response = $result
        }
        return $result
    } catch {
        Write-Host "[X] FAILED: $_" -ForegroundColor Red
        $testResults += [PSCustomObject]@{
            Test = $Name
            Status = "FAIL"
            Error = $_.Exception.Message
        }
        return $null
    }
}

Write-Host "`n[*] INICIANDO PRUEBAS DE ENDPOINTS - EVENT SERVICE" -ForegroundColor Yellow
Write-Host "=" * 60

# 1. GET - Listar eventos
$events = Test-Endpoint -Name "GET /event - Listar todos los eventos" -Method Get -Url "$baseUrl/event"
if ($events) { Write-Host "Total eventos: $($events.Count)" }

# 2. GET - Obtener evento específico CON consumptions
$event = Test-Endpoint -Name "GET /event/6 - Obtener evento con consumptions" -Method Get -Url "$baseUrl/event/6"
if ($event -and $event.availableConsumptions) {
    Write-Host "  Consumptions disponibles: $($event.availableConsumptions.Count)" -ForegroundColor Magenta
    $event.availableConsumptions | ForEach-Object { Write-Host "    - $($_.name): `$$($_.price)" }
}

# 3. POST - Crear evento SIN consumptions
$bodyNoConsumptions = '{"name":"Evento sin consumos","categoryId":1,"eventDate":"2026-03-01T18:00:00","lat":-34.6,"lng":-58.4,"maxCapacity":200,"basePrice":2000}'
$eventNoConsumptions = Test-Endpoint -Name "POST /event - Crear evento SIN consumptions" -Method Post -Url "$baseUrl/event" -Body $bodyNoConsumptions
if ($eventNoConsumptions) { Write-Host "  ID creado: $($eventNoConsumptions.id)" }

# 4. GET - Categorías de consumo
$consumptionCategories = Test-Endpoint -Name "GET /consumption-category - Listar categorías de consumo" -Method Get -Url "$baseUrl/consumption-category"
if ($consumptionCategories) { Write-Host "Total categorías: $($consumptionCategories.Count)" }

# 5. GET - Listar consumptions
$consumptions = Test-Endpoint -Name "GET /consumption - Listar todos los consumptions" -Method Get -Url "$baseUrl/consumption"
if ($consumptions) {
    Write-Host "Total consumptions: $($consumptions.Count)" -ForegroundColor Magenta
    $consumptions | ForEach-Object { Write-Host "    - ID $($_.id): $($_.name) - `$$($_.price)" }
}

# 6. GET - Obtener consumption específico
if ($consumptions -and $consumptions.Count -gt 0) {
    $consumption = Test-Endpoint -Name "GET /consumption/1 - Obtener consumption específico" -Method Get -Url "$baseUrl/consumption/1"
    if ($consumption) { Write-Host "  Consumo: $($consumption.name)" }
}

# 7. POST - Crear nuevo consumption
$bodyNewConsumption = '{"name":"Vaso de Vino","description":"Vino tinto de la casa","price":2000,"stock":150,"categoryId":1}'
$newConsumption = Test-Endpoint -Name "POST /consumption - Crear nuevo consumption" -Method Post -Url "$baseUrl/consumption" -Body $bodyNewConsumption
if ($newConsumption) { Write-Host "  Nuevo consumption ID: $($newConsumption.id)" }

# 8. POST - Crear evento CON el nuevo consumption
if ($newConsumption) {
    $bodyEventWithNew = "{`"name`":`"Evento con nuevo consumo`",`"categoryId`":1,`"eventDate`":`"2026-04-15T20:00:00`",`"lat`":-34.62,`"lng`":-58.39,`"maxCapacity`":800,`"basePrice`":6000,`"consumptionIds`":[$($newConsumption.id)]}"
    $eventWithNew = Test-Endpoint -Name "POST /event - Crear evento CON nuevo consumption" -Method Post -Url "$baseUrl/event" -Body $bodyEventWithNew
    if ($eventWithNew -and $eventWithNew.availableConsumptions) {
        Write-Host "  Consumptions del evento:" -ForegroundColor Magenta
        $eventWithNew.availableConsumptions | ForEach-Object { Write-Host "    - $($_.name)" }
    }
}

# 9. GET - Categorías de eventos
$eventCategories = Test-Endpoint -Name "GET /category - Listar categorías de eventos" -Method Get -Url "$baseUrl/category"
if ($eventCategories) { Write-Host "Total categorías de eventos: $($eventCategories.Count)" }

# 10. GET - Eventos por categoría (usando query param si está disponible)
$eventsByCategory = Test-Endpoint -Name "GET /event - Obtener todos los eventos" -Method Get -Url "$baseUrl/event"
if ($eventsByCategory) {
    $musicEvents = $eventsByCategory | Where-Object { $_.categoryId -eq 1 }
    Write-Host "Eventos de categoría 1: $($musicEvents.Count)" -ForegroundColor Magenta
}

# 11. PUT - Actualizar consumption
if ($newConsumption) {
    $bodyUpdate = "{`"name`":`"Vaso de Vino Premium`",`"description`":`"Vino tinto reserva`",`"price`":2500,`"stock`":150,`"categoryId`":1}"
    $updated = Test-Endpoint -Name "PUT /consumption/$($newConsumption.id) - Actualizar consumption" -Method Put -Url "$baseUrl/consumption/$($newConsumption.id)" -Body $bodyUpdate
    if ($updated) { Write-Host "  Nuevo nombre: $($updated.name), Nuevo precio: `$$($updated.price)" }
}

# 12. DELETE Lógico - Consumption
if ($newConsumption) {
    $deleted = Test-Endpoint -Name "DELETE /consumption/logical/$($newConsumption.id) - Eliminación lógica" -Method Delete -Url "$baseUrl/consumption/logical/$($newConsumption.id)"
    if ($deleted) { Write-Host "  Estado active: $($deleted.active)" }
}

# 13. Verificar tabla event_consumptions en base de datos
Write-Host "`n=== VERIFICACION BASE DE DATOS ===" -ForegroundColor Yellow
try {
    $dbResult = docker exec -it back-event-db-1 psql -U event_user -d event_db -c "SELECT ec.event_id, e.name as event_name, ec.consumption_id, c.name as consumption_name FROM event_consumptions ec JOIN events e ON ec.event_id = e.id JOIN consumptions c ON ec.consumption_id = c.id;" 2>&1
    Write-Host $dbResult
} catch {
    Write-Host "Error al verificar base de datos" -ForegroundColor Red
}

# RESUMEN FINAL
Write-Host "`n" + ("=" * 60) -ForegroundColor Yellow
Write-Host "[*] RESUMEN DE PRUEBAS" -ForegroundColor Yellow
Write-Host ("=" * 60) -ForegroundColor Yellow
$passed = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$total = $testResults.Count

Write-Host "`n[OK] PASSED: $passed / $total" -ForegroundColor Green
Write-Host "[X] FAILED: $failed / $total" -ForegroundColor $(if($failed -gt 0){"Red"}else{"Green"})
Write-Host "`nTasa de exito: $([math]::Round(($passed/$total)*100, 2))%" -ForegroundColor Cyan

if ($failed -gt 0) {
    Write-Host "`nPruebas fallidas:" -ForegroundColor Red
    $testResults | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
        Write-Host "  - $($_.Test): $($_.Error)" -ForegroundColor Red
    }
}

Write-Host "`n[*] PRUEBAS COMPLETADAS" -ForegroundColor Yellow
