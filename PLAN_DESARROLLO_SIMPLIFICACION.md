# 🚀 Plan de Desarrollo - Simplificación PackedGo

## 📋 Información del Proyecto

**Proyecto:** PackedGo - Sistema de gestión de eventos  
**Rama:** feature/employee-dashboard  
**Fecha inicio:** 18 de Noviembre 2025  
**Objetivo:** Simplificar arquitectura eliminando complejidad innecesaria e integrar Stripe

---

## 🎯 Resumen Ejecutivo

Este plan detalla la **simplificación completa** del sistema PackedGo, eliminando ~1,300 líneas de código innecesario y reemplazando MercadoPago por Stripe.

**Beneficios esperados:**
- ✅ 60% menos código
- ✅ Flujo de pago predecible con webhooks funcionales
- ✅ Testing real sin workarounds
- ✅ Arquitectura defendible para tesis

---

## 📊 Estado del Plan

| Fase | Estado | Progreso | Estimación | Archivos |
|------|--------|----------|------------|----------|
| FASE 1: Stripe Integration | � EN PROGRESO | 85% | 4-6h | 15 archivos |
| FASE 2: Eliminar MultiOrderSession | 🔵 PENDIENTE | 0% | 6-8h | 20 archivos |
| FASE 3: Fusionar Consumption-Service | 🔵 PENDIENTE | 0% | 3-4h | 10 archivos |
| FASE 4: Simplificar Shopping Cart | 🔵 PENDIENTE | 0% | 2-3h | 5 archivos |

**Estados:** 🔵 PENDIENTE | 🟡 EN PROGRESO | 🟢 COMPLETADO | 🔴 BLOQUEADO

---

## ✅ PROGRESO FASE 1 (85% COMPLETADO)

### ✅ Completado:
- [x] Stripe SDK agregado al pom.xml (stripe-java 26.7.0)
- [x] StripeService.java creado con integración directa del SDK
- [x] DTOs creados: StripeCheckoutRequest, StripeCheckoutResponse
- [x] Payment entity actualizado con campos Stripe (stripeSessionId, stripePaymentIntentId, paymentProvider)
- [x] PaymentRepository actualizado con findByStripeSessionId()
- [x] PaymentService modificado con createPaymentWithStripe() y handleStripePaymentSuccess()
- [x] StripeWebhookController creado para manejar webhooks
- [x] PaymentController actualizado con endpoint /create-checkout-stripe
- [x] application.properties configurado con Stripe
- [x] .env actualizado con claves de Stripe
- [x] Stripe CLI instalado y autenticado
- [x] Webhook listener activo (whsec_8c0d91651ba797412266b4297c822f5123bfb978454f16b5328628e5b0abcec8)
- [x] payment-service reiniciado con nueva configuración

### 🔄 Completados recientemente:
- [x] PaymentServiceClient actualizado con método createPaymentStripe()
- [x] OrderServiceImpl modificado para usar Stripe en lugar de MercadoPago
- [x] PaymentServiceResponse actualizado con campos de Stripe
- [x] PaymentResponse actualizado con campos de Stripe
- [x] Servicios recompilados sin errores
- [x] Docker containers reconstruyéndose con cambios

### 🔄 Pendiente para completar Fase 1:
- [ ] Verificar que servicios arranquen correctamente
- [ ] Testing end-to-end del flujo completo
- [ ] Verificar webhook funciona correctamente

---

# FASE 1: INTEGRACIÓN DE STRIPE (CRÍTICO)

## 📦 Paso 1.1: Preparar Stripe API Service

### Archivos a modificar:
```
stripe/stripe/
├── src/main/java/com/packegoapi/stripe/
│   ├── config/
│   │   ├── StripeConfiguration.java        [✅ YA EXISTE]
│   │   └── CorsConfiguration.java          [✅ YA EXISTE]
│   ├── controller/
│   │   ├── StripeController.java           [✅ YA EXISTE]
│   │   └── WebhookController.java          [✅ YA EXISTE]
│   ├── service/
│   │   └── StripeService.java              [✅ YA EXISTE]
│   └── dto/
│       ├── CreateCheckoutRequest.java      [✅ YA EXISTE]
│       └── CheckoutResponse.java           [✅ YA EXISTE]
```

### ✅ Acciones:
1. **VERIFICAR** que todos los archivos Stripe existen y compilan
2. **CONFIGURAR** variables de entorno en `stripe/.env`:
   ```properties
   STRIPE_SECRET_KEY=sk_test_51...
   STRIPE_PUBLISHABLE_KEY=pk_test_51...
   BASE_URL=http://localhost:8081
   FRONTEND_URL=http://localhost:3000
   SERVER_PORT=8081
   ```
3. **PROBAR** que Stripe API arranca: `cd stripe/stripe && mvn spring-boot:run`

**Checkpoint:** ✅ Stripe API corre en puerto 8081

---

## 📦 Paso 1.2: Agregar Stripe API a Docker Compose

### Archivo: `packedgo/back/docker-compose.yml`

### ✅ Acción: Agregar servicio stripe-api

**UBICACIÓN:** Después del servicio `payment-service` y antes de `networks:`

```yaml
  stripe-api:
    build:
      context: ../../../stripe/stripe
      dockerfile: Dockerfile
    container_name: stripe-api
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}
      - STRIPE_PUBLISHABLE_KEY=${STRIPE_PUBLISHABLE_KEY}
      - BASE_URL=http://stripe-api:8081
      - FRONTEND_URL=http://localhost:4200
      - CORS_ALLOWED_ORIGINS=http://localhost:4200,http://frontend:80
    networks:
      - packedgo-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

**IMPORTANTE:** Crear archivo `.env` en `packedgo/back/` con:
```bash
STRIPE_SECRET_KEY=sk_test_51...
STRIPE_PUBLISHABLE_KEY=pk_test_51...
```

**Checkpoint:** ✅ Docker Compose incluye stripe-api

---

## 📦 Paso 1.3: Crear StripeIntegrationService en Payment-Service

### Archivo NUEVO: `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/service/StripeIntegrationService.java`

### ✅ Acción: Crear servicio de integración

```java
package com.packedgo.payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import com.packedgo.payment_service.dto.StripeCheckoutRequest;
import com.packedgo.payment_service.dto.StripeCheckoutResponse;
import com.packedgo.payment_service.dto.PaymentRequest;
import com.packedgo.payment_service.exception.PaymentException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${stripe.api.url}")
    private String stripeApiUrl;
    
    @Value("${frontend.url}")
    private String frontendUrl;
    
    public StripeCheckoutResponse createPaymentSession(PaymentRequest paymentRequest) {
        log.info("Creating Stripe checkout session for order: {}", paymentRequest.getOrderId());
        
        // Mapear request interno a formato de Stripe API
        StripeCheckoutRequest stripeRequest = StripeCheckoutRequest.builder()
            .items(paymentRequest.getItems().stream()
                .map(item -> StripeCheckoutRequest.ItemDTO.builder()
                    .name(item.getName())
                    .description(item.getDescription())
                    .quantity(item.getQuantity())
                    .unitAmount(convertToCents(item.getUnitPrice()))
                    .currency("usd")
                    .build())
                .collect(Collectors.toList()))
            .successUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
            .cancelUrl(frontendUrl + "/payment/cancel")
            .customerEmail(paymentRequest.getCustomerEmail())
            .externalReference(paymentRequest.getOrderId())
            .build();
        
        String url = stripeApiUrl + "/create-checkout-session";
        
        try {
            StripeCheckoutResponse response = restTemplate.postForObject(
                url, stripeRequest, StripeCheckoutResponse.class);
            
            log.info("Stripe session created: {} for order: {}", 
                response.getSessionId(), paymentRequest.getOrderId());
            
            return response;
            
        } catch (RestClientException e) {
            log.error("Error creating Stripe session: {}", e.getMessage());
            throw new PaymentException("Error processing payment: " + e.getMessage());
        }
    }
    
    public StripeCheckoutResponse getSessionDetails(String sessionId) {
        String url = stripeApiUrl + "/session/" + sessionId;
        return restTemplate.getForObject(url, StripeCheckoutResponse.class);
    }
    
    private Long convertToCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
```

**Checkpoint:** ✅ StripeIntegrationService creado

---

## 📦 Paso 1.4: Crear DTOs para Stripe

### Archivos NUEVOS en `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/dto/`

### A) `StripeCheckoutRequest.java`

```java
package com.packedgo.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutRequest {
    
    private List<ItemDTO> items;
    private String successUrl;
    private String cancelUrl;
    private String customerEmail;
    private String externalReference;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemDTO {
        private String name;
        private String description;
        private Integer quantity;
        private Long unitAmount; // En centavos
        private String currency;
    }
}
```

### B) `StripeCheckoutResponse.java`

```java
package com.packedgo.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeCheckoutResponse {
    private String sessionId;
    private String url;
    private String status;
    private String paymentIntentId;
}
```

**Checkpoint:** ✅ DTOs creados

---

## 📦 Paso 1.5: Modificar PaymentService para usar Stripe

### Archivo: `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/service/PaymentService.java`

### ✅ Acción: REEMPLAZAR método `createPaymentPreference()` con nuevo método Stripe

**BUSCAR ESTE MÉTODO (líneas ~50-150):**
```java
@Transactional
public PaymentResponse createPaymentPreference(PaymentRequest request) {
    // ... TODO EL CÓDIGO DE MERCADOPAGO (150 líneas)
}
```

**REEMPLAZAR CON:**
```java
@Transactional
public PaymentResponse createPaymentWithStripe(PaymentRequest request) {
    log.info("Creating Stripe payment for order: {}", request.getOrderId());
    
    try {
        // 1. Crear el registro del pago en nuestra BD
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency("USD")
                .status(Payment.PaymentStatus.PENDING)
                .customerEmail(request.getCustomerEmail())
                .description(request.getDescription())
                .build();
        
        payment = paymentRepository.save(payment);
        log.info("Payment registered in DB with ID: {}", payment.getId());
        
        // 2. Crear sesión de Stripe
        StripeCheckoutResponse stripeResponse = stripeIntegrationService.createPaymentSession(request);
        
        // 3. Actualizar el pago con el sessionId de Stripe
        payment.setStripeSessionId(stripeResponse.getSessionId());
        paymentRepository.save(payment);
        
        log.info("Stripe session created: {} for order: {}", 
            stripeResponse.getSessionId(), request.getOrderId());
        
        // 4. Retornar respuesta
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .checkoutUrl(stripeResponse.getUrl())
                .sessionId(stripeResponse.getSessionId())
                .message("Payment session created successfully")
                .build();
        
    } catch (Exception e) {
        log.error("Error creating Stripe payment", e);
        throw new PaymentException("Error processing payment: " + e.getMessage());
    }
}
```

### ✅ Acción: AGREGAR campo stripeSessionId a Payment entity

**Archivo:** `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/model/Payment.java`

**AGREGAR después de la línea de `preferenceId`:**
```java
@Column(name = "stripe_session_id", length = 255)
private String stripeSessionId;

@Column(name = "stripe_payment_intent_id", length = 255)
private String stripePaymentIntentId;
```

**Checkpoint:** ✅ PaymentService usa Stripe

---

## 📦 Paso 1.6: Crear Webhook Handler para Stripe

### Archivo NUEVO: `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/controller/StripeWebhookController.java`

```java
package com.packedgo.payment_service.controller;

import com.packedgo.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        
        log.info("Received Stripe webhook");
        
        try {
            String eventType = (String) payload.get("type");
            
            if ("checkout.session.completed".equals(eventType)) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                
                String sessionId = (String) object.get("id");
                String paymentStatus = (String) object.get("payment_status");
                Map<String, Object> metadata = (Map<String, Object>) object.get("metadata");
                String orderId = metadata != null ? (String) metadata.get("externalReference") : null;
                
                log.info("Checkout completed for session: {}, order: {}, status: {}", 
                    sessionId, orderId, paymentStatus);
                
                if ("paid".equals(paymentStatus) && orderId != null) {
                    paymentService.handleStripePaymentSuccess(sessionId, orderId);
                }
            }
            
            return ResponseEntity.ok("Webhook processed");
            
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
}
```

### ✅ Acción: AGREGAR método `handleStripePaymentSuccess()` a PaymentService

**Archivo:** `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/service/PaymentService.java`

**AGREGAR al final de la clase:**
```java
@Transactional
public void handleStripePaymentSuccess(String sessionId, String orderId) {
    log.info("Processing Stripe payment success for order: {}", orderId);
    
    Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
    
    Payment.PaymentStatus previousStatus = payment.getStatus();
    
    // Actualizar estado del pago
    payment.setStatus(Payment.PaymentStatus.APPROVED);
    payment.setStripeSessionId(sessionId);
    payment.setApprovedAt(LocalDateTime.now());
    
    paymentRepository.save(payment);
    
    log.info("Payment {} updated: {} -> APPROVED", payment.getId(), previousStatus);
    
    // Notificar a order-service
    if (previousStatus != Payment.PaymentStatus.APPROVED) {
        notifyOrderService(payment, Payment.PaymentStatus.APPROVED, "paid");
    }
}
```

**Checkpoint:** ✅ Webhook handler creado

---

## 📦 Paso 1.7: Actualizar application.properties

### Archivo: `packedgo/back/payment-service/src/main/resources/application.properties`

### ✅ Acción: AGREGAR configuración de Stripe

**AGREGAR al final del archivo:**
```properties
# Stripe Configuration
stripe.api.url=http://stripe-api:8081/api/stripe
stripe.api.url.external=http://localhost:8081/api/stripe
frontend.url=http://localhost:4200

# RestTemplate Configuration
spring.http.client.connect-timeout=5000
spring.http.client.read-timeout=5000
```

**Checkpoint:** ✅ Configuración agregada

---

## 📦 Paso 1.8: Actualizar PaymentController

### Archivo: `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/controller/PaymentController.java`

### ✅ Acción: AGREGAR nuevo endpoint para Stripe

**AGREGAR después del método `createPayment`:**
```java
@PostMapping("/create-checkout-stripe")
public ResponseEntity<PaymentResponse> createCheckoutStripe(
        @RequestBody @Valid PaymentRequest request,
        @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
    
    log.info("Creating Stripe checkout for order: {}", request.getOrderId());
    
    PaymentResponse response = paymentService.createPaymentWithStripe(request);
    
    return ResponseEntity.ok(response);
}

@GetMapping("/status/{orderId}")
public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String orderId) {
    Payment payment = paymentService.getPaymentByOrderId(orderId);
    
    return ResponseEntity.ok(PaymentStatusResponse.builder()
        .orderId(payment.getOrderId())
        .status(payment.getStatus().name())
        .amount(payment.getAmount())
        .currency(payment.getCurrency())
        .createdAt(payment.getCreatedAt())
        .build());
}
```

**Checkpoint:** ✅ Endpoints actualizados

---

## 📦 Paso 1.9: Modificar Order-Service para usar Stripe

### Archivo: `packedgo/back/order-service/src/main/java/com/packed_go/order_service/service/impl/OrderServiceImpl.java`

### ✅ Acción: MODIFICAR método `checkout()` para llamar a Stripe

**BUSCAR (línea ~90):**
```java
PaymentServiceResponse paymentResponse = paymentServiceClient.createPayment(paymentRequest);
```

**REEMPLAZAR CON:**
```java
// Llamar al nuevo endpoint de Stripe
PaymentServiceResponse paymentResponse = paymentServiceClient.createPaymentStripe(paymentRequest);
```

### ✅ Acción: AGREGAR método en PaymentServiceClient

**Archivo:** `packedgo/back/order-service/src/main/java/com/packed_go/order_service/client/PaymentServiceClient.java`

**AGREGAR:**
```java
@PostMapping("/api/payments/create-checkout-stripe")
PaymentServiceResponse createPaymentStripe(@RequestBody PaymentServiceRequest request);
```

**Checkpoint:** ✅ Order-Service usa Stripe

---

## 📦 Paso 1.10: Testing de Fase 1

### ✅ Tests a ejecutar:

1. **Test 1: Stripe API arranca**
   ```bash
   cd stripe/stripe
   mvn clean package
   mvn spring-boot:run
   # Verificar: http://localhost:8081/actuator/health
   ```

2. **Test 2: Docker Compose arranca con Stripe**
   ```bash
   cd packedgo/back
   docker-compose down
   docker-compose up -d --build stripe-api payment-service order-service
   docker logs stripe-api
   ```

3. **Test 3: Crear sesión de pago**
   ```bash
   curl -X POST http://localhost:8085/api/payments/create-checkout-stripe \
     -H "Content-Type: application/json" \
     -d '{
       "orderId": "TEST-001",
       "customerEmail": "test@test.com",
       "amount": 100.00,
       "description": "Test Order",
       "items": [{
         "name": "Test Ticket",
         "description": "Event entry",
         "quantity": 1,
         "unitPrice": 100.00
       }]
     }'
   ```

4. **Test 4: Verificar webhook**
   - Abrir URL de checkout retornada
   - Usar tarjeta de prueba: `4242 4242 4242 4242`
   - Verificar logs: `docker logs payment-service`
   - Verificar orden marcada como PAID

**Checkpoint:** ✅ FASE 1 COMPLETADA - Stripe funcionando

---

# FASE 2: ELIMINAR MULTIORDERSESSION

## 📦 Paso 2.1: Identificar archivos a eliminar

### ✅ Archivos para ELIMINAR COMPLETAMENTE:

```
packedgo/back/order-service/src/main/java/com/packed_go/order_service/
├── entity/
│   └── MultiOrderSession.java                    [🗑️ ELIMINAR]
├── repository/
│   └── MultiOrderSessionRepository.java          [🗑️ ELIMINAR]
├── dto/
│   ├── SessionStateResponse.java                 [🗑️ ELIMINAR]
│   └── MultiOrderCheckoutResponse.java           [🗑️ ELIMINAR]
└── migration files/
    ├── migration_add_session_token.sql           [🗑️ ELIMINAR]
    └── migration_robust_session.sql              [🗑️ ELIMINAR]
```

### ✅ Métodos para ELIMINAR en OrderServiceImpl:

```java
// ELIMINAR estos métodos (líneas aproximadas):
- checkoutMulti()                    // Línea ~200-280
- getSessionStatus()                 // Línea ~290-305
- recoverSessionByToken()            // Línea ~306-320
- getSessionTickets()                // Línea ~325-350
- getCurrentCheckoutState()          // Línea ~360-415
- buildSessionStateResponse()        // Línea ~417-465
- abandonSession()                   // Línea ~470-520
- buildMultiOrderResponse()          // Línea ~530-570
```

**Total a eliminar:** ~500 líneas

---

## 📦 Paso 2.2: Eliminar referencias en Order entity

### Archivo: `packedgo/back/order-service/src/main/java/com/packed_go/order_service/entity/Order.java`

### ✅ Acción: ELIMINAR relación con MultiOrderSession

**BUSCAR y ELIMINAR estas líneas:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "session_id")
@JsonBackReference
private MultiOrderSession multiOrderSession;
```

**BUSCAR en `updateOrderFromPaymentCallback()` y ELIMINAR:**
```java
// Si esta orden pertenece a una sesión múltiple, actualizar el estado de la sesión
if (order.getMultiOrderSession() != null) {
    MultiOrderSession session = order.getMultiOrderSession();
    session.updateSessionStatus();
    sessionRepository.save(session);
    // ... más código relacionado
}
```

**Checkpoint:** ✅ Order desacoplado de MultiOrderSession

---

## 📦 Paso 2.3: Simplificar método checkout()

### Archivo: `packedgo/back/order-service/src/main/java/com/packed_go/order_service/service/impl/OrderServiceImpl.java`

### ✅ Acción: SIMPLIFICAR checkout() - eliminar lógica de sesiones

**BUSCAR método `checkout()` (línea ~55) y SIMPLIFICAR a:**

```java
@Override
@Transactional
public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
    log.info("Processing checkout for user: {}", userId);
    
    // 1. Obtener carrito activo del usuario
    List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
    if (activeCarts.isEmpty()) {
        throw new CartNotFoundException("No active cart found for user");
    }
    ShoppingCart cart = activeCarts.get(0);
    
    // 2. Validar que el carrito no esté expirado y tenga items
    if (cart.isExpired()) {
        cart.markAsExpired();
        cartRepository.save(cart);
        throw new CartExpiredException("Cart has expired");
    }
    
    if (cart.getItems().isEmpty()) {
        throw new IllegalStateException("Cart is empty");
    }
    
    // 3. Crear UNA SOLA orden desde el carrito
    Order order = Order.builder()
            .userId(userId)
            .cartId(cart.getId())
            .totalAmount(cart.getTotalAmount())
            .status(Order.OrderStatus.PENDING_PAYMENT)
            .build();
    
    // Copiar items del carrito a la orden
    cart.getItems().forEach(cartItem -> {
        OrderItem orderItem = OrderItem.fromCartItem(cartItem);
        order.addItem(orderItem);
    });
    
    order = orderRepository.save(order);
    
    log.info("Order created: {} with total: {}", order.getOrderNumber(), order.getTotalAmount());
    
    // 4. Crear el pago en payment-service (STRIPE)
    PaymentServiceRequest paymentRequest = PaymentServiceRequest.builder()
            .orderId(order.getOrderNumber())
            .amount(order.getTotalAmount())
            .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail() : "customer@packedgo.com")
            .description("Order " + order.getOrderNumber() + " - PackedGo Events")
            .items(mapOrderItemsToPaymentItems(order.getItems()))
            .build();
    
    PaymentServiceResponse paymentResponse = paymentServiceClient.createPaymentStripe(paymentRequest);
    
    // 5. Actualizar orden con datos del pago
    order.setPaymentId(paymentResponse.getPaymentId());
    order.setPaymentSessionId(paymentResponse.getSessionId());
    orderRepository.save(order);
    
    // 6. Marcar carrito como CHECKED_OUT
    cart.markAsCheckedOut();
    cartRepository.save(cart);
    
    log.info("Checkout completed successfully. Redirecting to: {}", paymentResponse.getCheckoutUrl());
    
    // 7. Retornar respuesta con URL de pago
    return CheckoutResponse.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .totalAmount(order.getTotalAmount())
            .status(order.getStatus().name())
            .paymentUrl(paymentResponse.getCheckoutUrl())
            .sessionId(paymentResponse.getSessionId())
            .message("Checkout successful. Redirect to payment gateway.")
            .build();
}

private List<PaymentServiceRequest.ItemDTO> mapOrderItemsToPaymentItems(List<OrderItem> orderItems) {
    return orderItems.stream()
            .map(item -> PaymentServiceRequest.ItemDTO.builder()
                    .name(item.getEventName())
                    .description("Event ticket")
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .build())
            .collect(Collectors.toList());
}
```

**Checkpoint:** ✅ checkout() simplificado

---

## 📦 Paso 2.4: Eliminar imports y dependencias

### Archivo: `packedgo/back/order-service/src/main/java/com/packed_go/order_service/service/impl/OrderServiceImpl.java`

### ✅ Acción: ELIMINAR imports no usados

**ELIMINAR estas líneas de import:**
```java
import com.packed_go.order_service.dto.SessionStateResponse;
import com.packed_go.order_service.entity.MultiOrderSession;
import com.packed_go.order_service.repository.MultiOrderSessionRepository;
import com.packed_go.order_service.dto.MultiOrderCheckoutResponse;
```

**ELIMINAR del constructor:**
```java
private final MultiOrderSessionRepository sessionRepository;  // ELIMINAR ESTA LÍNEA
```

**Checkpoint:** ✅ Imports limpiados

---

## 📦 Paso 2.5: Actualizar OrderService interface

### Archivo: `packedgo/back/order-service/src/main/java/com/packed_go/order_service/service/OrderService.java`

### ✅ Acción: ELIMINAR métodos de sesión

**ELIMINAR estos métodos:**
```java
MultiOrderCheckoutResponse checkoutMulti(Long userId);
MultiOrderCheckoutResponse getSessionStatus(String sessionId);
MultiOrderCheckoutResponse recoverSessionByToken(String sessionToken);
List<Object> getSessionTickets(String sessionId);
SessionStateResponse getCurrentCheckoutState(Long userId);
void abandonSession(String sessionId, Long userId);
```

**MANTENER solo:**
```java
CheckoutResponse checkout(Long userId, CheckoutRequest request);
void updateOrderFromPaymentCallback(PaymentCallbackRequest request);
List<Order> getUserOrders(Long userId);
Order getOrderByNumber(String orderNumber);
Order getOrderById(Long orderId);
```

**Checkpoint:** ✅ Interface limpiada

---

## 📦 Paso 2.6: Actualizar OrderController

### Archivo: `packedgo/back/order-service/src/main/java/com/packed_go/order_service/controller/OrderController.java`

### ✅ Acción: ELIMINAR endpoints de sesión

**ELIMINAR estos endpoints:**
```java
@PostMapping("/checkout-multi")  // ELIMINAR
@GetMapping("/session/{sessionId}")  // ELIMINAR
@GetMapping("/session/{sessionId}/tickets")  // ELIMINAR
@GetMapping("/session/recover")  // ELIMINAR
@GetMapping("/checkout-state")  // ELIMINAR
@DeleteMapping("/session/{sessionId}/abandon")  // ELIMINAR
```

**MANTENER solo:**
```java
@PostMapping("/checkout")
@PostMapping("/payment-callback")
@GetMapping("/user/{userId}")
@GetMapping("/{orderNumber}")
@GetMapping("/id/{orderId}")
```

**Checkpoint:** ✅ Controller limpiado

---

## 📦 Paso 2.7: Eliminar archivos físicos

### ✅ Comandos para ejecutar:

```powershell
# Eliminar entity
Remove-Item "packedgo\back\order-service\src\main\java\com\packed_go\order_service\entity\MultiOrderSession.java"

# Eliminar repository
Remove-Item "packedgo\back\order-service\src\main\java\com\packed_go\order_service\repository\MultiOrderSessionRepository.java"

# Eliminar DTOs
Remove-Item "packedgo\back\order-service\src\main\java\com\packed_go\order_service\dto\SessionStateResponse.java"
Remove-Item "packedgo\back\order-service\src\main\java\com\packed_go\order_service\dto\MultiOrderCheckoutResponse.java"

# Eliminar migraciones
Remove-Item "packedgo\back\order-service\migration_add_session_token.sql"
Remove-Item "packedgo\back\order-service\migration_robust_session.sql"
```

**Checkpoint:** ✅ Archivos eliminados

---

## 📦 Paso 2.8: Actualizar base de datos

### ✅ Script SQL a ejecutar en `orders_db`:

```sql
-- Eliminar columna session_id de orders
ALTER TABLE orders DROP COLUMN IF EXISTS session_id;

-- Eliminar tabla multi_order_sessions
DROP TABLE IF EXISTS multi_order_sessions CASCADE;

-- Agregar campos para Stripe si no existen
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS payment_session_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS stripe_payment_url TEXT;
```

**Checkpoint:** ✅ Base de datos actualizada

---

## 📦 Paso 2.9: Testing de Fase 2

### ✅ Tests a ejecutar:

1. **Test 1: Compilación**
   ```bash
   cd packedgo/back/order-service
   mvn clean compile
   # Debe compilar sin errores
   ```

2. **Test 2: Checkout simple**
   ```bash
   curl -X POST http://localhost:8084/api/orders/checkout \
     -H "Content-Type: application/json" \
     -H "X-User-Id: 1" \
     -d '{
       "customerEmail": "test@test.com"
     }'
   ```

3. **Test 3: Verificar que NO existen endpoints de sesión**
   ```bash
   curl http://localhost:8084/api/orders/checkout-multi
   # Debe retornar 404 Not Found
   ```

**Checkpoint:** ✅ FASE 2 COMPLETADA - MultiOrderSession eliminado

---

# FASE 3: FUSIONAR CONSUMPTION-SERVICE

## 📦 Paso 3.1: Mover validación de QR a event-service

### A) Copiar QRCodeService

**Archivo ORIGEN:** `packedgo/back/consumption-service/src/main/java/com/packed_go/consumption_service/services/QRCodeService.java`

**Archivo DESTINO:** `packedgo/back/event-service/src/main/java/com/packed_go/event_service/services/QRCodeService.java`

### ✅ Acción: COPIAR el servicio completo

---

## 📦 Paso 3.2: Crear endpoints de validación en TicketController

### Archivo: `packedgo/back/event-service/src/main/java/com/packed_go/event_service/controllers/TicketController.java`

### ✅ Acción: AGREGAR endpoints de validación

```java
@PostMapping("/validate-entry")
public ResponseEntity<EntryValidationResponse> validateEntry(@RequestBody ValidateEntryRequest request) {
    log.info("Validating entry for QR code");
    
    try {
        // Decodificar QR
        QRPayload payload = qrCodeService.validateAndDecodeQR(request.getQrCode());
        
        // Buscar ticket
        TicketDTO ticket = ticketService.findById(payload.getTicketId());
        
        // Validaciones
        if (ticket.isRedeemed()) {
            return ResponseEntity.ok(buildErrorResponse("Ticket already used"));
        }
        
        // Redimir ticket
        ticketService.redeemTicket(ticket.getId());
        
        return ResponseEntity.ok(EntryValidationResponse.builder()
            .valid(true)
            .message("Entry validated successfully")
            .ticketId(ticket.getId())
            .userId(ticket.getUserId())
            .validatedAt(LocalDateTime.now())
            .build());
            
    } catch (Exception e) {
        log.error("Error validating entry", e);
        return ResponseEntity.ok(buildErrorResponse(e.getMessage()));
    }
}

@PostMapping("/validate-consumption")
public ResponseEntity<ConsumptionValidationResponse> validateConsumption(
        @RequestBody ValidateConsumptionRequest request) {
    log.info("Validating consumption for QR code");
    
    try {
        // Decodificar QR
        QRPayload payload = qrCodeService.validateAndDecodeQR(request.getQrCode());
        
        // Buscar detalle de consumición
        TicketConsumptionDetail detail = ticketConsumptionDetailRepository
            .findById(payload.getDetailId())
            .orElseThrow(() -> new RuntimeException("Consumption not found"));
        
        // Validar
        if (detail.isRedeem()) {
            return ResponseEntity.ok(buildConsumptionErrorResponse("Already redeemed"));
        }
        
        // Redimir
        detail.setRedeem(true);
        detail.setRedeemedAt(LocalDateTime.now());
        ticketConsumptionDetailRepository.save(detail);
        
        return ResponseEntity.ok(ConsumptionValidationResponse.builder()
            .valid(true)
            .message("Consumption redeemed successfully")
            .detailId(detail.getId())
            .validatedAt(LocalDateTime.now())
            .build());
            
    } catch (Exception e) {
        log.error("Error validating consumption", e);
        return ResponseEntity.ok(buildConsumptionErrorResponse(e.getMessage()));
    }
}
```

**Checkpoint:** ✅ Endpoints de validación en event-service

---

## 📦 Paso 3.3: Eliminar consumption-service de Docker Compose

### Archivo: `packedgo/back/docker-compose.yml`

### ✅ Acción: ELIMINAR servicio consumption-service

**BUSCAR y ELIMINAR:**
```yaml
  consumption-service:
    build:
      context: ./consumption-service
      dockerfile: Dockerfile
    # ... TODO EL BLOQUE
```

**Checkpoint:** ✅ consumption-service eliminado de Docker

---

## 📦 Paso 3.4: Testing de Fase 3

### ✅ Tests a ejecutar:

1. **Test 1: Validar entrada**
   ```bash
   curl -X POST http://localhost:8082/api/tickets/validate-entry \
     -H "Content-Type: application/json" \
     -d '{"qrCode": "BASE64_QR_CODE"}'
   ```

2. **Test 2: Validar consumición**
   ```bash
   curl -X POST http://localhost:8082/api/tickets/validate-consumption \
     -H "Content-Type: application/json" \
     -d '{"qrCode": "BASE64_QR_CODE"}'
   ```

**Checkpoint:** ✅ FASE 3 COMPLETADA - consumption-service eliminado

---

# FASE 4: SIMPLIFICAR SHOPPING CART (OPCIONAL)

## 📦 Paso 4.1: Simplificar estados de ShoppingCart

### Archivo: `packedgo/back/order-service/src/main/java/com/packed_go/order_service/entity/ShoppingCart.java`

### ✅ Acción: ELIMINAR estados IN_CHECKOUT y métodos relacionados

**BUSCAR:**
```java
public void markAsInCheckout() {
    this.status = "IN_CHECKOUT";
    this.updatedAt = LocalDateTime.now();
}

public void reactivate() {
    this.status = "ACTIVE";
    this.expiresAt = LocalDateTime.now().plusMinutes(30);
    this.updatedAt = LocalDateTime.now();
}
```

**ELIMINAR ambos métodos**

**SIMPLIFICAR estados a:**
- `ACTIVE` - Carrito activo
- `COMPLETED` - Checkout completado

**Checkpoint:** ✅ Shopping Cart simplificado

---

# 📊 CHECKLIST FINAL DE VALIDACIÓN

## ✅ Fase 1 - Stripe Integration
- [ ] Stripe API corre en puerto 8081
- [ ] Docker Compose incluye stripe-api
- [ ] StripeIntegrationService creado
- [ ] PaymentService usa Stripe
- [ ] Webhook handler funciona
- [ ] Order-Service llama a Stripe
- [ ] Test end-to-end: crear orden → pagar → webhook → tickets

## ✅ Fase 2 - Eliminar MultiOrderSession
- [ ] MultiOrderSession.java eliminado
- [ ] MultiOrderSessionRepository.java eliminado
- [ ] SessionStateResponse.java eliminado
- [ ] MultiOrderCheckoutResponse.java eliminado
- [ ] Métodos de sesión eliminados de OrderServiceImpl
- [ ] Endpoints de sesión eliminados de OrderController
- [ ] Order entity sin referencia a session
- [ ] Base de datos actualizada
- [ ] Compilación exitosa

## ✅ Fase 3 - Fusionar Consumption-Service
- [ ] QRCodeService copiado a event-service
- [ ] Endpoints de validación en TicketController
- [ ] consumption-service eliminado de Docker
- [ ] Tests de validación funcionan

## ✅ Fase 4 - Simplificar Shopping Cart
- [ ] Estados reducidos a ACTIVE/COMPLETED
- [ ] Métodos innecesarios eliminados

---

# 🔧 COMANDOS ÚTILES

## Compilar servicios
```bash
cd packedgo/back/payment-service && mvn clean compile
cd packedgo/back/order-service && mvn clean compile
cd packedgo/back/event-service && mvn clean compile
```

## Reiniciar Docker
```bash
cd packedgo/back
docker-compose down
docker-compose up -d --build
docker-compose logs -f payment-service
```

## Ver logs
```bash
docker logs payment-service -f
docker logs order-service -f
docker logs stripe-api -f
```

## Test de checkout completo
```bash
# 1. Crear carrito (asume que ya existe)
# 2. Checkout
curl -X POST http://localhost:8084/api/orders/checkout \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{"customerEmail": "test@test.com"}'
  
# 3. Abrir URL de pago retornada
# 4. Usar tarjeta de prueba: 4242 4242 4242 4242
# 5. Verificar webhook en logs
# 6. Verificar tickets generados
```

---

# 📈 MÉTRICAS DE ÉXITO

| Métrica | Antes | Después | Mejora |
|---------|-------|---------|--------|
| Líneas de código | ~2,000 | ~700 | -65% |
| Microservicios | 5 | 4 | -20% |
| Endpoints API | 25 | 15 | -40% |
| Tablas BD | 12 | 10 | -17% |
| Estados de orden | 5 sistemas | 2 sistemas | -60% |
| Tiempo de checkout | ~8s | ~3s | -62% |
| Webhooks funcionales | ❌ NO | ✅ SÍ | ∞ |

---

# 🚨 NOTAS IMPORTANTES

1. **BACKUP:** Hacer commit antes de cada fase
2. **TESTING:** Ejecutar tests después de cada paso
3. **LOGS:** Revisar logs constantemente
4. **ROLLBACK:** Si algo falla, usar `git reset --hard`
5. **TOKENS:** Si se agotan, este documento tiene TODO lo necesario

---

# 📞 PRÓXIMOS PASOS DESPUÉS DE COMPLETAR

1. **Frontend:** Actualizar componente de checkout para usar Stripe
2. **Testing:** Pruebas de integración completas
3. **Documentación:** Actualizar README
4. **Deploy:** Preparar para producción
5. **Tesis:** Documentar arquitectura simplificada

---

**CREADO:** 18 de Noviembre 2025  
**ESTADO:** 🔵 LISTO PARA COMENZAR  
**PRIORIDAD:** 🔴 CRÍTICO

---

**CONTINUAR CON:** Paso 1.1 - Preparar Stripe API Service
