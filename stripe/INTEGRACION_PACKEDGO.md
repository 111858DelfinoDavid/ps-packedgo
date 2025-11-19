# Integración Stripe con PackedGo - Payment Service

## 🎯 Arquitectura de Integración

Esta API de Stripe está diseñada para integrarse con el microservicio `payment-service` (puerto 8085) de PackedGo.

## 🔄 Flujo Completo de Pago en PackedGo

```
1. FRONTEND (Angular) → ORDER-SERVICE (8084)
   Cliente construye paquete (entrada + consumiciones)
   
2. ORDER-SERVICE → PAYMENT-SERVICE (8085)
   Crea orden de pago con detalles del paquete
   
3. PAYMENT-SERVICE → STRIPE-API (8081)
   Solicita creación de sesión de checkout
   
4. STRIPE-API → Stripe SDK
   Crea checkout session en Stripe
   
5. Stripe → FRONTEND
   Redirige al cliente al checkout de Stripe
   
6. Cliente completa pago → Stripe Checkout
   Procesa tarjeta de crédito/débito (3D Secure incluido)
   
7. Stripe → WEBHOOK (8081/api/webhooks/stripe)
   Notifica resultado del pago (checkout.session.completed)
   
8. WEBHOOK → PAYMENT-SERVICE (8085)
   Actualiza estado del pago en DB
   
9. PAYMENT-SERVICE → ORDER-SERVICE (8084)
   Confirma orden de compra
   
10. ORDER-SERVICE → QR-SERVICE (8086)
    Genera códigos QR únicos
    
11. QR-SERVICE → AUTH-SERVICE (8081)
    Envía email con QR al cliente
```

## 🔧 Configuración para Payment-Service

### 1. Variables de Entorno en payment-service

```properties
# En payment-service/.env
STRIPE_API_URL=http://localhost:8081/api/stripe
STRIPE_API_INTERNAL=http://stripe-api:8081/api/stripe
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here
WEBHOOK_URL=http://localhost:8081/api/webhooks/stripe
```

### 2. Modelo de Request desde Order-Service

```java
// En payment-service
@Data
@Builder
public class PaymentRequest {
    private String orderId;              // ID de la orden desde order-service
    private String customerEmail;        // Email del cliente
    private List<PackageItem> items;     // Items del paquete
    private BigDecimal totalAmount;      // Total en ARS o USD
    private String currency;             // "usd" o "ars" (Stripe requiere minúsculas)
    
    @Data
    @Builder
    public static class PackageItem {
        private String name;             // "Entrada General"
        private String description;      // "Acceso al evento"
        private Integer quantity;
        private Long unitAmount;         // Precio en CENTAVOS (5000 = $50.00)
        private String currency;         // "usd" o "ars"
    }
}
```

### 3. Servicio de Integración en Payment-Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeIntegrationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${stripe.api.url}")
    private String stripeApiUrl;
    
    @Value("${frontend.url}")
    private String frontendUrl;
    
    public CheckoutSessionResponse createPaymentSession(PaymentRequest paymentRequest) {
        // Mapear request interno a formato de Stripe API
        CreateCheckoutRequest stripeRequest = CreateCheckoutRequest.builder()
            .items(paymentRequest.getItems().stream()
                .map(item -> CheckoutItemDTO.builder()
                    .name(item.getName())
                    .description(item.getDescription())
                    .quantity(item.getQuantity())
                    .unitAmount(convertToCents(item.getUnitAmount())) // Convertir a centavos
                    .currency(item.getCurrency().toLowerCase())
                    .build())
                .collect(Collectors.toList()))
            .successUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
            .cancelUrl(frontendUrl + "/payment/cancel")
            .customerEmail(paymentRequest.getCustomerEmail())
            .externalReference(paymentRequest.getOrderId())
            .build();
        
        // Llamar a la API de Stripe
        String url = stripeApiUrl + "/create-checkout-session";
        
        try {
            CheckoutSessionResponse response = restTemplate.postForObject(
                url, stripeRequest, CheckoutSessionResponse.class);
            
            log.info("Sesión de checkout creada: {} para orden: {}", 
                response.getSessionId(), paymentRequest.getOrderId());
            
            return response;
            
        } catch (RestClientException e) {
            log.error("Error al crear sesión de Stripe: {}", e.getMessage());
            throw new PaymentServiceException("Error al procesar el pago", e);
        }
    }
    
    private Long convertToCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
    
    public PaymentDetails getPaymentDetails(String sessionId) {
        String url = stripeApiUrl + "/session/" + sessionId;
        return restTemplate.getForObject(url, PaymentDetails.class);
    }
}
```

### 4. Controller en Payment-Service

```java
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final StripeIntegrationService stripeService;
    private final PaymentRepository paymentRepository;
    
    @PostMapping("/create-checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckout(
            @RequestBody @Valid PaymentRequest request,
            @RequestHeader("X-User-Email") String userEmail) {
        
        log.info("Creando checkout para orden: {}", request.getOrderId());
        
        // Crear sesión de Stripe
        CheckoutSessionResponse session = stripeService.createPaymentSession(request);
        
        // Guardar en DB con estado PENDING
        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .stripeSessionId(session.getSessionId())
            .amount(request.getTotalAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.PENDING)
            .customerEmail(request.getCustomerEmail())
            .build();
        
        paymentRepository.save(payment);
        
        return ResponseEntity.ok(session);
    }
    
    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String orderId) {
        
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentNotFoundException(orderId));
        
        return ResponseEntity.ok(PaymentStatusResponse.builder()
            .orderId(payment.getOrderId())
            .status(payment.getStatus().name())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .paymentMethod(payment.getPaymentMethod())
            .createdAt(payment.getCreatedAt())
            .build());
    }
}
```

## 📡 Configuración de Webhooks

### Webhook Handler en Payment-Service

```java
@RestController
@RequestMapping("/api/payment/webhook")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    
    private final PaymentService paymentService;
    private final OrderServiceClient orderServiceClient;
    private final StripeIntegrationService stripeService;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    @PostMapping("/stripe-notification")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Webhook recibido de Stripe");
        
        try {
            // Validar firma del webhook (importante para seguridad)
            if (!validateWebhookSignature(payload, signature)) {
                log.error("Firma de webhook inválida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            // Parsear el evento
            StripeWebhookEvent event = parseWebhookEvent(payload);
            
            // Manejar diferentes tipos de eventos
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutCompleted(event);
                    break;
                    
                case "payment_intent.succeeded":
                    handlePaymentSucceeded(event);
                    break;
                    
                case "payment_intent.payment_failed":
                    handlePaymentFailed(event);
                    break;
                    
                default:
                    log.info("Evento no manejado: {}", event.getType());
            }
            
            return ResponseEntity.ok("Webhook processed");
            
        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing webhook");
        }
    }
    
    private void handleCheckoutCompleted(StripeWebhookEvent event) {
        String sessionId = event.getData().getObject().getId();
        String orderId = event.getData().getObject().getMetadata().getExternalReference();
        
        log.info("Checkout completado para orden: {}", orderId);
        
        // Obtener detalles completos de la sesión
        PaymentDetails details = stripeService.getPaymentDetails(sessionId);
        
        // Actualizar estado en DB
        Payment payment = paymentService.updatePaymentStatus(
            orderId,
            PaymentStatus.COMPLETED,
            details.getPaymentIntentId(),
            details.getPaymentMethod()
        );
        
        // Notificar a order-service que el pago fue exitoso
        try {
            orderServiceClient.confirmOrder(orderId, payment.getId());
            log.info("Orden confirmada exitosamente: {}", orderId);
        } catch (Exception e) {
            log.error("Error al confirmar orden: {}", orderId, e);
            // Aquí podrías implementar un retry mechanism
        }
    }
    
    private void handlePaymentFailed(StripeWebhookEvent event) {
        String paymentIntentId = event.getData().getObject().getId();
        String orderId = event.getData().getObject().getMetadata().getExternalReference();
        
        log.warn("Pago fallido para orden: {}", orderId);
        
        paymentService.updatePaymentStatus(
            orderId,
            PaymentStatus.FAILED,
            paymentIntentId,
            null
        );
        
        // Opcional: Notificar al usuario por email
    }
    
    private boolean validateWebhookSignature(String payload, String signature) {
        // Implementar validación HMAC con webhook secret
        // Stripe proporciona esta funcionalidad en su SDK
        return true; // Simplificado para el ejemplo
    }
}
```

### Cliente Feign para Order-Service

```java
@FeignClient(name = "order-service", url = "${services.order.url}")
public interface OrderServiceClient {
    
    @PostMapping("/api/orders/{orderId}/confirm-payment")
    OrderResponse confirmOrder(
        @PathVariable String orderId,
        @RequestParam Long paymentId,
        @RequestHeader("X-Internal-Auth") String internalToken
    );
    
    @PostMapping("/api/orders/{orderId}/cancel")
    void cancelOrder(
        @PathVariable String orderId,
        @RequestHeader("X-Internal-Auth") String internalToken
    );
}
```

## 🔗 Docker Compose - Agregar Stripe API

Agrega este servicio a tu `docker-compose.yml` existente:

```yaml
version: '3.8'

services:
  # ... otros servicios existentes ...
  
  stripe-api:
    build: 
      context: ../../PackedGoMP-API/stripe/stripe
      dockerfile: Dockerfile
    container_name: stripe-api
    ports:
      - "8081:8081"  # Puerto para Stripe API
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}
      - STRIPE_PUBLISHABLE_KEY=${STRIPE_PUBLISHABLE_KEY}
      - BASE_URL=https://api.packedgo.com
      - FRONTEND_URL=https://packedgo.com
      - CORS_ALLOWED_ORIGINS=http://localhost:3000,http://frontend:80,https://packedgo.com
    networks:
      - packedgo-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/api/stripe/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  payment-service:
    # ... configuración existente ...
    environment:
      # ... variables existentes ...
      - STRIPE_API_URL=http://stripe-api:8081/api/stripe
      - STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}
      - INTERNAL_AUTH_TOKEN=${INTERNAL_AUTH_TOKEN}
    depends_on:
      - stripe-api
      - postgres
    networks:
      - packedgo-network

networks:
  packedgo-network:
    driver: bridge
```

## 🧪 Testing de Integración

### 1. Test Completo desde Order-Service

```bash
# 1. Crear orden (simula frontend → order-service)
curl -X POST http://localhost:8084/api/orders/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -d '{
    "eventId": "event-123",
    "userId": "user-456",
    "items": [
      {
        "type": "TICKET",
        "name": "Entrada General",
        "quantity": 1,
        "price": 50.00
      },
      {
        "type": "DRINK",
        "name": "Cerveza",
        "quantity": 3,
        "price": 8.00
      },
      {
        "type": "DRINK",
        "name": "Fernet con Cola",
        "quantity": 2,
        "price": 12.00
      }
    ],
    "customerEmail": "cliente@ejemplo.com"
  }'

# Respuesta esperada:
# {
#   "orderId": "ORDER-123456",
#   "status": "PENDING_PAYMENT",
#   "totalAmount": 98.00,
#   "currency": "USD"
# }
```

### 2. Test desde Payment-Service a Stripe-API

```bash
# 2. Crear sesión de pago (order-service → payment-service → stripe-api)
curl -X POST http://localhost:8085/api/payments/create-checkout \
  -H "Content-Type: application/json" \
  -H "X-User-Email: cliente@ejemplo.com" \
  -d '{
    "orderId": "ORDER-123456",
    "customerEmail": "cliente@ejemplo.com",
    "currency": "usd",
    "items": [
      {
        "name": "Entrada General",
        "description": "Acceso al evento",
        "quantity": 1,
        "unitAmount": 5000,
        "currency": "usd"
      },
      {
        "name": "Cerveza",
        "description": "Consumición prepagada",
        "quantity": 3,
        "unitAmount": 800,
        "currency": "usd"
      },
      {
        "name": "Fernet con Cola",
        "description": "Consumición prepagada",
        "quantity": 2,
        "unitAmount": 1200,
        "currency": "usd"
      }
    ]
  }'

# Respuesta esperada:
# {
#   "sessionId": "cs_test_...",
#   "url": "https://checkout.stripe.com/c/pay/cs_test_...",
#   "status": "open"
# }
```

### 3. Simular Webhook de Stripe

```bash
# 3. Simular notificación de pago completado
curl -X POST http://localhost:8085/api/payment/webhook/stripe-notification \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: test-signature" \
  -d '{
    "type": "checkout.session.completed",
    "data": {
      "object": {
        "id": "cs_test_...",
        "payment_intent": "pi_...",
        "payment_status": "paid",
        "amount_total": 9800,
        "currency": "usd",
        "customer_email": "cliente@ejemplo.com",
        "metadata": {
          "externalReference": "ORDER-123456"
        }
      }
    }
  }'
```

### 4. Verificar Estado del Pago

```bash
# 4. Consultar estado del pago
curl http://localhost:8085/api/payments/status/ORDER-123456 \
  -H "Authorization: Bearer ${JWT_TOKEN}"

# Respuesta esperada:
# {
#   "orderId": "ORDER-123456",
#   "status": "COMPLETED",
#   "amount": 98.00,
#   "currency": "USD",
#   "paymentMethod": "card",
#   "createdAt": "2025-11-13T22:30:00Z"
# }
```

## 📊 Base de Datos - Payment Service

```sql
-- Tabla de pagos en payments_db (puerto 5437)
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    stripe_session_id VARCHAR(255) UNIQUE,
    stripe_payment_intent_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    payment_method VARCHAR(50),
    customer_email VARCHAR(255),
    external_reference VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Índices para optimizar consultas
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_stripe_session ON payments(stripe_session_id);
CREATE INDEX idx_payments_customer_email ON payments(customer_email);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- Enum para estados de pago
CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
);

ALTER TABLE payments ALTER COLUMN status TYPE payment_status USING status::payment_status;
```

## 🔐 Seguridad

### 1. Validación de Webhooks con Stripe SDK

```java
@Component
@RequiredArgsConstructor
public class StripeWebhookValidator {
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;
    
    public Event validateAndParseWebhook(String payload, String signature) 
            throws SignatureVerificationException {
        
        try {
            // Stripe SDK valida automáticamente la firma HMAC
            return Webhook.constructEvent(payload, signature, webhookSecret);
            
        } catch (SignatureVerificationException e) {
            log.error("Firma de webhook inválida: {}", e.getMessage());
            throw e;
        }
    }
}
```

### 2. Comunicación Inter-Servicios Segura

```java
@Configuration
public class FeignClientConfig {
    
    @Value("${internal.auth.token}")
    private String internalAuthToken;
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // Agregar token de autenticación interna
            template.header("X-Internal-Auth", internalAuthToken);
            template.header("X-Service-Name", "payment-service");
        };
    }
}

// En cada microservicio, validar el token interno
@Component
public class InternalAuthFilter extends OncePerRequestFilter {
    
    @Value("${internal.auth.token}")
    private String expectedToken;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Solo validar rutas internas
        if (path.startsWith("/api/orders/") && path.contains("/confirm")) {
            String token = request.getHeader("X-Internal-Auth");
            
            if (!expectedToken.equals(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

## 📝 Variables de Entorno Completas

### .env para todos los servicios

```bash
# ============================================
# STRIPE API (.env en stripe/stripe/)
# ============================================
STRIPE_SECRET_KEY=sk_test_51STBhI...
STRIPE_PUBLISHABLE_KEY=pk_test_51STBhI...
BASE_URL=http://localhost:8081
FRONTEND_URL=http://localhost:3000
SERVER_PORT=8081

# ============================================
# PAYMENT SERVICE
# ============================================
STRIPE_API_URL=http://stripe-api:8081/api/stripe
STRIPE_WEBHOOK_SECRET=whsec_...
ORDER_SERVICE_URL=http://order-service:8084
QR_SERVICE_URL=http://qr-service:8086
INTERNAL_AUTH_TOKEN=your_super_secret_internal_token_here

# Base de datos
DB_HOST=postgres
DB_PORT=5437
DB_NAME=payments_db
DB_USER=packedgo_user
DB_PASSWORD=secure_password

# ============================================
# ORDER SERVICE
# ============================================
PAYMENT_SERVICE_URL=http://payment-service:8085
QR_SERVICE_URL=http://qr-service:8086
INTERNAL_AUTH_TOKEN=your_super_secret_internal_token_here

# ============================================
# FRONTEND (Angular)
# ============================================
STRIPE_PUBLISHABLE_KEY=pk_test_51STBhI...
API_URL=http://localhost:8084
PAYMENT_SERVICE_URL=http://localhost:8085
```

## 🚀 Deployment en Producción

### 1. Configurar Webhook en Stripe Dashboard

1. Ir a: https://dashboard.stripe.com/webhooks
2. Crear endpoint: `https://api.packedgo.com/api/payment/webhook/stripe-notification`
3. Seleccionar eventos:
   - `checkout.session.completed`
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
4. Copiar el **Signing Secret** (empieza con `whsec_`)
5. Agregarlo a las variables de entorno de producción

### 2. URLs de Producción

```properties
# application-prod.properties
stripe.api.url=https://api.packedgo.com/stripe
frontend.base.url=https://packedgo.com
webhook.base.url=https://api.packedgo.com

# URLs de retorno
payment.success.url=https://packedgo.com/payment/success?session_id={CHECKOUT_SESSION_ID}
payment.cancel.url=https://packedgo.com/payment/cancel
```

### 3. Migrar a Claves de Producción

```bash
# Obtener de: https://dashboard.stripe.com/apikeys
STRIPE_SECRET_KEY=sk_live_...  # Cambiar sk_test_ por sk_live_
STRIPE_PUBLISHABLE_KEY=pk_live_...  # Cambiar pk_test_ por pk_live_
```

## 🎨 Integración Frontend (Angular)

### payment.service.ts

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  
  private apiUrl = `${environment.paymentServiceUrl}/api/payments`;
  
  constructor(private http: HttpClient) { }
  
  createCheckoutSession(orderId: string, items: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/create-checkout`, {
      orderId,
      customerEmail: this.getUserEmail(),
      currency: 'usd',
      items: items.map(item => ({
        name: item.name,
        description: item.description,
        quantity: item.quantity,
        unitAmount: Math.round(item.price * 100), // Convertir a centavos
        currency: 'usd'
      }))
    });
  }
  
  redirectToCheckout(checkoutUrl: string): void {
    window.location.href = checkoutUrl;
  }
  
  getPaymentStatus(orderId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/status/${orderId}`);
  }
  
  private getUserEmail(): string {
    // Obtener email del usuario logueado
    return localStorage.getItem('userEmail') || '';
  }
}
```

### checkout.component.ts

```typescript
export class CheckoutComponent implements OnInit {
  
  constructor(
    private orderService: OrderService,
    private paymentService: PaymentService,
    private router: Router
  ) { }
  
  proceedToPayment(): void {
    this.loading = true;
    
    // 1. Crear orden
    this.orderService.createOrder(this.packageItems).subscribe({
      next: (order) => {
        
        // 2. Crear sesión de pago
        this.paymentService.createCheckoutSession(
          order.orderId, 
          this.packageItems
        ).subscribe({
          next: (session) => {
            // 3. Redirigir a Stripe Checkout
            this.paymentService.redirectToCheckout(session.url);
          },
          error: (error) => {
            this.handleError('Error al iniciar el pago');
            this.loading = false;
          }
        });
      },
      error: (error) => {
        this.handleError('Error al crear la orden');
        this.loading = false;
      }
    });
  }
}
```

### success.component.ts

```typescript
export class PaymentSuccessComponent implements OnInit {
  
  sessionId: string;
  paymentDetails: any;
  
  constructor(
    private route: ActivatedRoute,
    private paymentService: PaymentService
  ) { }
  
  ngOnInit(): void {
    // Obtener session_id de la URL
    this.sessionId = this.route.snapshot.queryParamMap.get('session_id');
    
    if (this.sessionId) {
      this.loadPaymentDetails();
    }
  }
  
  loadPaymentDetails(): void {
    // Extraer orderId del sessionId o buscarlo en el backend
    this.paymentService.getPaymentStatus(this.orderId).subscribe({
      next: (details) => {
        this.paymentDetails = details;
        
        if (details.status === 'COMPLETED') {
          this.showSuccessMessage();
          // Redirigir a la página de tickets/QR
          setTimeout(() => {
            this.router.navigate(['/my-tickets']);
          }, 3000);
        }
      }
    });
  }
}
```

## 📈 Monitoreo y Logs

### Application Insights / Logging

```java
@Slf4j
@Aspect
@Component
public class PaymentLoggingAspect {
    
    @Around("execution(* com.packedgo.payment.service..*(..))")
    public Object logPaymentOperations(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.info("Iniciando operación de pago: {} con args: {}", methodName, args);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Operación exitosa: {} - Duración: {}ms", methodName, duration);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error en operación de pago: {} - Error: {}", 
                methodName, e.getMessage(), e);
            throw e;
        }
    }
}
```

## 🔄 Manejo de Reintentos y Resiliencia

```java
@Configuration
public class ResilienceConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configurar timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }
}

// Usar @Retryable en métodos críticos
@Service
public class OrderServiceClient {
    
    @Retryable(
        value = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000)
    )
    public void confirmOrder(String orderId, Long paymentId) {
        // Llamada al order-service
    }
}
```

## 📞 Contacto y Soporte

Para dudas sobre la integración:
- **Email:** soporte@packedgo.com
- **Equipo:** David Delfino & Agustín Luparia
- **UTN-FRC:** 2025

---

## ✅ Ventajas de Stripe vs Mercado Pago para PackedGo

### Stripe
✅ Checkout hosteado por Stripe (menos código frontend)  
✅ Webhooks funcionan perfectamente en testing  
✅ Soporte 3D Secure nativo  
✅ Documentación excelente  
✅ SDK más moderno  
✅ Testing gratuito e ilimitado  
✅ Redireccionamiento automático funciona  

### Mercado Pago
⚠️ Requiere más código frontend personalizado  
❌ Webhooks NO funcionan en sandbox  
❌ autoReturn NO funciona en sandbox  
❌ Requiere botón manual "Volver al sitio"  
⚠️ Documentación menos clara  

**Recomendación:** Usar Stripe para tu tesis por mejor experiencia de testing.
