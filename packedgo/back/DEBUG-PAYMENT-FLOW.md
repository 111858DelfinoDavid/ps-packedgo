# 🔍 DEBUG: Flujo Completo de Pago

## Problema Original
- **Síntoma**: Error 401 al crear preferencia de pago
- **Causa Root**: Conflicto entre SecurityConfig (permitAll) y PaymentController (requiere JWT)
- **Efecto Secundario**: Auth interceptor hace logout → loop infinito

## Solución Implementada
✅ Removida validación JWT del endpoint `/payments/create`
✅ Endpoint ahora es público (desarrollo/testing)
✅ Se valida que adminId esté presente en el request

## Checklist de Testing

### 1. Verificar que el servicio esté corriendo
```powershell
docker ps | Select-String "payment-service"
```

### 2. Ver logs en tiempo real
```powershell
docker logs -f back-payment-service-1
```

### 3. Probar endpoint directamente (con Postman o curl)
```powershell
$body = @{
    adminId = 9
    orderId = "TEST-12345"
    amount = 1000.00
    description = "Test Payment"
    payerEmail = "test@test.com"
    payerName = "Test User"
    externalReference = "TEST-12345"
    successUrl = "http://localhost:3000/customer/checkout?paymentStatus=success"
    failureUrl = "http://localhost:3000/customer/checkout?paymentStatus=failure"
    pendingUrl = "http://localhost:3000/customer/checkout?paymentStatus=pending"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8085/api/payments/create" `
    -Method Post `
    -Body $body `
    -ContentType "application/json"
```

### 4. Verificar frontend logs
- Abrir DevTools (F12)
- Ver tab Console
- Ver tab Network → filtrar por "payments"
- Verificar headers del request

### 5. Verificar auth interceptor
- Buscar en Console: `"Token being sent:"`
- Buscar en Console: `"POST /api/payments/create"`
- Verificar que NO aparezca: `"Token expirado o inválido"`

## Posibles Problemas Adicionales

### A. Token expirado
**Síntoma**: Login funciona pero checkout falla
**Solución**: 
```typescript
// En auth.service.ts, aumentar el tiempo de expiración
// O implementar refresh token
```

### B. CORS issues
**Síntoma**: Request bloqueado por política CORS
**Solución**: Verificar SecurityConfig.java → allowedOrigins

### C. MercadoPago credenciales inválidas
**Síntoma**: Pago se crea pero no redirige
**Solución**: Verificar que las credenciales en AdminCredential sean de SANDBOX

### D. Return URLs incorrectas
**Síntoma**: Pago exitoso pero no vuelve al checkout
**Solución**: Verificar que las URLs de retorno incluyan sessionId

## Alternativas si el problema persiste

### Opción 1: Header Authorization opcional
Modificar PaymentController para que el header sea `required = false`

### Opción 2: Excluir /payments del auth interceptor
Modificar auth.interceptor.ts para NO procesar errores de /payments

### Opción 3: Usar API Gateway
Crear un gateway que maneje autenticación y enrute a los microservicios

### Opción 4: Implementar refresh token
Cuando el token expire, renovarlo automáticamente sin hacer logout

## Logs Útiles

### Backend (Payment Service)
```bash
docker logs back-payment-service-1 --tail 100
docker logs back-payment-service-1 --follow
docker logs back-payment-service-1 2>&1 | Select-String "401|Unauthorized|JWT"
```

### Backend (Auth Service)
```bash
docker logs back-auth-service-1 --tail 50
```

### Frontend (Browser Console)
- Network tab → filter "payments"
- Console tab → filter "auth" or "payment"

## Estado Actual del Sistema

### Endpoints Públicos (No requieren JWT)
- ✅ POST /api/payments/webhook
- ✅ GET /api/payments/health
- ✅ POST /api/payments/create (NUEVO)
- ✅ POST /admin/credentials/**

### Endpoints Protegidos (Requieren JWT)
- 🔒 GET /api/payments/{id}
- 🔒 GET /api/payments/order/{orderId}
- 🔒 Todos los demás endpoints

## Next Steps

1. ✅ **DONE**: Quitar validación JWT de /payments/create
2. ⏳ **TESTING**: Probar flujo completo de pago
3. 🔜 **TODO**: Implementar generación automática de tickets después del pago
4. 🔜 **TODO**: Agregar JWT validation de vuelta (producción)
5. 🔜 **TODO**: Implementar webhook handling para auto-generar tickets
