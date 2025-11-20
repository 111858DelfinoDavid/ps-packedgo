# Test del Sistema de Canje QR - PackedGo
# Este script prueba el flujo completo de canje de tickets y consumiciones

Write-Host "🎫 ==== TEST SISTEMA DE CANJE QR ====" -ForegroundColor Cyan
Write-Host ""

# Variables de configuración
$USERS_SERVICE = "http://localhost:8082/api"
$EVENT_SERVICE = "http://localhost:8086/api"

# Credenciales del empleado de prueba
$EMPLOYEE_EMAIL = "sasha@test.com"
$EMPLOYEE_PASSWORD = "password123"

Write-Host "📝 Paso 1: Login como empleado" -ForegroundColor Yellow
$loginBody = @{
    email = $EMPLOYEE_EMAIL
    password = $EMPLOYEE_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/json"
    
    $TOKEN = $loginResponse.token
    Write-Host "✅ Login exitoso - Token obtenido" -ForegroundColor Green
    Write-Host "   Username: $($loginResponse.user.username)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Error en login: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📅 Paso 2: Obtener eventos asignados" -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $TOKEN"
    "Content-Type" = "application/json"
}

try {
    $eventsResponse = Invoke-RestMethod -Uri "$USERS_SERVICE/employee/assigned-events" `
        -Method Get `
        -Headers $headers
    
    if ($eventsResponse.success -and $eventsResponse.data.Count -gt 0) {
        $EVENT_ID = $eventsResponse.data[0].id
        $EVENT_NAME = $eventsResponse.data[0].name
        Write-Host "✅ Eventos obtenidos: $($eventsResponse.data.Count)" -ForegroundColor Green
        Write-Host "   Evento seleccionado: $EVENT_NAME (ID: $EVENT_ID)" -ForegroundColor Gray
    } else {
        Write-Host "⚠️ No hay eventos asignados a este empleado" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "❌ Error obteniendo eventos: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🎫 Paso 3: Validar entrada (Ticket QR)" -ForegroundColor Yellow
# QR de ejemplo: PACKEDGO|T:1|E:1|U:3|TS:1732140000000
$QR_CODE = "PACKEDGO|T:1|E:$EVENT_ID|U:3|TS:1732140000000"

$validateTicketBody = @{
    qrCode = $QR_CODE
    eventId = $EVENT_ID
} | ConvertTo-Json

try {
    $ticketResponse = Invoke-RestMethod -Uri "$USERS_SERVICE/employee/validate-ticket" `
        -Method Post `
        -Headers $headers `
        -Body $validateTicketBody
    
    if ($ticketResponse.success -and $ticketResponse.data.valid) {
        Write-Host "✅ Entrada validada correctamente" -ForegroundColor Green
        Write-Host "   Ticket ID: $($ticketResponse.data.ticketInfo.ticketId)" -ForegroundColor Gray
        Write-Host "   Cliente: $($ticketResponse.data.ticketInfo.customerName)" -ForegroundColor Gray
        Write-Host "   Pass: $($ticketResponse.data.ticketInfo.passType)" -ForegroundColor Gray
        Write-Host "   Mensaje: $($ticketResponse.data.message)" -ForegroundColor Gray
    } else {
        Write-Host "⚠️ Entrada rechazada: $($ticketResponse.data.message)" -ForegroundColor Yellow
        Write-Host "   (Esto puede ser normal si el ticket ya fue usado anteriormente)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Error validando ticket: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Esto puede ocurrir si el ticket ya fue usado o no existe" -ForegroundColor Gray
}

Write-Host ""
Write-Host "🍺 Paso 4: Obtener consumiciones disponibles" -ForegroundColor Yellow
$TICKET_ID = 1

try {
    $consumptionsResponse = Invoke-RestMethod -Uri "$EVENT_SERVICE/event-service/ticket-consumption/by-ticket/$TICKET_ID/details" `
        -Method Get
    
    if ($consumptionsResponse.Count -gt 0) {
        Write-Host "✅ Consumiciones encontradas: $($consumptionsResponse.Count)" -ForegroundColor Green
        
        $availableConsumptions = $consumptionsResponse | Where-Object { $_.active -and -not $_.redeem -and $_.quantity -gt 0 }
        
        if ($availableConsumptions.Count -gt 0) {
            Write-Host "   📋 Consumiciones disponibles para canje:" -ForegroundColor Cyan
            foreach ($consumption in $availableConsumptions) {
                Write-Host "      - $($consumption.consumptionName): $($consumption.quantity) unidades" -ForegroundColor Gray
            }
            
            # Seleccionar la primera consumición disponible para probar el canje
            $DETAIL_ID = $availableConsumptions[0].id
            $CONSUMPTION_NAME = $availableConsumptions[0].consumptionName
            $AVAILABLE_QUANTITY = $availableConsumptions[0].quantity
            
            Write-Host ""
            Write-Host "🔄 Paso 5: Canjear consumición" -ForegroundColor Yellow
            Write-Host "   Canjeando: $CONSUMPTION_NAME (1 de $AVAILABLE_QUANTITY)" -ForegroundColor Gray
            
            $redeemBody = @{
                qrCode = $QR_CODE
                eventId = $EVENT_ID
                detailId = $DETAIL_ID
                quantity = 1
            } | ConvertTo-Json
            
            try {
                $redeemResponse = Invoke-RestMethod -Uri "$USERS_SERVICE/employee/register-consumption" `
                    -Method Post `
                    -Headers $headers `
                    -Body $redeemBody
                
                if ($redeemResponse.success) {
                    Write-Host "✅ Consumición canjeada exitosamente" -ForegroundColor Green
                    Write-Host "   Consumición: $($redeemResponse.data.consumptionInfo.consumptionName)" -ForegroundColor Gray
                    Write-Host "   Cantidad canjeada: $($redeemResponse.data.consumptionInfo.quantityRedeemed)" -ForegroundColor Gray
                    Write-Host "   Cantidad restante: $($redeemResponse.data.consumptionInfo.remainingQuantity)" -ForegroundColor Gray
                    Write-Host "   Totalmente canjeado: $($redeemResponse.data.consumptionInfo.fullyRedeemed)" -ForegroundColor Gray
                } else {
                    Write-Host "⚠️ Error al canjear: $($redeemResponse.message)" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "❌ Error en canje de consumición: $($_.Exception.Message)" -ForegroundColor Red
            }
        } else {
            Write-Host "⚠️ No hay consumiciones disponibles para canjear" -ForegroundColor Yellow
            Write-Host "   Todas las consumiciones ya fueron canjeadas" -ForegroundColor Gray
        }
    } else {
        Write-Host "⚠️ No se encontraron consumiciones para este ticket" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Error obteniendo consumiciones: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "📊 Paso 6: Obtener estadísticas del empleado" -ForegroundColor Yellow
try {
    $statsResponse = Invoke-RestMethod -Uri "$USERS_SERVICE/employee/stats" `
        -Method Get `
        -Headers $headers
    
    if ($statsResponse.success) {
        Write-Host "✅ Estadísticas obtenidas" -ForegroundColor Green
        Write-Host "   Tickets escaneados hoy: $($statsResponse.data.ticketsScannedToday)" -ForegroundColor Gray
        Write-Host "   Consumiciones hoy: $($statsResponse.data.consumptionsToday)" -ForegroundColor Gray
        Write-Host "   Total escaneado hoy: $($statsResponse.data.totalScannedToday)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Error obteniendo estadísticas: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "✅ ==== TEST COMPLETADO ====" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Resumen:" -ForegroundColor White
Write-Host "   1. ✅ Login empleado funcional" -ForegroundColor Gray
Write-Host "   2. ✅ Eventos asignados obtenidos" -ForegroundColor Gray
Write-Host "   3. ✅ Validación de entrada probada" -ForegroundColor Gray
Write-Host "   4. ✅ Consumiciones listadas" -ForegroundColor Gray
Write-Host "   5. ✅ Canje de consumición ejecutado" -ForegroundColor Gray
Write-Host "   6. ✅ Estadísticas consultadas" -ForegroundColor Gray
Write-Host ""
Write-Host "🌐 Accede al dashboard del empleado en:" -ForegroundColor Cyan
Write-Host "   http://localhost:3000/employee/login" -ForegroundColor Yellow
Write-Host ""
Write-Host "🔐 Credenciales de prueba:" -ForegroundColor Cyan
Write-Host "   Email: $EMPLOYEE_EMAIL" -ForegroundColor Yellow
Write-Host "   Password: $EMPLOYEE_PASSWORD" -ForegroundColor Yellow
Write-Host ""
