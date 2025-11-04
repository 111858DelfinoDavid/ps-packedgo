# 🎯 GUÍA COMPLETA - PROBAR FLUJO DE PAGO CON DOCKER

## ✅ ESTADO ACTUAL

Todos los servicios están corriendo en Docker:

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**Servicios disponibles:**
- ✅ Auth Service: http://localhost:8081
- ✅ Users Service: http://localhost:8082
- ✅ Order Service: http://localhost:8084
- ✅ Payment Service: http://localhost:8085
- ✅ Event Service: http://localhost:8086
- ✅ Consumption Service: http://localhost:8088

**Bases de datos:**
- ✅ Auth DB: localhost:5433
- ✅ Users DB: localhost:5434
- ✅ Event DB: localhost:5435
- ✅ Order DB: localhost:5436
- ✅ Payment DB: localhost:5437

---

## 🚀 PASO 1: INICIAR EL FRONTEND

El frontend NO está en Docker, debes iniciarlo manualmente:

```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\front-angular
ng serve
```

**Espera el mensaje:**
```
✔ Compiled successfully
```

---

## 🌐 PASO 2: ABRIR LA APLICACIÓN

Abre tu navegador en:
```
http://localhost:4200
```

---

## 👤 PASO 3: REGISTRAR USUARIOS

### 3.1 Registrar Admin

1. Ve a: `http://localhost:4200/admin/register`
2. Completa el formulario:
   - **Nombre**: Admin Test
   - **Email**: admin@packedgo.com
   - **Password**: Admin123!
   - **Confirm Password**: Admin123!
3. Haz clic en **Registrar**

### 3.2 Registrar Consumer

1. Ve a: `http://localhost:4200/consumer/register`
2. Completa el formulario:
   - **Nombre**: Consumer Test
   - **Email**: consumer@test.com
   - **Password**: Test123!
   - **Confirm Password**: Test123!
3. Haz clic en **Registrar**

---

## 🎪 PASO 4: CREAR EVENTO (Como Admin)

1. **Login como Admin:**
   - Email: admin@packedgo.com
   - Password: Admin123!

2. **Ir a "Gestión de Eventos"**

3. **Crear un nuevo evento:**
   - Nombre: "Concierto de Prueba"
   - Descripción: "Evento para probar el flujo de pago"
   - Fecha: Cualquier fecha futura
   - Ubicación: "Venue Test"
   - Precio del ticket: 1000 (ARS)
   - Cantidad de tickets: 100
   - (Opcional) Agregar consumiciones

4. **Guardar el evento**

5. **Configurar MercadoPago (IMPORTANTE):**
   - En el panel de administración del evento
   - Ir a "Configuración de Pagos"
   - Ingresar:
     - **Access Token**: `APP_USR-1160956444149133-101721-055aec8c374959f568654aeda79ccd31-2932397372`
     - **Public Key**: `APP_USR-704e26b4-2405-4401-8cd9-fe981e4f70ae`
   - Guardar credenciales

6. **Logout**

---

## 🛒 PASO 5: COMPRAR TICKETS (Como Consumer)

1. **Login como Consumer:**
   - Email: consumer@test.com
   - Password: Test123!

2. **Buscar eventos disponibles**
   - Verás el "Concierto de Prueba"

3. **Ver detalles del evento**

4. **Agregar al carrito:**
   - Cantidad de tickets: 2
   - (Opcional) Seleccionar consumiciones
   - Clic en "Agregar al Carrito"

5. **Ir al Carrito**

6. **Proceder al Pago**

---

## 💳 PASO 6: PAGAR CON MERCADOPAGO

1. **Serás redirigido a MercadoPago Checkout**

2. **Usar tarjeta de prueba:**
   ```
   Número: 5031 7557 3453 0604
   CVV: 123
   Fecha de vencimiento: 12/25
   Nombre en la tarjeta: APRO TEST
   ```

3. **Completar el pago:**
   - Hacer clic en "Pagar"
   - ⏱️ **NO CERRAR LA VENTANA**
   - Esperar a ser redirigido automáticamente

---

## 🎫 PASO 7: VERIFICAR TICKETS GENERADOS

Después del pago, serás redirigido a la página de éxito:

1. **El sistema esperará 2 segundos** ⏱️

2. **Verificará automáticamente el pago** en MercadoPago 🔍

3. **Generará los tickets con QR codes** 🎟️

4. **Verás:**
   - ✅ "Pago confirmado exitosamente"
   - 🎫 Tickets con códigos QR
   - 📥 Botón "Descargar QR" para cada ticket
   - 📥 Botón "Descargar todos los QR"

---

## 📋 PASO 8: VERIFICAR LOGS

### 8.1 Ver logs del Payment Service

```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back
docker compose logs -f payment-service
```

**Busca estas líneas:**
```
🔍 Verificando estado de pago para orden: ORD-202511-XXXX
Consultando MercadoPago con mpPaymentId: 123456789
Estado anterior: PENDING → Estado nuevo: APPROVED
✅ Estado de pago cambió, notificando a Order Service
```

### 8.2 Ver logs del Order Service

```powershell
docker compose logs -f order-service
```

**Busca estas líneas:**
```
Updating order ORD-202511-XXXX with payment status: APPROVED
Order ORD-202511-XXXX marked as PAID
🎟️ Generating tickets for order: ORD-202511-XXXX
✅ Ticket #1 generated: ID=123
✅ Ticket #2 generated: ID=124
```

---

## 🛠️ COMANDOS ÚTILES

### Ver estado de todos los servicios
```powershell
C:\Users\david\Documents\ps-packedgo\docker-status.ps1
```

### Ver logs de todos los servicios
```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back
docker compose logs -f
```

### Ver logs de un servicio específico
```powershell
docker compose logs -f payment-service
docker compose logs -f order-service
docker compose logs -f event-service
```

### Reiniciar un servicio
```powershell
docker compose restart payment-service
```

### Detener todos los servicios
```powershell
docker compose down
```

### Iniciar todos los servicios
```powershell
docker compose up -d
```

### Ver base de datos de Payment Service
```powershell
docker exec -it back-payment-db-1 psql -U payment_user -d payment_db
\dt
SELECT * FROM payments;
\q
```

---

## 🆘 TROUBLESHOOTING

### Problema: No se generan los tickets

**Solución:**
1. Verificar logs de Order Service:
   ```powershell
   docker compose logs order-service | Select-String "error|exception" -Context 2
   ```

2. Verificar logs de Payment Service:
   ```powershell
   docker compose logs payment-service | Select-String "error|exception" -Context 2
   ```

3. Verificar que Event Service esté corriendo:
   ```powershell
   docker ps | Select-String event-service
   ```

### Problema: El pago no se verifica

**Solución:**
1. Asegúrate de haber **completado el pago** en MercadoPago
2. NO cierres la ventana antes de la redirección
3. Ver logs de Payment Service para errores
4. Verificar las credenciales de MercadoPago del admin

### Problema: Error de conexión a base de datos

**Solución:**
1. Verificar que las bases de datos estén saludables:
   ```powershell
   docker ps | Select-String "healthy"
   ```

2. Si no están healthy:
   ```powershell
   docker compose restart payment-db order-db event-db
   ```

### Problema: Frontend no se conecta al backend

**Solución:**
1. Verificar que los servicios estén corriendo:
   ```powershell
   C:\Users\david\Documents\ps-packedgo\docker-status.ps1
   ```

2. Verificar el archivo `proxy.conf.json` del frontend:
   ```json
   {
     "/api": {
       "target": "http://localhost:8080",
       "secure": false
     }
   }
   ```

---

## 🎯 RESULTADO ESPERADO

✅ Usuario realiza el pago en MercadoPago  
✅ Es redirigido automáticamente al sitio  
✅ Frontend espera 2 segundos  
✅ Frontend llama a `/api/payments/verify/{orderId}`  
✅ Payment Service consulta MercadoPago API  
✅ Payment Service actualiza el pago a APPROVED  
✅ Payment Service notifica a Order Service  
✅ Order Service marca la orden como PAID  
✅ Order Service genera tickets con QR codes  
✅ Frontend recibe los tickets y los muestra  
✅ Usuario puede descargar los QR codes  

---

## 📞 ESTADO DE LOS SERVICIOS

Para ver el estado actual:

```powershell
C:\Users\david\Documents\ps-packedgo\docker-status.ps1
```

Para ver logs en tiempo real:

```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back
docker compose logs -f payment-service order-service
```

---

**¡Todo está listo para probar el flujo completo! 🚀**

Las correcciones implementadas:
- ✅ Verificación manual mejorada (`verifyPaymentStatus`)
- ✅ Funciona sin webhook
- ✅ Consulta directa a MercadoPago API
- ✅ Actualización automática del estado
- ✅ Generación de tickets garantizada

¡Ahora prueba el flujo completo!
