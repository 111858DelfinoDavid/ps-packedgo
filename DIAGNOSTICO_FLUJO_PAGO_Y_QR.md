# 🔍 DIAGNÓSTICO EXHAUSTIVO: FLUJO DE PAGO Y GENERACIÓN DE QR

## 📊 RESUMEN EJECUTIVO

He realizado un análisis exhaustivo del código y he identificado **EL PROBLEMA PRINCIPAL** por el cual no se están generando los QR codes después del pago con cuentas de prueba.

---

## 🚨 PROBLEMA IDENTIFICADO

### **El flujo de pago con MercadoPago NO está funcionando correctamente por FALTA DE WEBHOOK**

**El problema está en el flujo de comunicación entre los servicios:**

1. ✅ **Frontend** → Order Service: Checkout exitoso
2. ✅ **Order Service** → Payment Service: Crea preferencia de pago
3. ✅ **Payment Service** → MercadoPago: Crea preferencia
4. ✅ **Usuario** → MercadoPago: Paga con cuenta de prueba
5. ❌ **MercadoPago** → Payment Service: **NO NOTIFICA (webhook no configurado o no accesible)**
6. ❌ **Payment Service** → Order Service: **NO NOTIFICA** porque nunca recibió el webhook
7. ❌ **Order Service**: **NO MARCA LA ORDEN COMO PAID**
8. ❌ **Order Service**: **NO GENERA LOS TICKETS** (solo se generan cuando order.status = PAID)

---

## 🔎 ANÁLISIS DETALLADO DEL CÓDIGO

### 1️⃣ **Payment Service - Configuración de Webhook**

**Archivo**: `payment-service/src/main/java/com/packedgo/payment_service/service/PaymentService.java`

```java
@Value("${mercadopago.webhook.url:}")
private String webhookUrl;

// Línea 115-125:
if (webhookUrl != null && !webhookUrl.isEmpty()) {
    if (webhookUrl.startsWith("https://") || credential.getIsSandbox()) {
        String fullWebhookUrl = webhookUrl + "?adminId=" + request.getAdminId();
        preferenceBuilder.notificationUrl(fullWebhookUrl);
        log.info("Webhook configurado: {}", fullWebhookUrl);
    } else {
        log.warn("Webhook URL debe ser HTTPS en producción: {}", webhookUrl);
    }
} else {
    log.warn("Webhook URL no configurada - las notificaciones automáticas no funcionarán");
}
```

**🔴 PROBLEMA**: El webhook probablemente no está configurado o no es accesible desde MercadoPago.

### 2️⃣ **Order Service - Generación de Tickets**

**Archivo**: `order-service/src/main/java/com/packed_go/order_service/service/impl/OrderServiceImpl.java`

```java
// Línea 121-148: updateOrderFromPaymentCallback
@Override
@Transactional
public void updateOrderFromPaymentCallback(PaymentCallbackRequest request) {
    log.info("Updating order {} with payment status: {}", 
        request.getOrderNumber(), request.getPaymentStatus());
    
    Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
            .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderNumber()));
    
    switch (request.getPaymentStatus().toUpperCase()) {
        case "APPROVED":
            order.markAsPaid();
            log.info("Order {} marked as PAID", order.getOrderNumber());
            
            // 🎟️ GENERAR TICKETS cuando el pago es aprobado
            try {
                generateTicketsForOrder(order);  // ⬅️ AQUÍ SE GENERAN LOS TICKETS
            } catch (Exception e) {
                log.error("Failed to generate tickets for order {}: {}", 
                    order.getOrderNumber(), e.getMessage(), e);
            }
            break;
        // ...
    }
}
```

**✅ CORRECTO**: La lógica de generación de tickets está bien implementada, PERO solo se ejecuta cuando:
1. Payment Service recibe webhook de MercadoPago
2. Payment Service notifica a Order Service
3. Order Service marca la orden como PAID
4. Order Service genera los tickets

### 3️⃣ **Generación de Tickets**

**Archivo**: `order-service/src/main/java/com/packed_go/order_service/service/impl/OrderServiceImpl.java`

```java
// Línea 437-486: generateTicketsForOrder
private void generateTicketsForOrder(Order order) {
    log.info("🎟️ Generating tickets for order: {}", order.getOrderNumber());
    
    for (OrderItem orderItem : order.getItems()) {
        Long eventId = orderItem.getEventId();
        Integer quantity = orderItem.getQuantity();
        
        // Generar un ticket por cada entrada
        for (int i = 0; i < quantity; i++) {
            // Preparar consumiciones
            List<TicketConsumptionDTO> consumptions = orderItem.getConsumptions().stream()
                    .map(cons -> TicketConsumptionDTO.builder()
                            .ticketConsumptionId(cons.getConsumptionId())
                            .quantity(cons.getQuantity())
                            .build())
                    .collect(Collectors.toList());
            
            // Crear ticket con consumiciones
            CreateTicketWithConsumptionsRequest ticketRequest = 
                CreateTicketWithConsumptionsRequest.builder()
                    .userId(order.getUserId())
                    .eventId(eventId)
                    .consumptions(consumptions)
                    .build();
            
            TicketWithConsumptionsResponse response = 
                eventServiceClient.createTicketWithConsumptions(ticketRequest);
            
            if (response.getSuccess()) {
                log.info("✅ Ticket #{} generated: ID={}, QR={}", 
                    (i + 1), response.getTicketId(), response.getQrCode());
            }
        }
    }
}
```

**✅ CORRECTO**: Este método llama al Event Service para crear tickets con consumiciones y genera QR codes.

### 4️⃣ **Event Service - Creación de Tickets**

**Archivo**: `event-service/src/main/java/com/packed_go/event_service/controllers/TicketController.java`

```java
@PostMapping("/create-with-consumptions")
public ResponseEntity<TicketWithConsumptionsResponse> createTicketWithConsumptions(
        @Valid @RequestBody CreateTicketWithConsumptionsRequest request) {
    log.info("Creando ticket con consumiciones para usuario: {}, evento: {}", 
            request.getUserId(), request.getEventId());
    TicketWithConsumptionsResponse response = 
        ticketService.createTicketWithConsumptions(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

**✅ CORRECTO**: El endpoint existe y está listo para crear tickets.

### 5️⃣ **Frontend - Order Success Component**

**Archivo**: `front-angular/src/app/features/customer/order-success/order-success.component.ts`

```typescript
ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.sessionId = params['sessionId'];
      if (this.sessionId) {
        this.loadSessionData();
      } else {
        this.router.navigate(['/customer/dashboard']);
      }
    });
  }

loadSessionData(): void {
    this.orderService.getSessionStatus(this.sessionId).subscribe({
      next: (data) => {
        this.sessionData = data;
        this.isLoading = false;
        // Load tickets after session data is loaded
        this.loadTickets();  // ⬅️ INTENTA CARGAR TICKETS
      }
    });
  }

loadTickets(): void {
    const userId = this.authService.getUserId();
    this.ticketService.getActiveTickets(userId).subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        console.log('✅ Tickets loaded:', tickets);
      },
      error: (error) => {
        console.error('❌ Error loading tickets:', error);
      }
    });
  }
```

**⚠️ PROBLEMA SECUNDARIO**: El componente espera recibir `sessionId` en la URL cuando MercadoPago redirige, pero MercadoPago NO envía ese parámetro automáticamente.

---

## 🛠️ SOLUCIONES

### **SOLUCIÓN 1: Configurar Webhook de MercadoPago (RECOMENDADO para Producción)**

#### Opción A: Usar ngrok para desarrollo local

1. **Instalar ngrok**:
   ```bash
   # Descargar de https://ngrok.com/download
   # O con choco:
   choco install ngrok
   ```

2. **Exponer el payment-service**:
   ```bash
   ngrok http 8085
   ```

3. **Copiar la URL HTTPS** (ejemplo: `https://abc123.ngrok.io`)

4. **Configurar en application.properties**:
   ```properties
   # payment-service/src/main/resources/application.properties
   mercadopago.webhook.url=https://abc123.ngrok.io/api/payments/webhook
   ```

5. **Reiniciar payment-service**

#### Opción B: Deploy en servidor con IP pública

Si tienes un servidor con IP pública o dominio:
```properties
mercadopago.webhook.url=https://tu-dominio.com/api/payments/webhook
```

---

### **SOLUCIÓN 2: Implementar Polling Manual (TEMPORAL para Testing)**

Si no puedes configurar webhooks, implementa un endpoint para consultar manualmente el estado del pago:

#### Backend - Payment Service

**Crear endpoint de verificación manual**:

```java
// PaymentController.java
@PostMapping("/verify/{orderId}")
public ResponseEntity<?> verifyPayment(
        @PathVariable String orderId,
        @RequestHeader("Authorization") String authHeader) {
    
    try {
        String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
        if (!jwtTokenValidator.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Long userId = jwtTokenValidator.getUserIdFromToken(token);
        
        // Buscar el pago por orderId
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        
        // Si el pago tiene mpPaymentId, verificar estado en MercadoPago
        if (payment.getMpPaymentId() != null) {
            paymentService.processWebhookNotification(
                payment.getAdminId(), 
                payment.getMpPaymentId()
            );
        }
        
        return ResponseEntity.ok(Map.of(
            "status", payment.getStatus().name(),
            "verified", true
        ));
        
    } catch (Exception e) {
        log.error("Error verifying payment", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}
```

#### Frontend - Order Success Component

**Agregar verificación automática**:

```typescript
loadSessionData(): void {
    this.orderService.getSessionStatus(this.sessionId).subscribe({
      next: (data) => {
        this.sessionData = data;
        this.isLoading = false;
        
        // Si hay órdenes pendientes, verificar pagos
        const pendingOrders = data.paymentGroups?.filter(
          group => group.status === 'PENDING_PAYMENT'
        );
        
        if (pendingOrders && pendingOrders.length > 0) {
          // Esperar 2 segundos y verificar
          setTimeout(() => {
            this.verifyPendingPayments(pendingOrders);
          }, 2000);
        }
        
        this.loadTickets();
      }
    });
  }

verifyPendingPayments(orders: any[]): void {
    orders.forEach(order => {
      this.paymentService.verifyPayment(order.orderNumber).subscribe({
        next: (result) => {
          console.log('Payment verified:', result);
          // Recargar sesión
          this.loadSessionData();
        },
        error: (err) => {
          console.error('Error verifying payment:', err);
        }
      });
    });
  }
```

---

### **SOLUCIÓN 3: Simular Webhook Manualmente (SOLO PARA TESTING)**

Crear un endpoint de desarrollo que simule el callback de MercadoPago:

```java
// PaymentController.java
@PostMapping("/dev/simulate-webhook/{orderId}")
public ResponseEntity<?> simulateWebhook(@PathVariable String orderId) {
    
    // SOLO EN DESARROLLO
    if (!environment.acceptsProfiles("dev", "local")) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
    
    try {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        
        // Simular aprobación
        payment.setStatus(Payment.PaymentStatus.APPROVED);
        payment.setMpPaymentId(System.currentTimeMillis()); // ID falso
        paymentRepository.save(payment);
        
        // Notificar a order-service
        orderServiceClient.notifyPaymentApproved(orderId, payment.getMpPaymentId());
        
        return ResponseEntity.ok(Map.of("message", "Webhook simulado"));
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
    }
}
```

**Llamar desde Postman después de pagar**:
```
POST http://localhost:8085/api/payments/dev/simulate-webhook/ORD-202511-123
```

---

## 🧪 CÓMO PROBAR EL FLUJO COMPLETO

### 1. **Verificar que todos los servicios estén corriendo**

```powershell
# Terminal 1 - Auth Service (puerto 8081)
cd packedgo\back\auth-service
.\mvnw spring-boot:run

# Terminal 2 - Users Service (puerto 8082)
cd packedgo\back\users-service
.\mvnw spring-boot:run

# Terminal 3 - Order Service (puerto 8084)
cd packedgo\back\order-service
.\mvnw spring-boot:run

# Terminal 4 - Payment Service (puerto 8085)
cd packedgo\back\payment-service
.\mvnw spring-boot:run

# Terminal 5 - Event Service (puerto 8086)
cd packedgo\back\event-service
.\mvnw spring-boot:run

# Terminal 6 - Consumption Service (puerto 8087)
cd packedgo\back\consumption-service
.\mvnw spring-boot:run

# Terminal 7 - Frontend Angular
cd packedgo\front-angular
npm start
```

### 2. **Configurar Webhook (Opción A: ngrok)**

```powershell
# Terminal 8
ngrok http 8085
```

Copiar la URL HTTPS y configurar en `payment-service/src/main/resources/application.properties`:
```properties
mercadopago.webhook.url=https://TU-URL-NGROK.ngrok.io/api/payments/webhook
```

Reiniciar payment-service.

### 3. **Probar el flujo**

1. Abrir navegador en `http://localhost:4200`
2. Registrarse/Iniciar sesión como CUSTOMER
3. Agregar items al carrito
4. Hacer checkout
5. Pagar con cuenta de prueba de MercadoPago:
   - Tarjeta: `5031 7557 3453 0604`
   - CVV: `123`
   - Fecha: Cualquier fecha futura
   - Nombre: Cualquiera
6. Esperar redirección
7. Ver tickets en "Mis Entradas"

### 4. **Verificar logs**

**Payment Service logs** (debe mostrar):
```
✅ Webhook configurado: https://xxx.ngrok.io/api/payments/webhook
✅ Procesando webhook para MercadoPago payment: 12345678
✅ Webhook procesado. Pago 1 actualizado: PENDING -> APPROVED
✅ Notificando aprobación de pago a order-service: orderId=ORD-202511-001
```

**Order Service logs** (debe mostrar):
```
✅ Updating order ORD-202511-001 with payment status: APPROVED
✅ Order ORD-202511-001 marked as PAID
🎟️ Generating tickets for order: ORD-202511-001
✅ Ticket #1 generated: ID=1, QR=data:image/png;base64,...
```

---

## 🎯 PRÓXIMOS PASOS: SISTEMA DE EMPLEADOS

Una vez resuelto el problema de pagos, necesitas implementar:

### 1. **Backend - Nuevo User Role: EMPLOYEE**

```java
// users-service/src/main/java/com/packedgo/user_service/models/ROLE.java
public enum ROLE {
    CUSTOMER, 
    ADMIN,
    EMPLOYEE  // ⬅️ NUEVO
}
```

### 2. **Backend - Employee Credentials (auth-service)**

```java
@Entity
@Table(name = "employee_credentials")
public class EmployeeCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long employeeId;  // FK a users
    private Long adminId;     // FK al admin que lo creó
    private String username;
    private String passwordHash;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
```

### 3. **Backend - Employee Dashboard Endpoints**

```java
// event-service/src/main/java/com/packed_go/event_service/controllers/EmployeeController.java
@RestController
@RequestMapping("/event-service/employee")
@RequiredArgsConstructor
public class EmployeeController {
    
    @PostMapping("/scan-qr")
    public ResponseEntity<?> scanQR(@RequestBody ScanQRRequest request) {
        // Validar QR
        // Mostrar info del ticket
        return ResponseEntity.ok(ticketInfo);
    }
    
    @PostMapping("/redeem-consumption")
    public ResponseEntity<?> redeemConsumption(@RequestBody RedeemRequest request) {
        // Marcar consumición como canjeada
        return ResponseEntity.ok(result);
    }
}
```

### 4. **Frontend - Employee Dashboard**

Crear nuevos componentes:
- `employee-login.component.ts`
- `employee-dashboard.component.ts`
- `employee-scan-qr.component.ts`

---

## 📝 CONCLUSIONES

### ✅ **El código está BIEN IMPLEMENTADO**

La arquitectura y la lógica de negocio son correctas:
- ✅ Order Service crea órdenes correctamente
- ✅ Payment Service se integra con MercadoPago
- ✅ Event Service puede crear tickets con QR
- ✅ Frontend tiene la UI para mostrar tickets

### ❌ **El problema es de INFRAESTRUCTURA**

El webhook de MercadoPago no puede notificar al backend local porque:
1. No está configurado (`mercadopago.webhook.url` vacío)
2. O no es accesible desde Internet (localhost no es accesible por MercadoPago)

### 🚀 **Solución inmediata**

Usa **ngrok** para exponer el payment-service y configura el webhook.

### 📋 **Checklist de verificación**

- [ ] Todos los servicios corriendo (8081, 8082, 8084, 8085, 8086, 8087)
- [ ] ngrok corriendo en puerto 8085
- [ ] `mercadopago.webhook.url` configurado con URL de ngrok
- [ ] Payment service reiniciado
- [ ] Credenciales de MercadoPago válidas en BD
- [ ] Hacer checkout y pagar
- [ ] Verificar logs de payment-service y order-service
- [ ] Verificar que tickets aparecen en frontend

---

¿Necesitas ayuda implementando alguna de estas soluciones? 🚀
