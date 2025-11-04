# 🚀 INICIO RÁPIDO - PACKEDGO CON MERCADOPAGO

## ✅ TODO LO QUE SE CORRIGIÓ

1. ✅ Sistema de verificación manual mejorado (funciona sin webhooks)
2. ✅ Configuración local lista (`.env.local`)
3. ✅ Script de verificación de servicios (`verify-services.ps1`)
4. ✅ Documentación completa de MercadoPago
5. ✅ Código compilado y funcionando

---

## 🏁 INICIAR TODO EL SISTEMA (SIN WEBHOOKS)

### Paso 1: Verificar PostgreSQL

```powershell
# Verificar si PostgreSQL está corriendo
Get-Service postgresql*

# Si no está corriendo, iniciarlo
Start-Service postgresql-x64-15  # Ajustar según tu versión

# O usar Docker
docker run --name postgres-packedgo `
  -e POSTGRES_PASSWORD=postgres `
  -p 5432:5432 `
  -d postgres:15-alpine
```

### Paso 2: Crear bases de datos

```powershell
# Conectar a PostgreSQL
psql -U postgres -h localhost

# Crear las bases de datos necesarias
CREATE DATABASE auth_service_db;
CREATE DATABASE users_service_db;
CREATE DATABASE order_service_db;
CREATE DATABASE payment_service_db;
CREATE DATABASE event_service_db;
\q
```

### Paso 3: Copiar configuración local

```powershell
# Para Payment Service (el más importante)
cd packedgo\back\payment-service
cp .env.local .env

# Verificar que tenga localhost
cat .env | Select-String "DB_URL"
# Debe decir: DB_URL=jdbc:postgresql://localhost:5432/payment_service_db
```

### Paso 4: Iniciar servicios backend

**Abrir 5 terminales PowerShell separadas:**

```powershell
# Terminal 1 - Auth Service
cd packedgo\back\auth-service
.\mvnw spring-boot:run

# Terminal 2 - Users Service
cd packedgo\back\users-service
.\mvnw spring-boot:run

# Terminal 3 - Order Service
cd packedgo\back\order-service
.\mvnw spring-boot:run

# Terminal 4 - Payment Service
cd packedgo\back\payment-service
.\mvnw spring-boot:run

# Terminal 5 - Event Service
cd packedgo\back\event-service
.\mvnw spring-boot:run
```

**Esperar a ver:** `Started [Servicio]Application in X seconds`

### Paso 5: Iniciar frontend

```powershell
# Terminal 6 - Frontend Angular
cd packedgo\front-angular
npm install  # Solo la primera vez
ng serve
```

### Paso 6: Verificar que todo esté corriendo

```powershell
# En una nueva terminal
.\verify-services.ps1
```

**Resultado esperado:**
```
✅ Auth Service - Puerto 8081
✅ Users Service - Puerto 8082
✅ Order Service - Puerto 8084
✅ Payment Service - Puerto 8085
✅ Event Service - Puerto 8086
✅ PostgreSQL - Puerto 5432
✅ Angular Frontend - Puerto 4200
```

---

## 🧪 PROBAR EL FLUJO DE PAGO

### 1. Abrir navegador

```
http://localhost:4200
```

### 2. Login como Consumer

```
Email: consumer@test.com
Password: password123
```

(Si no existe, registrarse primero)

### 3. Buscar eventos

- Ir a "Eventos disponibles"
- Ver eventos creados por administradores

### 4. Agregar al carrito

- Seleccionar un evento
- Elegir cantidad
- Agregar consumiciones si hay
- Agregar al carrito

### 5. Hacer Checkout

- Ir al carrito
- Hacer clic en "Proceder al pago"
- Confirmar orden

### 6. Pagar con MercadoPago

**Usar tarjeta de prueba:**
```
Número: 5031 7557 3453 0604
CVV: 123
Fecha: 12/25 (cualquier fecha futura)
Nombre: APRO TEST
```

### 7. Completar el pago

- Hacer clic en "Pagar"
- **IMPORTANTE:** Esperar a ser redirigido automáticamente

### 8. Ver los tickets

- Serás redirigido a `/order-success`
- El sistema esperará 2 segundos
- **Verificará automáticamente** el pago en MercadoPago
- Actualizará el estado a PAID
- **Generará los tickets con QR**
- Los mostrará en pantalla

### 9. Descargar QR codes

- Hacer clic en "Descargar QR" en cada ticket
- O "Descargar todos los QR"

---

## ⚠️ SI NO SE GENERAN LOS TICKETS

### Verificación manual:

```powershell
# Obtener el token JWT (después de login)
# Lo puedes obtener de localStorage en el navegador

# Verificar el pago manualmente
Invoke-RestMethod -Uri "http://localhost:8085/api/payments/verify/ORD-202511-XXXX" `
  -Method POST `
  -Headers @{
    "Authorization" = "Bearer TU_TOKEN_JWT"
    "Content-Type" = "application/json"
  }
```

### Ver logs:

**Payment Service (Terminal 4):**
```
🔍 Verificando estado de pago para orden: ORD-202511-XXX
Consultando MercadoPago con mpPaymentId: 123456789
Estado anterior: PENDING → Estado nuevo: APPROVED
✅ Estado de pago cambió, notificando a Order Service
```

**Order Service (Terminal 3):**
```
Updating order ORD-202511-XXX with payment status: APPROVED
Order ORD-202511-XXX marked as PAID
🎟️ Generating tickets for order: ORD-202511-XXX
✅ Ticket #1 generated: ID=123, QR=data:image/png;base64,...
```

---

## 🔧 (OPCIONAL) CONFIGURAR WEBHOOKS

Si quieres que funcione automáticamente sin esperar:

### 1. Instalar ngrok

```powershell
winget install ngrok
```

### 2. Iniciar ngrok

```powershell
ngrok http 8085
```

### 3. Copiar URL HTTPS

```
Forwarding: https://abc123.ngrok-free.app -> http://localhost:8085
```

### 4. Configurar webhook

```powershell
# Editar packedgo\back\payment-service\.env
notepad packedgo\back\payment-service\.env

# Agregar:
WEBHOOK_URL=https://abc123.ngrok-free.app/api/payments/webhook
```

### 5. Reiniciar Payment Service

```powershell
# Detener con Ctrl+C
# Volver a ejecutar
cd packedgo\back\payment-service
.\mvnw spring-boot:run
```

### 6. Verificar en logs

```
Webhook configurado: https://abc123.ngrok-free.app/api/payments/webhook?adminId=1
```

**Ahora:** Cuando pagues, MercadoPago notificará automáticamente y los tickets se generarán instantáneamente.

---

## 📋 CHECKLIST ANTES DE PROBAR

- [ ] PostgreSQL corriendo en 5432
- [ ] Bases de datos creadas (auth_service_db, users_service_db, etc.)
- [ ] Auth Service corriendo en 8081
- [ ] Users Service corriendo en 8082
- [ ] Order Service corriendo en 8084
- [ ] Payment Service corriendo en 8085
- [ ] Event Service corriendo en 8086
- [ ] Frontend corriendo en 4200
- [ ] `.env` configurado en payment-service (localhost)
- [ ] Credenciales de MercadoPago válidas
- [ ] (Opcional) ngrok configurado

---

## 🎯 QUÉ ESPERAR

### ✅ Con verificación manual (sin webhooks):
1. Pagas en MercadoPago ✅
2. Regresas a la app ✅
3. Esperas 2 segundos ⏱️
4. Sistema verifica automáticamente ✅
5. Tickets aparecen en pantalla ✅

### ✅ Con webhooks (ngrok):
1. Pagas en MercadoPago ✅
2. MercadoPago notifica webhook ⚡
3. Payment Service actualiza ✅
4. Order Service genera tickets ✅
5. Regresas y ya están listos ✅

---

## 🆘 PROBLEMAS COMUNES

### "Service not available"
- Verificar que todos los servicios estén corriendo
- Ejecutar `.\verify-services.ps1`

### "Database connection failed"
- Verificar PostgreSQL corriendo
- Verificar bases de datos creadas
- Verificar `.env` con localhost

### "Payment not found"
- Verificar que el checkout se completó
- Verificar Order Service está corriendo

### "No tickets generated"
- Ver logs de Order Service
- Verificar Payment Service notificó correctamente
- Verificar Event Service está corriendo

---

## 📚 DOCUMENTACIÓN ADICIONAL

- **Configuración detallada:** `GUIA_CONFIGURACION_MERCADOPAGO.md`
- **Resumen de cambios:** `RESUMEN_CORRECCIONES.md`
- **Diagnóstico:** `DIAGNOSTICO_FLUJO_PAGO_Y_QR.md`

---

## ✅ ESTÁS LISTO

Todo está configurado y corregido. Solo necesitas:
1. Iniciar los servicios
2. Probar el flujo de pago
3. Ver tus tickets con QR

**¡Buena suerte! 🚀**
