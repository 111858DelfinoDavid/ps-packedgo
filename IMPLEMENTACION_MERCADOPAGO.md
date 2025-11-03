# 🚀 GUÍA DE IMPLEMENTACIÓN - MERCADOPAGO CON POLLING AUTOMÁTICO

## ✅ CAMBIOS IMPLEMENTADOS

### 1. **Configuración del Payment Service**

#### `.env` actualizado:
```properties
SERVER_PORT=8085
MERCADOPAGO_ACCESS_TOKEN=APP_USR-1160956444149133-101721-055aec8c374959f568654aeda79ccd31-2932397372
MERCADOPAGO_PUBLIC_KEY=APP_USR-704e26b4-2405-4401-8cd9-fe981e4f70ae
WEBHOOK_URL=
```

✅ **Puerto corregido a 8085**
✅ **Credenciales de MercadoPago configuradas**
✅ **Webhook deshabilitado** (usaremos polling manual)

---

### 2. **Nuevo Endpoint de Verificación Manual**

**Backend - PaymentController.java**

Se agregó el endpoint:
```java
POST /api/payments/verify/{orderId}
```

Este endpoint:
- ✅ Consulta el estado del pago en MercadoPago
- ✅ Actualiza el estado en la base de datos
- ✅ Notifica a Order Service si el pago fue aprobado
- ✅ Retorna el estado actualizado

---

### 3. **Frontend - Verificación Automática**

**PaymentService.ts** - Nuevo método:
```typescript
verifyPaymentStatus(orderId: string): Observable<any>
```

**OrderSuccessComponent.ts** - Lógica mejorada:
```typescript
verifyPendingPayments(orders: any[]): void {
  // Espera 2 segundos después del redirect
  // Verifica todos los pagos pendientes
  // Recarga la sesión y los tickets automáticamente
}
```

**Flujo automático:**
1. Usuario regresa de MercadoPago → `order-success` component
2. Se detectan órdenes PENDING_PAYMENT
3. Espera 2 segundos (dar tiempo a MercadoPago)
4. Verifica estado de cada orden
5. Recarga sesión con estados actualizados
6. Carga y muestra tickets con QR

---

## 🧪 CÓMO PROBAR

### **Opción A: Script Automático** (RECOMENDADO)

```powershell
# Desde la raíz del proyecto
.\test-mercadopago-flow.ps1
```

Este script:
- ✅ Verifica que todos los servicios estén corriendo
- ✅ Valida la configuración de MercadoPago
- ✅ Muestra instrucciones paso a paso
- ✅ Abre el navegador automáticamente

---

### **Opción B: Manual**

#### 1. **Iniciar todos los servicios**

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

# Terminal 4 - Payment Service (PUERTO 8085)
cd packedgo\back\payment-service
.\mvnw spring-boot:run

# Terminal 5 - Event Service
cd packedgo\back\event-service
.\mvnw spring-boot:run

# Terminal 6 - Frontend Angular
cd packedgo\front-angular
npm start
```

#### 2. **Probar el flujo**

1. Abrir navegador en `http://localhost:4200`
2. Registrarse o iniciar sesión como **CUSTOMER**
3. Agregar eventos al carrito
4. Hacer checkout
5. Pagar con tarjeta de prueba:

**💳 TARJETA DE PRUEBA:**
```
Número: 5031 7557 3453 0604
CVV: 123
Fecha: 11/25 (cualquier fecha futura)
Nombre: APRO (para aprobar) o OTROC (para rechazar)
```

6. Esperar redirección automática
7. Ver banner de "Verificando estado de los pagos..."
8. Ver tickets con códigos QR

---

## 🔍 VERIFICACIÓN DE LOGS

### **Payment Service** (debe mostrar):

```
✅ POST /api/payments/create - AdminId from JWT: 1, OrderId: ORD-202511-001
✅ Preferencia creada exitosamente: 123456789 para orden: ORD-202511-001
✅ POST /api/payments/verify/ORD-202511-001 - UserId from JWT: 1
✅ Verificando estado del pago en MercadoPago: mpPaymentId=987654321
✅ Webhook procesado. Pago 1 actualizado: PENDING -> APPROVED
✅ Notificando aprobación de pago a order-service: orderId=ORD-202511-001
✅ Order-service notificado exitosamente para orden: ORD-202511-001
```

### **Order Service** (debe mostrar):

```
✅ POST /api/orders/payment-callback - Updating order: ORD-202511-001 with status: APPROVED
✅ Order ORD-202511-001 marked as PAID
🎟️ Generating tickets for order: ORD-202511-001
✅ Ticket #1 generated: ID=1, QR=data:image/png;base64,...
✅ Ticket generation completed for order ORD-202511-001: 2 successful, 0 failed
```

### **Frontend Console** (F12):

```
✅ Checkout multi completado: {sessionId: "xxx", ...}
⏳ Encontradas 1 órdenes pendientes, verificando pagos...
✅ Payment verification response: {orderId: "ORD-202511-001", status: "APPROVED", ...}
✅ Verificaciones completadas
✅ Tickets loaded: [{ticketId: 1, qrCode: "...", ...}]
```

---

## 🐛 TROUBLESHOOTING

### Problema: "No se generan los tickets"

**Solución:**
1. Verifica que payment-service esté en **puerto 8085** (no 8082)
2. Revisa los logs de payment-service
3. Asegúrate de esperar al menos 2 segundos después del pago
4. Recarga la página de order-success

### Problema: "Error 401 Unauthorized"

**Solución:**
1. Verifica que el token JWT sea válido
2. Vuelve a iniciar sesión
3. Verifica que el `JWT_SECRET` sea el mismo en todos los servicios

### Problema: "Payment Service no inicia"

**Solución:**
1. Verifica que PostgreSQL esté corriendo
2. Verifica la conexión a la base de datos en `.env`
3. Revisa el puerto 8085 no esté ocupado:
   ```powershell
   netstat -ano | findstr :8085
   ```

### Problema: "MercadoPago devuelve error"

**Solución:**
1. Verifica que las credenciales sean válidas (Prueba, no Productivas)
2. Asegúrate de usar tarjetas de prueba
3. Verifica que estés usando credenciales de Checkout API (no Checkout Pro)

---

## 🎯 PRÓXIMOS PASOS

Una vez que el flujo de pago funcione correctamente:

### 1. **Implementar Sistema de Empleados**

- [ ] Agregar rol `EMPLOYEE` en el enum ROLE
- [ ] Crear tabla `employee_credentials` en auth-service
- [ ] Crear endpoints de admin para gestionar empleados
- [ ] Crear login de empleados
- [ ] Crear dashboard simple para empleados

### 2. **Implementar Escaneo de QR**

- [ ] Crear componente `employee-scan-qr`
- [ ] Implementar lector de QR con cámara
- [ ] Crear endpoint `POST /event-service/employee/scan-qr`
- [ ] Mostrar información del ticket
- [ ] Botón para canjear consumición

### 3. **Implementar Canje de Consumiciones**

- [ ] Crear endpoint `POST /event-service/employee/redeem-consumption`
- [ ] Validar que el empleado pertenece al admin del evento
- [ ] Marcar consumición como canjeada
- [ ] Actualizar cantidad disponible
- [ ] Registrar quién y cuándo canjeó

---

## 📝 CHECKLIST DE VERIFICACIÓN

Antes de considerar que todo funciona:

- [ ] Payment service corre en puerto 8085
- [ ] Credenciales de MercadoPago configuradas correctamente
- [ ] Frontend puede crear checkout
- [ ] MercadoPago acepta el pago de prueba
- [ ] Frontend verifica automáticamente el pago
- [ ] Order Service recibe notificación
- [ ] Order Service marca orden como PAID
- [ ] Order Service genera tickets
- [ ] Event Service crea tickets con QR
- [ ] Frontend muestra tickets en order-success
- [ ] QR codes se pueden descargar
- [ ] Tickets aparecen en "Mis Entradas"

---

## 💡 NOTAS IMPORTANTES

### **Webhook vs Polling**

**Implementación actual: Polling Manual**
- ✅ No requiere ngrok
- ✅ Funciona en localhost
- ✅ Más fácil de desarrollar
- ❌ Requiere esperar 2 segundos
- ❌ Usuario debe esperar en la página

**Para producción: Webhook con HTTPS**
1. Deploy en servidor con dominio
2. Configurar `WEBHOOK_URL=https://tu-dominio.com/api/payments/webhook`
3. MercadoPago notificará automáticamente
4. No requiere polling

### **Cuentas de Prueba**

Las credenciales proporcionadas son de **PRUEBA (Sandbox)**:
- Solo funcionan con tarjetas de prueba
- No procesan pagos reales
- Perfectas para desarrollo

Para producción necesitarás:
- Credenciales productivas de MercadoPago
- Certificación de seguridad (HTTPS)
- Cuenta empresarial validada

---

## 🎉 ¡LISTO!

Todo está implementado y listo para probar. Ejecuta:

```powershell
.\test-mercadopago-flow.ps1
```

Y sigue las instrucciones en pantalla. 🚀

---

## 📞 SOPORTE

Si tienes problemas, revisa:
1. Este documento (IMPLEMENTACION_MERCADOPAGO.md)
2. Diagnóstico completo (DIAGNOSTICO_FLUJO_PAGO_Y_QR.md)
3. Logs de los servicios
4. Consola del navegador (F12)
