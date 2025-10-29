# 🔔 Guía del Webhook de MercadoPago

## 📋 Resumen

El webhook permite que **MercadoPago notifique automáticamente** a tu aplicación cuando cambia el estado de un pago (aprobado, rechazado, etc.), sin necesidad de que el usuario complete el flujo en el navegador.

---

## 🔄 Flujo Completo del Webhook

```
1. Usuario hace checkout
   └─> order-service crea orden (status: PENDING_PAYMENT)
   └─> payment-service crea preferencia en MercadoPago
   └─> Usuario redirigido a MercadoPago

2. Usuario completa el pago en MercadoPago
   └─> MercadoPago procesa el pago
   └─> MercadoPago envía notificación al webhook

3. payment-service recibe webhook
   └─> Consulta estado del pago en MercadoPago
   └─> Actualiza registro en payment_service_db
   └─> Notifica a order-service

4. order-service actualiza la orden
   └─> Cambia status de PENDING_PAYMENT a PAID
   └─> Libera recursos/tickets
```

---

## 🛠️ Implementación

### 1. Configuración del Webhook URL

**Archivo**: `payment-service/application.properties`

```properties
# Webhook Configuration
mercadopago.webhook.url=${WEBHOOK_URL:http://localhost:8087/api/payments/webhook}
```

**Producción**: Debe ser una URL HTTPS pública accesible por MercadoPago
```
https://tu-dominio.com/api/payments/webhook
```

**Sandbox/Desarrollo**: Puede usar HTTP + ngrok/localtunnel
```bash
# Opción 1: ngrok (requiere instalación)
ngrok http 8087
# Copiar URL forwarding: https://abc123.ngrok.io

# Opción 2: localtunnel (requiere npm)
npm install -g localtunnel
lt --port 8087 --subdomain packedgo-payment
# URL: https://packedgo-payment.loca.lt
```

### 2. Endpoint del Webhook

**URL**: `POST /api/payments/webhook`

**Headers enviados por MercadoPago**:
```
Content-Type: application/json
x-signature: <firma_de_seguridad>
x-request-id: <id_unico>
```

**Body enviado por MercadoPago**:
```json
{
  "action": "payment.created",
  "api_version": "v1",
  "data": {
    "id": "123456789"  // ID del pago en MercadoPago
  },
  "date_created": "2025-10-28T00:00:00Z",
  "id": 987654321,
  "live_mode": false,
  "type": "payment",
  "user_id": "293239737"
}
```

**Parámetros de Query**:
- `adminId`: ID del administrador (añadido por nuestra aplicación)

**Ejemplo completo**:
```
POST https://tu-dominio.com/api/payments/webhook?adminId=1
```

### 3. Procesamiento del Webhook

**Archivo**: `PaymentService.java`

```java
@Transactional
public void processWebhookNotification(Long adminId, Long paymentId) {
    // 1. Obtener credenciales del admin
    AdminCredential credential = credentialService.getValidatedCredentials(adminId);
    MercadoPagoConfig.setAccessToken(credential.getAccessToken());

    // 2. Consultar estado del pago en MercadoPago
    PaymentClient client = new PaymentClient();
    com.mercadopago.resources.payment.Payment mpPayment = client.get(paymentId);

    // 3. Buscar pago en nuestra BD por external_reference (orderId)
    Payment payment = paymentRepository
            .findByOrderId(mpPayment.getExternalReference())
            .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

    // 4. Actualizar estado del pago
    payment.setStatus(mapMercadoPagoStatus(mpPayment.getStatus()));
    payment.setMpPaymentId(mpPayment.getId());
    payment.setPaymentMethod(mpPayment.getPaymentMethodId());
    paymentRepository.save(payment);

    // 5. Notificar a order-service
    if (newStatus == PaymentStatus.APPROVED) {
        orderServiceClient.notifyPaymentApproved(payment.getOrderId(), mpPayment.getId());
    }
}
```

### 4. Notificación a Order-Service

**Archivo**: `OrderServiceClient.java`

```java
public boolean notifyPaymentApproved(String orderNumber, Long paymentId) {
    String url = orderServiceUrl + "/api/orders/payment-callback";
    
    Map<String, Object> request = Map.of(
            "orderNumber", orderNumber,
            "mpPaymentId", paymentId,
            "paymentStatus", "APPROVED");

    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            url, HttpMethod.POST, new HttpEntity<>(request), Map.class);

    return response.getStatusCode().is2xxSuccessful();
}
```

### 5. Actualización de la Orden

**Archivo**: `OrderServiceImpl.java` (order-service)

```java
@Transactional
public void updateOrderFromPaymentCallback(PaymentCallbackRequest request) {
    Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    if ("APPROVED".equals(request.getPaymentStatus())) {
        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentId(request.getMpPaymentId());
        // Liberar recursos, enviar confirmación, etc.
    }

    orderRepository.save(order);
}
```

---

## 🧪 Testing del Webhook

### Opción 1: Simulación Manual (Desarrollo)

```bash
# 1. Obtener un paymentId de prueba de MercadoPago
# (Se obtiene al completar un pago en sandbox)

# 2. Llamar manualmente al webhook
curl -X POST http://localhost:8087/api/payments/webhook?adminId=1 \
  -H "Content-Type: application/json" \
  -d '{
    "type": "payment",
    "data": {
      "id": "123456789"
    }
  }'
```

### Opción 2: Simulación desde la BD (Desarrollo)

```sql
-- Simular pago aprobado
UPDATE payments 
SET status='APPROVED', 
    mp_payment_id=123456789,
    approved_at=NOW()
WHERE order_id='ORD-202510-1761696313289';

-- Luego notificar manualmente a order-service
UPDATE orders 
SET status='PAID' 
WHERE order_number='ORD-202510-1761696313289';
```

### Opción 3: Testing Real con ngrok

```bash
# 1. Instalar ngrok
# Windows: choco install ngrok
# Mac: brew install ngrok
# Linux: snap install ngrok

# 2. Exponer payment-service
ngrok http 8087

# 3. Actualizar .env con la URL de ngrok
WEBHOOK_URL=https://abc123.ngrok.io/api/payments/webhook

# 4. Reiniciar payment-service
docker-compose restart payment-service-app

# 5. Crear nueva orden y completar pago en MercadoPago
# El webhook se llamará automáticamente
```

---

## 🔒 Seguridad del Webhook

### 1. Validación de Firma (Recomendado para Producción)

MercadoPago envía una firma en el header `x-signature` para validar que la petición es legítima:

```java
@PostMapping("/webhook")
public ResponseEntity<?> handleWebhook(
        @RequestHeader(value = "x-signature", required = false) String signature,
        @RequestHeader(value = "x-request-id", required = false) String requestId,
        @RequestBody WebhookNotification notification) {
    
    // Validar firma (implementar según docs de MercadoPago)
    if (!validateSignature(signature, requestId, notification)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid signature"));
    }
    
    // Procesar webhook...
}
```

### 2. Idempotencia

MercadoPago puede enviar el mismo webhook múltiples veces. Asegúrate de:

- Usar transacciones `@Transactional`
- Verificar estado anterior antes de actualizar
- Usar `mp_payment_id` como clave única

```java
// Verificar si ya fue procesado
if (payment.getMpPaymentId() != null && payment.getMpPaymentId().equals(mpPaymentId)) {
    log.info("Webhook ya procesado anteriormente");
    return ResponseEntity.ok(Map.of("status", "already_processed"));
}
```

### 3. Rate Limiting

Implementar rate limiting para evitar abuso:

```java
@RateLimiter(name = "webhookLimiter", fallbackMethod = "webhookRateLimitFallback")
@PostMapping("/webhook")
public ResponseEntity<?> handleWebhook(...) {
    // Procesar webhook
}
```

---

## 📊 Logs del Webhook

**Logs exitosos**:
```
2025-10-29 00:27:15 - Procesando webhook para admin: 1, payment: 123456789
2025-10-29 00:27:15 - Pago de MercadoPago obtenido: ID=123456789, Status=approved
2025-10-29 00:27:15 - Webhook procesado. Pago 5 actualizado: PENDING -> APPROVED
2025-10-29 00:27:15 - Notificando aprobación de pago a order-service: orderId=ORD-202510-1761696313289
2025-10-29 00:27:15 - Order-service notificado exitosamente para orden: ORD-202510-1761696313289
```

**Logs de error**:
```
2025-10-29 00:27:15 - ERROR - Pago no encontrado en BD: external_reference=ORD-INVALID
2025-10-29 00:27:15 - ERROR - Error procesando webhook: MercadoPago API error
```

---

## 🐛 Troubleshooting

### Problema: Webhook no se llama

**Causas**:
1. URL del webhook no es HTTPS (en producción)
2. URL no es accesible públicamente
3. Firewall bloqueando peticiones de MercadoPago

**Solución**:
```bash
# Verificar que el webhook esté configurado
docker exec -it payment-service-app cat application.properties | grep webhook

# Probar accesibilidad
curl https://tu-dominio.com/api/payments/webhook

# Ver logs de payment-service
docker logs payment-service-app --tail 100
```

### Problema: Webhook se llama pero falla

**Causas**:
1. Credenciales incorrectas
2. Payment ID no existe
3. Error de red con MercadoPago API

**Solución**:
```bash
# Verificar credenciales en BD
docker exec -it payment-service-db psql -U postgres -d payment_service_db \
  -c "SELECT admin_id, is_active, is_sandbox FROM admin_credentials WHERE admin_id=1;"

# Verificar logs detallados
docker logs payment-service-app | grep "ERROR"
```

### Problema: Orden no se actualiza

**Causas**:
1. order-service no es accesible desde payment-service
2. orderNumber incorrecto
3. Endpoint de callback no existe

**Solución**:
```bash
# Verificar conectividad
docker exec -it payment-service-app ping order-service

# Verificar endpoint
curl -X POST http://localhost:8084/api/orders/payment-callback \
  -H "Content-Type: application/json" \
  -d '{"orderNumber":"ORD-202510-123","paymentStatus":"APPROVED","mpPaymentId":123}'

# Ver logs de order-service
docker logs back-order-service-1 --tail 50
```

---

## 📝 Checklist de Producción

- [ ] Configurar URL HTTPS pública para webhook
- [ ] Habilitar validación de firma de MercadoPago
- [ ] Implementar rate limiting
- [ ] Configurar alertas para webhooks fallidos
- [ ] Implementar retry logic
- [ ] Configurar monitoreo de logs
- [ ] Probar con credenciales de producción de MercadoPago
- [ ] Documentar URLs de webhook en panel de MercadoPago
- [ ] Configurar CORS adecuadamente
- [ ] Habilitar SSL/TLS

---

## 🔗 Referencias

- [MercadoPago Webhooks Documentation](https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks)
- [MercadoPago Signature Validation](https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks/validate-notifications)
- [Testing Webhooks with ngrok](https://ngrok.com/docs)

---

## 💡 Notas Importantes

1. **External Reference**: Usamos `orderId` (ej: `ORD-202510-1761696313289`) como `external_reference` en MercadoPago para vincular pagos con órdenes.

2. **AdminId en Query**: Agregamos `?adminId=1` al webhook URL para identificar qué credenciales usar al consultar MercadoPago.

3. **Sandbox vs Producción**: 
   - Sandbox acepta HTTP
   - Producción requiere HTTPS obligatoriamente

4. **Reintentos**: MercadoPago reintenta enviar el webhook durante 8 horas si falla.

5. **Timeout**: Debes responder al webhook en menos de 10 segundos o MercadoPago lo considerará fallido.
