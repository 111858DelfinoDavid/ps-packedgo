# 🧪 Guía Completa de Pruebas - Payment Service

## 📋 Requisitos Previos

- ✅ PostgreSQL instalado y corriendo
- ✅ Java 17+ instalado
- ✅ Maven instalado
- ✅ Puerto 8082 disponible

## 🚀 Paso 1: Iniciar PostgreSQL

### Windows (Si usas PostgreSQL local)

```powershell
# Verificar si PostgreSQL está corriendo
Get-Service postgresql*

# Si no está corriendo, iniciarlo
Start-Service postgresql-x64-15  # Ajustar según tu versión
```

### Con Docker (Recomendado)

```powershell
# Iniciar solo PostgreSQL
docker run --name payment-db `
  -e POSTGRES_DB=payment_service_db `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:15-alpine

# Verificar que está corriendo
docker ps
```

## 🗄️ Paso 2: Crear la Base de Datos

### Opción A: Con psql

```powershell
# Conectar a PostgreSQL
psql -U postgres -h localhost

# Dentro de psql:
CREATE DATABASE payment_service_db;
\q
```

### Opción B: Con pgAdmin

1. Abrir pgAdmin
2. Conectar al servidor local
3. Click derecho en "Databases" → "Create" → "Database"
4. Nombre: `payment_service_db`
5. Save

### Opción C: Dejar que Hibernate lo cree (si tienes permisos)

La aplicación creará las tablas automáticamente.

## ▶️ Paso 3: Iniciar la Aplicación

```powershell
# Navegar al directorio del proyecto
cd C:\Users\david\Documents\tesis\payment-service

# Limpiar y compilar
mvn clean install -DskipTests

# Iniciar la aplicación
mvn spring-boot:run
```

**Espera a ver este mensaje:**
```
Started PaymentServiceApplication in X.XXX seconds
```

La aplicación estará disponible en: `http://localhost:8082`

## ✅ Paso 4: Verificar que la Aplicación Funciona

### Test 1: Health Check

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/payments/health" -Method Get
```

**Respuesta esperada:**
```json
{
  "status": "UP"
}
```

### Test 2: Actuator Health (con detalles de BD)

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -Method Get
```

**Respuesta esperada:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

✅ Si ves `"status": "UP"` y `"db": { "status": "UP" }`, ¡todo está funcionando!

## 🔑 Paso 5: Configurar Credenciales de MercadoPago

### Opción A: Usar Credenciales de Prueba (Recomendado para testing)

```powershell
# Guardar credenciales de prueba
$credentials = @{
    adminId = 1
    accessToken = "TEST-1234567890-010101-abc123def456-789012345"
    publicKey = "TEST-abc123def-456789-012345-678901-234567"
    isSandbox = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials" `
    -Method Post `
    -ContentType "application/json" `
    -Body $credentials
```

**Respuesta esperada:**
```json
{
  "message": "Credenciales guardadas exitosamente",
  "adminId": 1,
  "isSandbox": true,
  "isActive": true
}
```

### Opción B: Usar tus Credenciales Reales de MercadoPago

1. Ve a: https://www.mercadopago.com.ar/developers/panel/credentials
2. Copia tu **Access Token de TEST** (comienza con `TEST-`)
3. Copia tu **Public Key de TEST** (comienza con `TEST-`)
4. Ejecuta:

```powershell
$credentials = @{
    adminId = 1
    accessToken="APP_USR-1160956444149133-101721-055aec8c374959f568654aeda79ccd31-2932397372"
    publicKey = "APP_USR-704e26b4-2405-4401-8cd9-fe981e4f70ae"
    isSandbox = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials" `
    -Method Post `
    -ContentType "application/json" `
    -Body $credentials
```

### Verificar que las Credenciales se Guardaron

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials/check/1" -Method Get
```

**Respuesta esperada:**
```json
{
  "adminId": 1,
  "hasCredentials": true
}
```

## 💳 Paso 6: Crear un Pago de Prueba

```powershell
$payment = @{
    adminId = 1
    orderId = "ORDER-TEST-001"
    amount = 1500.00
    description = "Paquete Premium - Envío Express"
    payerEmail = "test_user_12345678@testuser.com"
    payerName = "Juan Pérez"
    externalReference = "REF-001"
    successUrl = "http://localhost:3000/payment/success"
    failureUrl = "http://localhost:3000/payment/failure"
    pendingUrl = "http://localhost:3000/payment/pending"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
    -Method Post `
    -ContentType "application/json" `
    -Body $payment

$response
```

**Respuesta esperada:**
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

✅ Si recibes una respuesta con `preferenceId` e `initPoint`, ¡funciona correctamente!

## 🌐 Paso 7: Completar el Pago (Opcional)

### Con Credenciales Reales de MercadoPago

1. Copia el `sandboxInitPoint` de la respuesta
2. Abre ese link en tu navegador
3. Verás la página de pago de MercadoPago
4. Usa estas credenciales de prueba:
   - **Usuario:** test_user_12345678@testuser.com
   - **Tarjeta:** 5031 7557 3453 0604
   - **CVV:** 123
   - **Vencimiento:** 11/25

5. Completa el pago
6. Serás redirigido a tu `successUrl`

### Verificar el Pago en la Base de Datos

```powershell
# Conectar a PostgreSQL
psql -U postgres -h localhost -d payment_service_db

# Dentro de psql:
SELECT * FROM payments ORDER BY created_at DESC LIMIT 1;
```

Deberías ver tu pago con status `PENDING` o `APPROVED`.

## 🔄 Paso 8: Probar Múltiples Admins (Multi-Tenant)

### Crear un segundo admin

```powershell
$credentials2 = @{
    adminId = 2
    accessToken = "TEST-9999999999-020202-xyz789ghi012-345678901"
    publicKey = "TEST-xyz789ghi-012345-678901-234567-890123"
    isSandbox = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials" `
    -Method Post `
    -ContentType "application/json" `
    -Body $credentials2
```

### Crear pago con admin 2

```powershell
$payment2 = @{
    adminId = 2  # ← Diferente admin
    orderId = "ORDER-ADMIN2-001"
    amount = 2500.00
    description = "Paquete Admin 2"
    payerEmail = "cliente2@email.com"
    payerName = "María López"
    externalReference = "REF-002"
    successUrl = "http://localhost:3000/success"
    failureUrl = "http://localhost:3000/failure"
    pendingUrl = "http://localhost:3000/pending"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
    -Method Post `
    -ContentType "application/json" `
    -Body $payment2
```

✅ Verás que usa las credenciales del admin 2, diferentes del admin 1!

## 🧪 Paso 9: Pruebas de Error

### Test: Admin sin Credenciales

```powershell
$paymentInvalid = @{
    adminId = 999  # ← Admin que no existe
    orderId = "ORDER-INVALID"
    amount = 1000.00
    description = "Test Error"
    payerEmail = "test@email.com"
    payerName = "Test"
    externalReference = "REF-ERR"
    successUrl = "http://localhost:3000/success"
    failureUrl = "http://localhost:3000/failure"
    pendingUrl = "http://localhost:3000/pending"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
        -Method Post `
        -ContentType "application/json" `
        -Body $paymentInvalid
} catch {
    $_.Exception.Response
}
```

**Respuesta esperada: Error 401**
```json
{
  "timestamp": "2024-10-25T...",
  "status": 401,
  "error": "Credential Error",
  "message": "Admin sin credenciales configuradas o credenciales inactivas"
}
```

### Test: Validación de Campos

```powershell
$paymentInvalid = @{
    adminId = 1
    orderId = ""  # ← Vacío (inválido)
    amount = -100  # ← Negativo (inválido)
    description = ""
    payerEmail = "invalid-email"  # ← Email inválido
    payerName = "Test"
    externalReference = "REF"
    successUrl = "http://localhost:3000/success"
    failureUrl = "http://localhost:3000/failure"
    pendingUrl = "http://localhost:3000/pending"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
        -Method Post `
        -ContentType "application/json" `
        -Body $paymentInvalid
} catch {
    Write-Host "Error de validación (esperado)"
}
```

## 📊 Paso 10: Verificar en la Base de Datos

```sql
-- Ver todas las credenciales configuradas
SELECT 
    id,
    admin_id,
    LEFT(access_token, 20) as token_preview,
    is_active,
    is_sandbox,
    created_at
FROM admin_credentials
ORDER BY admin_id;

-- Ver todos los pagos
SELECT 
    id,
    admin_id,
    order_id,
    amount,
    status,
    payer_email,
    created_at
FROM payments
ORDER BY created_at DESC;

-- Ver pagos por admin
SELECT 
    admin_id,
    COUNT(*) as total_pagos,
    SUM(amount) as monto_total
FROM payments
GROUP BY admin_id;
```

## 🎯 Script de Prueba Completo (Copia y Pega)

```powershell
# ==============================================
# SCRIPT DE PRUEBA COMPLETO - PAYMENT SERVICE
# ==============================================

Write-Host "🚀 Iniciando pruebas del Payment Service..." -ForegroundColor Green

# 1. Health Check
Write-Host "`n✅ Test 1: Health Check" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8082/api/payments/health" -Method Get
    Write-Host "   ✅ Aplicación: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Error: Aplicación no responde" -ForegroundColor Red
    exit
}

# 2. Actuator Health
Write-Host "`n✅ Test 2: Actuator Health (con BD)" -ForegroundColor Yellow
try {
    $actuator = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -Method Get
    Write-Host "   ✅ App: $($actuator.status)" -ForegroundColor Green
    Write-Host "   ✅ BD: $($actuator.components.db.status)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Error en actuator" -ForegroundColor Red
}

# 3. Guardar credenciales Admin 1
Write-Host "`n✅ Test 3: Guardar Credenciales Admin 1" -ForegroundColor Yellow
$credentials = @{
    adminId = 1
    accessToken = "TEST-1234567890-010101-abc123def456-789012345"
    publicKey = "TEST-abc123def-456789-012345-678901-234567"
    isSandbox = $true
} | ConvertTo-Json

try {
    $credResult = Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials" `
        -Method Post `
        -ContentType "application/json" `
        -Body $credentials
    Write-Host "   ✅ Credenciales guardadas: Admin ID $($credResult.adminId)" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️  Credenciales ya existían o error al guardar" -ForegroundColor Yellow
}

# 4. Verificar credenciales
Write-Host "`n✅ Test 4: Verificar Credenciales" -ForegroundColor Yellow
try {
    $check = Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials/check/1" -Method Get
    Write-Host "   ✅ Admin 1 tiene credenciales: $($check.hasCredentials)" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Error verificando credenciales" -ForegroundColor Red
}

# 5. Crear pago
Write-Host "`n✅ Test 5: Crear Pago" -ForegroundColor Yellow
$payment = @{
    adminId = 1
    orderId = "ORDER-TEST-$(Get-Date -Format 'yyyyMMddHHmmss')"
    amount = 1500.00
    description = "Paquete Premium - Test Automático"
    payerEmail = "test@email.com"
    payerName = "Juan Pérez Test"
    externalReference = "REF-$(Get-Date -Format 'yyyyMMddHHmmss')"
    successUrl = "http://localhost:3000/payment/success"
    failureUrl = "http://localhost:3000/payment/failure"
    pendingUrl = "http://localhost:3000/payment/pending"
} | ConvertTo-Json

try {
    $paymentResult = Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
        -Method Post `
        -ContentType "application/json" `
        -Body $payment
    
    Write-Host "   ✅ Pago creado exitosamente!" -ForegroundColor Green
    Write-Host "   📝 Payment ID: $($paymentResult.paymentId)" -ForegroundColor Cyan
    Write-Host "   📝 Order ID: $($paymentResult.orderId)" -ForegroundColor Cyan
    Write-Host "   📝 Status: $($paymentResult.status)" -ForegroundColor Cyan
    Write-Host "   📝 Amount: $($paymentResult.amount) $($paymentResult.currency)" -ForegroundColor Cyan
    Write-Host "   📝 Preference ID: $($paymentResult.preferenceId)" -ForegroundColor Cyan
    Write-Host "`n   🌐 Init Point: $($paymentResult.initPoint)" -ForegroundColor Blue
} catch {
    Write-Host "   ❌ Error creando pago: $($_.Exception.Message)" -ForegroundColor Red
}

# 6. Test de error (admin sin credenciales)
Write-Host "`n✅ Test 6: Error - Admin sin Credenciales" -ForegroundColor Yellow
$paymentError = @{
    adminId = 999
    orderId = "ORDER-ERROR-001"
    amount = 1000.00
    description = "Test Error"
    payerEmail = "test@email.com"
    payerName = "Test Error"
    externalReference = "REF-ERR"
    successUrl = "http://localhost:3000/success"
    failureUrl = "http://localhost:3000/failure"
    pendingUrl = "http://localhost:3000/pending"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
        -Method Post `
        -ContentType "application/json" `
        -Body $paymentError
    Write-Host "   ❌ No se esperaba éxito" -ForegroundColor Red
} catch {
    Write-Host "   ✅ Error capturado correctamente (esperado)" -ForegroundColor Green
}

Write-Host "`n🎉 ¡Pruebas completadas!" -ForegroundColor Green
Write-Host "`n📊 Resumen:" -ForegroundColor Cyan
Write-Host "   ✅ Aplicación funcionando" -ForegroundColor Green
Write-Host "   ✅ Base de datos conectada" -ForegroundColor Green
Write-Host "   ✅ Credenciales guardadas" -ForegroundColor Green
Write-Host "   ✅ Pago creado exitosamente" -ForegroundColor Green
Write-Host "   ✅ Validaciones funcionando" -ForegroundColor Green
Write-Host "`n💡 Siguiente paso: Abre el initPoint en tu navegador para completar el pago" -ForegroundColor Yellow
```

## 📝 Guardar y Ejecutar el Script

1. Copia todo el script de arriba
2. Guarda como `test-payment-service.ps1`
3. Ejecuta:

```powershell
.\test-payment-service.ps1
```

## 🐛 Troubleshooting

### Error: "No se puede conectar a la BD"

```powershell
# Verificar PostgreSQL
Get-Service postgresql*

# O con Docker
docker ps | Select-String payment-db
```

### Error: "Puerto 8082 en uso"

```powershell
# Ver qué proceso usa el puerto
netstat -ano | findstr :8082

# Matar el proceso
taskkill /PID <PID> /F
```

### Error: "Maven no encontrado"

```powershell
# Verificar instalación
mvn -version

# Si no está instalado, descargar de: https://maven.apache.org/download.cgi
```

## ✅ Checklist Final

- [ ] PostgreSQL corriendo
- [ ] Base de datos creada
- [ ] Aplicación iniciada (puerto 8082)
- [ ] Health check responde OK
- [ ] Credenciales guardadas
- [ ] Pago creado exitosamente
- [ ] PreferenceId recibido
- [ ] InitPoint generado

¡Si completaste todos los pasos, tu Payment Service está funcionando perfectamente! 🎉
