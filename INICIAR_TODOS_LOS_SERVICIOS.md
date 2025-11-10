# 🚀 INSTRUCCIONES PARA INICIAR TODOS LOS SERVICIOS

## ✅ ESTADO ACTUAL

- ✅ PostgreSQL corriendo en Docker (puerto 5432)
- ✅ Bases de datos creadas (auth_service_db, users_service_db, order_service_db, payment_service_db, event_service_db)
- ✅ Payment Service configurado para localhost (.env copiado)
- ✅ Código compilado sin errores

---

## 🎯 PASO 1: ABRIR 5 TERMINALES POWERSHELL

Abre 5 ventanas de PowerShell en VS Code o terminales separadas.

---

## 📝 PASO 2: INICIAR SERVICIOS BACKEND

### Terminal 1 - Auth Service ✅
```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back\auth-service
.\mvnw spring-boot:run
```
**Esperar mensaje:** `Started AuthServiceApplication in X seconds`

---

### Terminal 2 - Users Service ✅
```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back\users-service
.\mvnw spring-boot:run
```
**Esperar mensaje:** `Started UsersServiceApplication in X seconds`

---

### Terminal 3 - Event Service ✅
```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back\event-service
.\mvnw spring-boot:run
```
**Esperar mensaje:** `Started EventServiceApplication in X seconds`

---

### Terminal 4 - Order Service ✅
```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back\order-service
.\mvnw spring-boot:run
```
**Esperar mensaje:** `Started OrderServiceApplication in X seconds`

---

### Terminal 5 - Payment Service ✅ (EL MÁS IMPORTANTE)
```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back\payment-service
.\mvnw spring-boot:run
```
**Buscar en los logs:**
```
⚠️  Webhook NO configurado - las notificaciones automáticas no funcionarán
```
**Esto es NORMAL** - El sistema usará verificación manual automática.

---

## 🌐 PASO 3: INICIAR FRONTEND (Terminal 6)

```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\front-angular
ng serve
```

**Esperar mensaje:** `Compiled successfully`

---

## ✅ PASO 4: VERIFICAR QUE TODO ESTÉ CORRIENDO

En una nueva terminal:

```powershell
cd C:\Users\david\Documents\ps-packedgo
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

## 🧪 PASO 5: PROBAR EL FLUJO DE PAGO

### 1. Abrir navegador
```
http://localhost:4200
```

### 2. Registrarse como Admin (si no tienes cuenta)
- Ir a `/admin/register`
- Crear cuenta de administrador

### 3. Login como Admin
- Email: tu email
- Password: tu password

### 4. Crear un evento
- Ir a "Gestión de Eventos"
- Crear un evento nuevo
- Agregar consumiciones (opcional)

### 5. Logout y registrarse como Consumer
- Logout del admin
- Ir a `/consumer/register`
- Crear cuenta de consumidor

### 6. Login como Consumer
- Email: consumer email
- Password: consumer password

### 7. Buscar eventos disponibles
- Ver el evento creado por el admin
- Ver detalles

### 8. Agregar al carrito
- Seleccionar cantidad
- Elegir consumiciones si hay
- Agregar al carrito

### 9. Hacer Checkout
- Ir al carrito
- Hacer clic en "Proceder al pago"
- Confirmar la orden

### 10. Pagar con MercadoPago
**Usar tarjeta de prueba:**
```
Número: 5031 7557 3453 0604
CVV: 123
Fecha: 12/25
Nombre: APRO TEST
```

### 11. Completar el pago
- Hacer clic en "Pagar" en MercadoPago
- **IMPORTANTE:** Esperar a ser redirigido automáticamente
- NO cerrar la ventana

### 12. Ver los tickets 🎫
Serás redirigido a `/order-success`:
- El sistema esperará 2 segundos ⏱️
- **Verificará automáticamente** el pago en MercadoPago
- Actualizará el estado a PAID
- **Generará los tickets con QR** ✅
- Los mostrará en pantalla

### 13. Descargar QR codes
- Hacer clic en "Descargar QR" en cada ticket
- O "Descargar todos los QR"

---

## 📋 LOGS A OBSERVAR

### En Payment Service (Terminal 5):

Cuando vuelvas después del pago, deberías ver:

```
POST /api/payments/verify/ORD-202511-XXXX
🔍 Verificando estado de pago para orden: ORD-202511-XXXX
Consultando MercadoPago con mpPaymentId: 123456789
Estado anterior: PENDING → Estado nuevo: APPROVED
✅ Estado de pago cambió, notificando a Order Service
Orden ORD-202511-XXXX actualizada exitosamente en order-service
```

### En Order Service (Terminal 4):

```
Updating order ORD-202511-XXXX with payment status: APPROVED
Order ORD-202511-XXXX marked as PAID
🎟️ Generating tickets for order: ORD-202511-XXXX
✅ Ticket #1 generated: ID=123, QR=data:image/png;base64,...
```

---

## 🆘 SI ALGO FALLA

### Problema: "Connection refused" en algún servicio
**Solución:** Verificar que ese servicio esté corriendo en su terminal

### Problema: "No se generan tickets"
**Solución:** 
1. Ver logs de Order Service
2. Ver logs de Payment Service
3. Verificar que Event Service esté corriendo

### Problema: "Error de base de datos"
**Solución:**
```powershell
# Verificar PostgreSQL
docker ps | Select-String postgres

# Si no está corriendo
docker start postgres-packedgo
```

### Problema: "El pago no se verifica"
**Solución:** 
1. Asegúrate de **completar el pago** en MercadoPago
2. Esperar a ser redirigido (no cerrar la ventana)
3. Ver logs de Payment Service para errores

---

## 🎯 RESULTADO ESPERADO

✅ Usuario paga en MercadoPago  
✅ Es redirigido automáticamente  
✅ El frontend verifica el pago (2 segundos)  
✅ Payment Service consulta MercadoPago  
✅ Payment Service notifica a Order Service  
✅ Order Service genera tickets con QR  
✅ Tickets aparecen en pantalla  
✅ Usuario puede descargar QR codes  

---

## 📞 NECESITAS AYUDA?

1. Ejecutar `.\verify-services.ps1` para diagnóstico
2. Revisar logs de cada servicio
3. Ver `GUIA_CONFIGURACION_MERCADOPAGO.md` para troubleshooting

---

**¡Todo está listo para probar! 🚀**
