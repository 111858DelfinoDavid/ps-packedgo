# Guía de Pruebas del API - Payment Service

Este documento contiene ejemplos de requests para probar el Payment Service.

## Requisitos Previos

1. Base de datos PostgreSQL corriendo
2. Aplicación Payment Service ejecutándose en `http://localhost:8082`
3. Credenciales de MercadoPago configuradas en la BD

## Configurar Credenciales de Admin

Antes de crear pagos, debes insertar las credenciales de MercadoPago en la base de datos:

```sql
INSERT INTO admin_credentials (
    admin_id, 
    access_token, 
    public_key, 
    is_active, 
    is_sandbox, 
    created_at
) VALUES (
    1,
    'TEST-123456789-010101-abc123def456-789012345',  -- Tu Access Token de Sandbox
    'TEST-abc123def-456789-012345-678901-234567',    -- Tu Public Key de Sandbox
    true,
    true,
    NOW()
);
```

## Endpoints para Probar

### 1. Health Check

**GET** `http://localhost:8082/api/payments/health`

**Response:**
```json
{
  "status": "UP"
}
```

### 2. Actuator Health

**GET** `http://localhost:8082/actuator/health`

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

### 3. Crear Preferencia de Pago

**POST** `http://localhost:8082/api/payments/create`

**Headers:**
```
Content-Type: application/json
```

**Body:**
```json
{
  "adminId": 1,
  "orderId": "ORDER-TEST-001",
  "amount": 1500.00,
  "description": "Paquete Premium - Envío Express",
  "payerEmail": "test@email.com",
  "payerName": "Juan Pérez",
  "externalReference": "REF-001",
  "successUrl": "http://localhost:3000/payment/success",
  "failureUrl": "http://localhost:3000/payment/failure",
  "pendingUrl": "http://localhost:3000/payment/pending"
}
```

**Response Exitoso:**
```json
{
  "paymentId": 1,
  "orderId": "ORDER-TEST-001",
  "status": "PENDING",
  "amount": 1500.00,
  "currency": "ARS",
  "preferenceId": "123456789-abc123-def456",
  "initPoint": "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=...",
  "sandboxInitPoint": "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=...",
  "message": "Preferencia de pago creada exitosamente"
}
```

### 4. Webhook de MercadoPago

**POST** `http://localhost:8082/api/payments/webhook?adminId=1`

**Headers:**
```
Content-Type: application/json
```

**Body (Ejemplo de notificación de MercadoPago):**
```json
{
  "action": "payment.updated",
  "api_version": "v1",
  "data": {
    "id": "123456789"
  },
  "date_created": "2024-10-25T10:00:00Z",
  "id": 123456789,
  "live_mode": false,
  "type": "payment",
  "user_id": 987654321
}
```

**Response:**
```json
{
  "status": "processed"
}
```

### 5. Consultar Pago por OrderId

**GET** `http://localhost:8082/api/payments/order/ORDER-TEST-001`

**Response:**
```json
{
  "message": "Endpoint de consulta"
}
```

## Pruebas con cURL

### Health Check
```bash
curl -X GET http://localhost:8082/api/payments/health
```

### Crear Pago
```bash
curl -X POST http://localhost:8082/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 1,
    "orderId": "ORDER-TEST-001",
    "amount": 1500.00,
    "description": "Paquete Premium",
    "payerEmail": "test@email.com",
    "payerName": "Juan Pérez",
    "externalReference": "REF-001",
    "successUrl": "http://localhost:3000/payment/success",
    "failureUrl": "http://localhost:3000/payment/failure",
    "pendingUrl": "http://localhost:3000/payment/pending"
  }'
```

## Pruebas con PowerShell

### Health Check
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/payments/health" -Method Get
```

### Crear Pago
```powershell
$body = @{
    adminId = 1
    orderId = "ORDER-TEST-001"
    amount = 1500.00
    description = "Paquete Premium"
    payerEmail = "test@email.com"
    payerName = "Juan Pérez"
    externalReference = "REF-001"
    successUrl = "http://localhost:3000/payment/success"
    failureUrl = "http://localhost:3000/payment/failure"
    pendingUrl = "http://localhost:3000/payment/pending"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

## Errores Comunes

### Error: Credenciales no encontradas
```json
{
  "timestamp": "2024-10-25T10:00:00",
  "status": 401,
  "error": "Credential Error",
  "message": "Admin sin credenciales configuradas o credenciales inactivas"
}
```
**Solución:** Verificar que las credenciales del admin estén insertadas en la BD.

### Error: Validación de campos
```json
{
  "timestamp": "2024-10-25T10:00:00",
  "status": 400,
  "error": "Validation Error",
  "message": "Error de validación en los campos",
  "details": {
    "amount": "El monto debe ser mayor a 0",
    "orderId": "El ID de la orden es requerido"
  }
}
```
**Solución:** Verificar que todos los campos requeridos estén presentes y válidos.

## Flujo de Prueba Completo

1. **Verificar salud del servicio**: `GET /api/payments/health`
2. **Insertar credenciales de admin en BD** (SQL)
3. **Crear preferencia de pago**: `POST /api/payments/create`
4. **Usar el `initPoint` o `sandboxInitPoint`** para completar el pago en MercadoPago
5. **MercadoPago enviará notificación al webhook** automáticamente
6. **Verificar estado del pago en BD**

## Consultas SQL Útiles

```sql
-- Ver todos los pagos
SELECT * FROM payments ORDER BY created_at DESC;

-- Ver pagos de un admin específico
SELECT * FROM payments WHERE admin_id = 1 ORDER BY created_at DESC;

-- Ver credenciales configuradas
SELECT admin_id, is_active, is_sandbox FROM admin_credentials;

-- Actualizar estado de un pago manualmente (para pruebas)
UPDATE payments SET status = 'APPROVED' WHERE order_id = 'ORDER-TEST-001';
```

## Notas Importantes

- Para pruebas, usar credenciales de **Sandbox** de MercadoPago
- Los webhooks en desarrollo local requieren una URL pública (usar ngrok o similar)
- Verificar que el puerto 8082 esté disponible
- La BD debe estar corriendo antes de iniciar el servicio
