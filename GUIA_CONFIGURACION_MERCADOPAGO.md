# 🔧 GUÍA DE CONFIGURACIÓN - MERCADOPAGO CON WEBHOOKS

## 📋 RESUMEN DEL PROBLEMA

El flujo de pago con MercadoPago **NO funciona automáticamente** porque:
1. ❌ El webhook no está configurado
2. ❌ MercadoPago no puede notificar cuando un pago se aprueba
3. ❌ Los tickets con QR no se generan automáticamente

## ✅ SOLUCIONES DISPONIBLES

### **OPCIÓN 1: Webhook con ngrok (RECOMENDADO para desarrollo)**

Esta es la mejor solución para que el flujo funcione automáticamente.

#### Paso 1: Instalar ngrok

```powershell
# Con winget (recomendado)
winget install ngrok

# O descargar de https://ngrok.com/download
```

#### Paso 2: Iniciar ngrok

```powershell
# Exponer el puerto 8085 (Payment Service)
ngrok http 8085
```

**Resultado esperado:**
```
Session Status                online
Account                       [tu cuenta]
Forwarding                    https://abc123.ngrok-free.app -> http://localhost:8085
```

#### Paso 3: Copiar la URL HTTPS

Copia la URL que aparece en "Forwarding" (ejemplo: `https://abc123.ngrok-free.app`)

#### Paso 4: Configurar el webhook

**Opción A: Usar .env.local (para desarrollo local)**
```bash
cd packedgo/back/payment-service
cp .env.local .env
```

Editar `.env` y agregar:
```properties
WEBHOOK_URL=https://abc123.ngrok-free.app/api/payments/webhook
```

**Opción B: Editar .env directamente**
```properties
# packedgo/back/payment-service/.env
WEBHOOK_URL=https://TU-URL-DE-NGROK.ngrok-free.app/api/payments/webhook
```

#### Paso 5: Reiniciar Payment Service

```powershell
cd packedgo\back\payment-service
.\mvnw spring-boot:run
```

#### Paso 6: Verificar la configuración

El log debe mostrar:
```
Webhook configurado: https://abc123.ngrok-free.app/api/payments/webhook?adminId=X
```

---

### **OPCIÓN 2: Verificación Manual (YA IMPLEMENTADO)**

Si no puedes configurar webhooks, el sistema tiene un mecanismo de verificación manual que **ya está funcionando**.

#### ¿Cómo funciona?

1. El usuario paga en MercadoPago ✅
2. Es redirigido a `/order-success` ✅
3. El frontend **automáticamente** verifica el pago después de 2 segundos ✅
4. Si el pago fue aprobado, actualiza el estado y genera tickets ✅

#### Código implementado:

**Frontend:** `order-success.component.ts`
```typescript
verifyPendingPayments(orders: any[]): void {
  setTimeout(() => {
    // Verifica cada orden pendiente
    orders.forEach(order => {
      this.paymentService.verifyPaymentStatus(order.orderNumber)
        .subscribe(/* ... */);
    });
  }, 2000);
}
```

**Backend:** `POST /api/payments/verify/{orderId}`
- Consulta MercadoPago
- Actualiza el estado del pago
- Notifica a Order Service
- Genera tickets automáticamente

#### Limitación:

⚠️ **Solo funciona si el usuario completó el pago en MercadoPago**
- Si el usuario no pagó, no hay nada que verificar
- Si cerró la ventana antes de pagar, debe volver a intentarlo

---

## 🧪 CÓMO PROBAR

### Preparación

1. **Verificar que todos los servicios estén corriendo:**
   ```powershell
   .\verify-services.ps1
   ```

2. **Si usas webhooks, iniciar ngrok:**
   ```powershell
   ngrok http 8085
   ```

3. **Configurar webhook en .env (si usas ngrok)**

4. **Iniciar todos los servicios:**
   ```powershell
   # En terminales separadas:
   cd packedgo\back\auth-service; .\mvnw spring-boot:run
   cd packedgo\back\users-service; .\mvnw spring-boot:run
   cd packedgo\back\order-service; .\mvnw spring-boot:run
   cd packedgo\back\payment-service; .\mvnw spring-boot:run
   cd packedgo\back\event-service; .\mvnw spring-boot:run
   cd packedgo\front-angular; ng serve
   ```

### Flujo de prueba

1. **Ir a** `http://localhost:4200`

2. **Login como cliente:**
   - Email: `consumer@test.com`
   - Password: `password123`

3. **Buscar eventos disponibles**

4. **Agregar al carrito y hacer checkout**

5. **Hacer clic en "Pagar con MercadoPago"**

6. **En MercadoPago:**
   - Usar tarjeta de prueba: `5031 7557 3453 0604`
   - CVV: `123`
   - Fecha: Cualquier fecha futura
   - Nombre: Cualquiera

7. **Esperar redirección automática**

8. **El sistema debe:**
   - ✅ Detectar el pago pendiente
   - ✅ Verificar en MercadoPago (después de 2 segundos)
   - ✅ Actualizar el estado a PAID
   - ✅ Generar tickets con QR
   - ✅ Mostrar los tickets en pantalla

---

## 🔍 VERIFICACIÓN DE LOGS

### En Payment Service:

**Con webhook configurado:**
```
Webhook configurado: https://abc123.ngrok-free.app/api/payments/webhook
```

**Cuando se recibe notificación:**
```
POST /api/payments/webhook - Type: payment, Data: {...}
Procesando webhook para MercadoPago payment: 123456789
Orden ORD-202511-XXX actualizada exitosamente en order-service
```

**Con verificación manual:**
```
POST /api/payments/verify/ORD-202511-XXX
🔍 Verificando estado de pago para orden: ORD-202511-XXX
Consultando MercadoPago con mpPaymentId: 123456789
Estado anterior: PENDING → Estado nuevo: APPROVED
✅ Estado de pago cambió, notificando a Order Service
```

### En Order Service:

```
Updating order ORD-202511-XXX with payment status: APPROVED
Order ORD-202511-XXX marked as PAID
🎟️ Generating tickets for order: ORD-202511-XXX
✅ Ticket #1 generated: ID=123, QR=data:image/png;base64,...
```

---

## 🚨 TROUBLESHOOTING

### Problema: "El pago no tiene mpPaymentId todavía"

**Causa:** El usuario no completó el pago en MercadoPago
**Solución:** El usuario debe volver a hacer checkout y completar el pago

### Problema: "Webhook URL no configurada"

**Causa:** La variable `WEBHOOK_URL` está vacía
**Solución:** 
1. Iniciar ngrok: `ngrok http 8085`
2. Copiar URL HTTPS
3. Configurar en `.env`
4. Reiniciar Payment Service

### Problema: "No se generan tickets"

**Causa:** El Order Service no recibe la notificación del pago
**Solución:**
1. Verificar que Order Service esté corriendo en puerto 8084
2. Verificar logs de Payment Service
3. Probar endpoint manualmente:
   ```powershell
   Invoke-RestMethod -Uri "http://localhost:8085/api/payments/verify/ORD-202511-XXX" `
     -Method POST `
     -Headers @{"Authorization"="Bearer TOKEN"}
   ```

### Problema: "Error de conexión con MercadoPago"

**Causa:** Credenciales inválidas o expiradas
**Solución:**
1. Verificar credenciales en `.env`
2. Obtener nuevas credenciales en: https://www.mercadopago.com.ar/developers
3. Asegurar que sean credenciales de **TEST** (sandbox)

---

## 📝 NOTAS IMPORTANTES

1. **ngrok gratuito cambia la URL cada vez que se reinicia**
   - Debes actualizar `WEBHOOK_URL` cada vez
   - Para URL fija, necesitas cuenta paga de ngrok

2. **Las credenciales de prueba expiran**
   - Regenerar periódicamente en el panel de MercadoPago

3. **El webhook solo funciona con HTTPS**
   - En desarrollo: usar ngrok
   - En producción: tener dominio con SSL

4. **La verificación manual es un backup**
   - Funciona si completaste el pago
   - No reemplaza completamente el webhook

---

## ✅ CHECKLIST DE CONFIGURACIÓN

- [ ] PostgreSQL corriendo en puerto 5432
- [ ] Auth Service corriendo en puerto 8081
- [ ] Users Service corriendo en puerto 8082
- [ ] Order Service corriendo en puerto 8084
- [ ] Payment Service corriendo en puerto 8085
- [ ] Event Service corriendo en puerto 8086
- [ ] Frontend corriendo en puerto 4200
- [ ] (Opcional) ngrok exponiendo puerto 8085
- [ ] (Opcional) WEBHOOK_URL configurado en .env
- [ ] Credenciales de MercadoPago válidas

---

## 🎯 RESULTADO ESPERADO

Con todo configurado correctamente:

1. Usuario hace checkout → Crea orden PENDING_PAYMENT
2. Usuario paga en MercadoPago → Aprueba el pago
3. **Con webhook:** MercadoPago notifica → Payment Service actualiza → Order Service genera tickets
4. **Sin webhook:** Usuario regresa → Frontend verifica → Payment Service actualiza → Order Service genera tickets
5. Usuario ve sus tickets con QR en pantalla ✅

---

¿Necesitas ayuda? Verifica los logs de cada servicio para identificar dónde está fallando el flujo.
