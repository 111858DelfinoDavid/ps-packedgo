# PAYMENT-SERVICE (Stripe Integration)

Microservicio de gestión de pagos integrado con **Stripe** para el sistema PackedGo.

## Descripción

Este microservicio proporciona una pasarela de pagos segura que permite procesar pagos a través de Stripe Checkout. Gestiona la creación de sesiones de pago, verificación de estados y recepción de webhooks para confirmar transacciones.

## Tecnologías

- **Java 17**
- **Spring Boot 3.5.7**
- **Stripe Java SDK**
- **Spring Security (JWT)**
- **PostgreSQL**
- **Lombok**

## Arquitectura

```
payment-service/
 controller/
    PaymentController.java       # Endpoints API
    StripeWebhookController.java # Webhooks Stripe
 service/
    PaymentService.java          # Lógica de negocio
 dto/
    PaymentRequest.java
    PaymentResponse.java
    PaymentStatsDTO.java
 entity/
    Payment.java                 # Entidad JPA
 security/
    JwtTokenValidator.java       # Validación JWT
 config/
     StripeConfig.java            # Configuración Stripe
```

## API Endpoints

### Gestión de Pagos

**POST** `/payments/create-checkout-stripe`
Crea una sesión de Checkout en Stripe.
```json
Headers: Authorization: Bearer {token}
Body: {
  "adminId": 1,
  "orderId": "ORD-202510-123",
  "amount": 1500.00,
  "currency": "usd",
  "successUrl": "https://myapp.com/success",
  "cancelUrl": "https://myapp.com/cancel"
}
Response: 201 CREATED
{
  "checkoutUrl": "https://checkout.stripe.com/c/pay/...",
  "sessionId": "cs_test_...",
  "paymentId": 123
}
```

**POST** `/payments/verify/{orderId}`
Verificación manual del estado de un pago (útil si el webhook falla).
```json
Response: 200 OK
{
  "status": "PAID",
  "orderId": "ORD-202510-123"
}
```

**GET** `/payments/stats`
Obtiene estadísticas de pagos para el administrador autenticado.
```json
Headers: Authorization: Bearer {token}
Response: 200 OK
{
  "totalRevenue": 5000.00,
  "successfulPayments": 45,
  "pendingPayments": 2
}
```

### Webhooks

**POST** `/api/webhooks/stripe`
Endpoint público para recibir notificaciones de Stripe.
- Evento manejado: `checkout.session.completed`
- Valida la firma `Stripe-Signature`

### Health Check

**GET** `/payments/health`
```json
{
  "status": "UP",
  "service": "payment-gateway",
  "version": "2.0.0",
  "provider": "Stripe"
}
```

## Configuración

Variables de entorno requeridas en `.env` o `application.properties`:

```properties
# Server
SERVER_PORT=8085

# Stripe Keys
STRIPE_API_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Database
DB_URL=jdbc:postgresql://payment-db:5432/payment_db
DB_USERNAME=payment_user
DB_PASSWORD=payment_password

# JWT
JWT_SECRET=your_jwt_secret_key
```

## Flujo de Pago

1. **Frontend** envía solicitud de pago a `/payments/create-checkout-stripe`.
2. **Payment Service** crea una sesión en Stripe y devuelve la URL de checkout.
3. **Frontend** redirige al usuario a la página de pago de Stripe.
4. **Usuario** completa el pago.
5. **Stripe** redirige al usuario a la `successUrl`.
6. **Stripe** envía un webhook asíncrono a `/api/webhooks/stripe`.
7. **Payment Service** valida la firma del webhook y actualiza el estado del pago a `PAID`.
8. **Payment Service** notifica al `order-service` (vía HTTP o cola) para confirmar la orden.

## Manejo de Errores

- **401 Unauthorized**: Token JWT inválido o faltante.
- **400 Bad Request**: Error en la firma del webhook o datos inválidos.
- **500 Internal Server Error**: Error de comunicación con Stripe.

## Testing

Para probar los webhooks localmente, usar Stripe CLI:

```bash
stripe listen --forward-to localhost:8085/api/webhooks/stripe
```
