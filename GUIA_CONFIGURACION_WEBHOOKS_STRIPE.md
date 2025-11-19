# 🎯 GUÍA COMPLETA: Configurar Webhooks de Stripe

## ❓ ¿Por qué necesitas webhooks SI YA tienes successUrl?

### successUrl vs Webhooks

| Aspecto | successUrl | Webhook |
|---------|-----------|---------|
| **Propósito** | Redirigir al usuario | Notificar a tu backend |
| **Confiabilidad** | ❌ Usuario puede cerrar ventana | ✅ Stripe garantiza entrega |
| **Seguridad** | ⚠️ Puede ser manipulado | ✅ Firma criptográfica verificada |
| **Actualiza tu DB** | ❌ No automáticamente | ✅ Sí, en StripeWebhookController |
| **Orden del flujo** | Usuario ve "éxito" | Backend confirma pago real |

### ⚠️ Problema sin webhook:
```
Usuario paga con tarjeta → Stripe procesa → Redirige a successUrl 
→ Frontend muestra "¡Pago exitoso!" 
→ PERO tu Payment en DB sigue como PENDING ❌
→ Order Service nunca se entera ❌
```

### ✅ Con webhook:
```
Usuario paga → Stripe procesa → 2 cosas en paralelo:
  1. Redirige a successUrl (usuario ve "éxito")
  2. Envía webhook a tu backend
     → StripeWebhookController recibe evento
     → PaymentService.handleStripePaymentSuccess()
     → Payment → APPROVED en DB ✅
     → Notifica a Order Service ✅
```

---

## 💰 ¿Es Gratis?

**SÍ, completamente GRATIS**. Los webhooks son parte del servicio de Stripe sin costo adicional.

---

## 🚀 OPCIÓN 1: Stripe CLI (RECOMENDADO para desarrollo local)

### Ventajas:
- ✅ No necesitas exponer tu localhost a internet
- ✅ Funciona inmediatamente
- ✅ Gratis y fácil de configurar
- ✅ Perfecto para testing

### Paso 1: Instalar Stripe CLI

**En Windows con Chocolatey:**
```powershell
choco install stripe-cli
```

**O descarga manual:**
1. Ir a: https://github.com/stripe/stripe-cli/releases/latest
2. Descargar: `stripe_X.X.X_windows_x86_64.zip`
3. Extraer y agregar al PATH

### Paso 2: Autenticar Stripe CLI
```powershell
stripe login
```
Esto abrirá tu navegador para autorizar el CLI.

### Paso 3: Configurar el webhook secret en payment-service

Copiar tu `STRIPE_SECRET_KEY` al `.env` del payment-service:

**Archivo:** `packedgo/back/payment-service/.env`
```bash
# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_51STBhICs02rkj5edqrxN1xrMKmSsRvSSD4oMhyXDFZUNzAw80mX0wgF1jy9xPcwvWKdg73YURXEqFaj4brIuNdbp00kucJwCdp
STRIPE_WEBHOOK_SECRET=whsec_temporal_stripe_cli_lo_generara
FRONTEND_URL=http://localhost:4200
```

### Paso 4: Iniciar servicios
```powershell
# Terminal 1: Levantar Docker con todos los servicios
cd C:\Users\Agustin\Documents\GitHub\PS-Agus\ps-packedgo\packedgo\back
docker-compose up -d --build

# Esperar que payment-service esté listo (puerto 8085)
```

### Paso 5: Iniciar Stripe CLI listener
```powershell
# Terminal 2: Stripe CLI escucha y reenvía webhooks
stripe listen --forward-to http://localhost:8085/api/webhooks/stripe
```

**Salida esperada:**
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxx
```

### Paso 6: Copiar el webhook secret
1. Copia el `whsec_...` que apareció
2. Actualiza tu `.env`:
```bash
STRIPE_WEBHOOK_SECRET=whsec_el_que_copiaste_aqui
```
3. Reinicia payment-service:
```powershell
docker-compose restart payment-service
```

### Paso 7: ¡Probar!
1. Crea un pago desde tu frontend
2. Completa el checkout en Stripe con tarjeta `4242 4242 4242 4242`
3. En la terminal de Stripe CLI verás:
```
[200] POST http://localhost:8085/api/webhooks/stripe [evt_xxx]
checkout.session.completed
```
4. En los logs de payment-service verás:
```
🔷 Webhook recibido de Stripe
✅ Firma de webhook verificada
🔷 Procesando evento checkout.session.completed
✅ Pago procesado exitosamente
```

---

## 🌐 OPCIÓN 2: Dashboard de Stripe (Para testing sin CLI)

### Ventajas:
- ✅ No requiere instalar nada
- ✅ Puedes simular webhooks manualmente
- ⚠️ Requiere exponer localhost (con ngrok) o desplegar a servidor

### Paso 1: Exponer tu localhost (con ngrok)

**Instalar ngrok:**
```powershell
choco install ngrok
```

**Crear túnel:**
```powershell
ngrok http 8085
```

**Salida:**
```
Forwarding: https://abcd1234.ngrok.io -> http://localhost:8085
```

### Paso 2: Crear webhook en Stripe Dashboard

1. Ir a: https://dashboard.stripe.com/test/webhooks
2. Click **"Add endpoint"**
3. Configurar:
   - **Endpoint URL:** `https://abcd1234.ngrok.io/api/webhooks/stripe` (usa tu URL de ngrok)
   - **Events to send:** Selecciona `checkout.session.completed`
   - Click **"Add endpoint"**

### Paso 3: Copiar Signing Secret

1. En el webhook recién creado, click **"Reveal"** en "Signing secret"
2. Copiar el valor `whsec_...`
3. Actualizar `.env`:
```bash
STRIPE_WEBHOOK_SECRET=whsec_el_valor_copiado
```
4. Reiniciar payment-service

### Paso 4: Probar

Crea un pago y verás en el Dashboard → Webhooks → el evento `checkout.session.completed` marcado como exitoso.

---

## 🔧 Configuración Final en payment-service

### Archivo: `.env`
```bash
# Stripe Configuration (COPIAR desde stripe/stripe/.env)
STRIPE_SECRET_KEY=sk_test_51STBhICs02rkj5edqrxN1xrMKmSsRvSSD4oMhyXDFZUNzAw80mX0wgF1jy9xPcwvWKdg73YURXEqFaj4brIuNdbp00kucJwCdp

# Webhook Secret (generado por Stripe CLI o Dashboard)
STRIPE_WEBHOOK_SECRET=whsec_... # Lo obtienes de stripe listen o del Dashboard

# Frontend URL (para redirect después del pago)
FRONTEND_URL=http://localhost:4200
```

### Verificar en application.properties
Debe tener:
```properties
stripe.secret.key=${STRIPE_SECRET_KEY:}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET:}
frontend.url=${FRONTEND_URL:http://localhost:4200}
```

---

## 🧪 Testing: Flujo Completo

### Paso 1: Verificar servicios
```powershell
# Verificar que payment-service está corriendo
curl http://localhost:8085/api/actuator/health

# Verificar logs
docker logs payment-service -f
```

### Paso 2: Crear checkout
```bash
POST http://localhost:8085/api/payments/create-checkout-stripe
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "orderId": "TEST-ORDER-001",
  "amount": 1500.00,
  "description": "Test Stripe Payment",
  "adminId": 1
}
```

**Respuesta:**
```json
{
  "id": 123,
  "orderId": "TEST-ORDER-001",
  "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_...",
  "status": "PENDING",
  "paymentProvider": "STRIPE"
}
```

### Paso 3: Completar pago
1. Abrir `checkoutUrl` en navegador
2. Usar tarjeta de prueba: `4242 4242 4242 4242`
3. Fecha: cualquier futura (ej: 12/30)
4. CVC: 123
5. Click **"Pay"**

### Paso 4: Verificar webhook
En terminal de `stripe listen`:
```
[200] POST /api/webhooks/stripe [evt_xxx]
checkout.session.completed
```

En logs de payment-service:
```
🔷 Webhook recibido de Stripe
✅ Firma de webhook verificada. Tipo de evento: checkout.session.completed
🔷 Procesando evento checkout.session.completed
✅ Sesión completada: cs_test_...
🔷 Manejando pago exitoso de Stripe para sessionId: cs_test_...
✅ Pago Stripe aprobado. PaymentId: 123, PaymentIntentId: pi_...
✅ Estado de pago cambió, notificando a Order Service
```

### Paso 5: Verificar en base de datos
```sql
SELECT 
  id, 
  order_id, 
  amount, 
  status, 
  payment_provider,
  stripe_session_id,
  stripe_payment_intent_id,
  approved_at
FROM payments 
WHERE stripe_session_id IS NOT NULL
ORDER BY created_at DESC 
LIMIT 1;
```

**Resultado esperado:**
```
id  | order_id        | amount | status   | payment_provider | approved_at
123 | TEST-ORDER-001  | 1500   | APPROVED | STRIPE           | 2025-11-18 ...
```

---

## 📊 Comparación de Opciones

| Aspecto | Stripe CLI | Dashboard + ngrok |
|---------|-----------|-------------------|
| **Setup** | 5 minutos | 10 minutos |
| **Instalación** | Solo CLI | CLI + ngrok |
| **Internet necesario** | ❌ No | ✅ Sí |
| **Webhook secret** | Se regenera cada vez | Permanente |
| **Para desarrollo** | ⭐ IDEAL | Funciona |
| **Para producción** | ❌ No | ✅ Con dominio real |

---

## 🎯 Recomendación

**Para desarrollo local:** Usa **Stripe CLI** (Opción 1)
- Más simple
- No expones tu localhost
- Logs en tiempo real

**Comando rápido:**
```powershell
# Terminal 1: Servicios
cd C:\Users\Agustin\Documents\GitHub\PS-Agus\ps-packedgo\packedgo\back
docker-compose up -d

# Terminal 2: Stripe listener
stripe listen --forward-to http://localhost:8085/api/webhooks/stripe

# Copiar el whsec_... que aparece y ponerlo en .env
# Reiniciar payment-service
docker-compose restart payment-service
```

---

## 🚨 Errores Comunes

### Error: "No signatures found matching the expected signature"
**Causa:** El `STRIPE_WEBHOOK_SECRET` no coincide
**Solución:** 
1. Si usas Stripe CLI: copiar el `whsec_...` que aparece en `stripe listen`
2. Si usas Dashboard: copiar desde el webhook endpoint

### Error: "Missing Stripe-Signature header"
**Causa:** El request no viene de Stripe
**Solución:** Asegúrate de que Stripe CLI o el Dashboard están enviando el webhook

### Error: "Pago no encontrado para sessionId"
**Causa:** El Payment no se guardó con el `stripeSessionId`
**Solución:** Verificar que `createPaymentWithStripe()` está guardando correctamente

---

## ✅ Checklist de Configuración

- [ ] Instalar Stripe CLI: `choco install stripe-cli`
- [ ] Autenticar: `stripe login`
- [ ] Copiar `STRIPE_SECRET_KEY` de `stripe/stripe/.env` a `payment-service/.env`
- [ ] Iniciar servicios: `docker-compose up -d`
- [ ] Iniciar listener: `stripe listen --forward-to http://localhost:8085/api/webhooks/stripe`
- [ ] Copiar `whsec_...` generado a `STRIPE_WEBHOOK_SECRET` en `.env`
- [ ] Reiniciar: `docker-compose restart payment-service`
- [ ] Probar creando un pago y completándolo en Stripe
- [ ] Verificar logs de webhook en ambas terminales
- [ ] Verificar Payment en DB con status APPROVED

---

**¡Listo para usar Stripe con webhooks funcionando!** 🎉
