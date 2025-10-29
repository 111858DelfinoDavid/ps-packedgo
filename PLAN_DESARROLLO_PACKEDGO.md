# 📋 PLAN DE DESARROLLO COMPLETO - PackedGo
## Proyecto Final Integrador - UTN FRC

**Fecha:** Octubre 2025  
**Equipo:** David Delfino & Agustín Luparia  
**Estado Actual:** Microservicios base implementados, falta integración completa

---

## 🎯 OBJETIVO GENERAL

Completar la implementación de PackedGo como plataforma SaaS Multi-Tenant funcional, integrando todos los microservicios para lograr un flujo completo desde la compra de entradas hasta el canje de consumiciones en eventos.

---

## 📊 ESTADO ACTUAL DEL PROYECTO

### ✅ Completado (70%)
- **AUTH-SERVICE**: Autenticación diferenciada (Admin/Customer) con JWT
- **USERS-SERVICE**: Gestión de perfiles de usuario
- **EVENT-SERVICE**: 
  - Entidades: Event, Pass, Ticket, TicketConsumption, Consumption
  - CRUD de eventos y consumiciones (parcial)
  - Endpoints de tickets implementados (`POST /tickets`, `GET /tickets/{id}`, etc.)
  - Optimistic locking con `@Version` en todas las entidades críticas
- **ORDER-SERVICE**: 
  - Carrito de compras (ShoppingCart, CartItem, CartItemConsumption)
  - Endpoints de carrito funcionales
- **Frontend Angular**: Estructura base con login/register funcional

### 🚧 En Progreso / Incompleto (30%)
- **EVENT-SERVICE**: 
  - ❌ Falta generación automática de Passes
  - ❌ Falta validación de `createdBy` en endpoints de admin
  - ❌ Consumption sin campo `createdBy` (seguridad multi-tenant)
  - ❌ Falta endpoint de reserva de Passes
  - ❌ Falta validación de stock antes de checkout
- **ORDER-SERVICE**: 
  - ❌ NO tiene entidad Order (solo tiene carrito)
  - ❌ Falta proceso de checkout completo
- **PAYMENT-SERVICE**: ❌ NO implementado (lo está haciendo el compañero)
- **CONSUMPTION-SERVICE (QR)**: ❌ NO implementado
- **API-GATEWAY**: ❌ NO implementado (se hará al final)
- **Sistema de Empleados**: ❌ NO implementado

---

## 🗓️ FASES DE DESARROLLO

---

## 📦 FASE 1: FIXES CRÍTICOS Y SEGURIDAD MULTI-TENANT
**Duración:** 1-2 semanas  
**Prioridad:** 🔴 CRÍTICA  
**Objetivo:** Corregir problemas de seguridad y funcionalidades faltantes en EVENT-SERVICE

### 1.1 Agregar `createdBy` a Consumption (CRÍTICO)
**Archivo:** `event-service/entities/Consumption.java`

**Problema:** Sin `createdBy`, cualquier organizador puede ver consumiciones de otros.

**Tareas:**
1. Agregar campo `createdBy` a entidad Consumption
2. Modificar `ConsumptionController` para asignar `createdBy` del JWT al crear
3. Agregar endpoint `GET /my-consumptions` que filtre por `createdBy`
4. Validar propiedad en `PUT /consumptions/{id}` y `DELETE /consumptions/{id}`
5. Migración de base de datos: agregar columna con valor default (asignar a admin ID 1 temporalmente)

**Código a implementar:**
```java
// Consumption.java - Agregar campo
@Column(nullable = false)
private Long createdBy;

// ConsumptionController.java - Crear con createdBy
@PostMapping
public ResponseEntity<ConsumptionDTO> create(
    @RequestHeader("Authorization") String authHeader,
    @RequestBody CreateConsumptionDTO dto) {
    Long organizerId = jwtTokenValidator.getUserIdFromToken(authHeader.substring(7));
    ConsumptionDTO created = service.createConsumption(dto, organizerId);
    return ResponseEntity.ok(created);
}

// ConsumptionService.java - Validar propiedad
public ConsumptionDTO updateConsumption(Long id, UpdateConsumptionDTO dto, Long organizerId) {
    Consumption consumption = repository.findById(id).orElseThrow();
    if (!consumption.getCreatedBy().equals(organizerId)) {
        throw new UnauthorizedException("No puedes modificar consumiciones de otros organizadores");
    }
    // ... continuar actualización
}
```

**Archivos a modificar:**
- `event-service/src/main/java/com/packed_go/event_service/entities/Consumption.java`
- `event-service/src/main/java/com/packed_go/event_service/controllers/ConsumptionController.java`
- `event-service/src/main/java/com/packed_go/event_service/services/ConsumptionService.java`
- `event-service/src/main/java/com/packed_go/event_service/services/ConsumptionServiceImpl.java`
- `event-service/src/main/java/com/packed_go/event_service/repositories/ConsumptionRepository.java` (agregar `findByCreatedBy()`)

---

### 1.2 Validar `createdBy` en Event Controller
**Archivo:** `event-service/controllers/EventController.java`

**Problema:** Actualmente NO valida que el admin solo pueda modificar sus propios eventos.

**Tareas:**
1. Agregar `@RequestHeader("Authorization")` a endpoints de admin
2. Extraer `userId` del JWT con `jwtTokenValidator`
3. Validar `event.createdBy == userId` en UPDATE/DELETE
4. Agregar endpoint `GET /my-events` para listar eventos del organizador

**Código a implementar:**
```java
@PutMapping("/{id}")
public ResponseEntity<EventDTO> update(
    @RequestHeader("Authorization") String authHeader,
    @PathVariable Long id, 
    @RequestBody UpdateEventDTO dto) {
    
    Long organizerId = jwtTokenValidator.getUserIdFromToken(authHeader.substring(7));
    EventDTO updated = service.updateEvent(id, dto, organizerId);
    return ResponseEntity.ok(updated);
}

@GetMapping("/my-events")
public ResponseEntity<List<EventDTO>> getMyEvents(
    @RequestHeader("Authorization") String authHeader) {
    Long organizerId = jwtTokenValidator.getUserIdFromToken(authHeader.substring(7));
    return ResponseEntity.ok(service.findByCreatedBy(organizerId));
}
```

**Archivos a modificar:**
- `event-service/src/main/java/com/packed_go/event_service/controllers/EventController.java`
- `event-service/src/main/java/com/packed_go/event_service/services/EventService.java`
- `event-service/src/main/java/com/packed_go/event_service/services/EventServiceImpl.java`

---

### 1.3 Implementar Generación Automática de Passes
**Archivos nuevos:** `event-service/services/PassGenerationService.java`

**Problema:** No existe lógica para generar los Passes con códigos únicos al crear un evento.

**Tareas:**
1. Crear `PassGenerationService` con método `generatePassesForEvent()`
2. Generar códigos únicos formato: `PKG-{eventId}-{timestamp}-{random8chars}`
3. Llamar automáticamente al crear evento si `maxCapacity > 0`
4. Actualizar contadores `totalPasses` y `availablePasses` en Event

**Código a implementar:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PassGenerationService {
    
    private final PassRepository passRepository;
    private final EventRepository eventRepository;
    
    @Transactional
    public List<Pass> generatePassesForEvent(Long eventId, Integer quantity) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));
        
        List<Pass> newPasses = new ArrayList<>();
        
        for (int i = 0; i < quantity; i++) {
            String code = generateUniqueCode(eventId);
            Pass pass = new Pass(code, event);
            newPasses.add(pass);
        }
        
        List<Pass> savedPasses = passRepository.saveAll(newPasses);
        
        // Actualizar contadores
        event.setTotalPasses(event.getTotalPasses() + quantity);
        event.setAvailablePasses(event.getAvailablePasses() + quantity);
        eventRepository.save(event);
        
        log.info("✅ Generated {} passes for event {}", quantity, eventId);
        return savedPasses;
    }
    
    private String generateUniqueCode(Long eventId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("PKG-%d-%s-%s", eventId, timestamp, random);
    }
}

// EventServiceImpl.java - Llamar al crear evento
@Transactional
public EventDTO createEvent(CreateEventDTO dto, Long organizerId) {
    Event event = mapper.map(dto, Event.class);
    event.setCreatedBy(organizerId);
    Event savedEvent = eventRepository.save(event);
    
    // ✅ Generar passes automáticamente
    if (dto.getMaxCapacity() != null && dto.getMaxCapacity() > 0) {
        passGenerationService.generatePassesForEvent(savedEvent.getId(), dto.getMaxCapacity());
    }
    
    return mapper.map(savedEvent, EventDTO.class);
}
```

**Archivos a crear/modificar:**
- **NUEVO:** `event-service/src/main/java/com/packed_go/event_service/services/PassGenerationService.java`
- `event-service/src/main/java/com/packed_go/event_service/services/EventServiceImpl.java`

---

### 1.4 Proteger Edición de Capacidad de Evento
**Archivo:** `event-service/services/EventServiceImpl.java`

**Problema:** Si se reduce `maxCapacity`, podría invalidar entradas ya vendidas.

**Solución:** Solo permitir AUMENTAR capacidad, nunca reducir.

**Código a implementar:**
```java
public EventDTO updateEvent(Long id, UpdateEventDTO dto, Long organizerId) {
    Event event = eventRepository.findById(id).orElseThrow();
    
    // Validar propiedad
    if (!event.getCreatedBy().equals(organizerId)) {
        throw new UnauthorizedException("No puedes modificar eventos de otros organizadores");
    }
    
    // ⚠️ Validar capacidad - Solo permitir aumentar
    if (dto.getMaxCapacity() != null) {
        if (dto.getMaxCapacity() < event.getTotalPasses()) {
            throw new InvalidCapacityException(
                "No puedes reducir la capacidad. Actual: " + event.getTotalPasses() + 
                " entradas generadas. Solo puedes aumentar."
            );
        }
        
        // Si aumenta, generar más passes
        if (dto.getMaxCapacity() > event.getTotalPasses()) {
            int newPasses = dto.getMaxCapacity() - event.getTotalPasses();
            passGenerationService.generatePassesForEvent(id, newPasses);
        }
        
        event.setMaxCapacity(dto.getMaxCapacity());
    }
    
    // Actualizar otros campos...
    return mapper.map(eventRepository.save(event), EventDTO.class);
}
```

---

### 1.5 Implementar Reserva Temporal de Passes (CRÍTICO)
**Archivos:** `event-service/services/PassService.java`, `PassServiceImpl.java`

**Problema:** Sin reserva, el mismo pass podría venderse a 2 usuarios si pagan simultáneamente.

**Solución:** Reservar pass antes de ir a MercadoPago, liberar si expira.

**Tareas:**
1. Agregar enum `PassStatus` (AVAILABLE, RESERVED, SOLD, EXPIRED)
2. Agregar campos `reservedByUserId`, `reservedAt`, `reservationExpiresAt` a Pass
3. Crear método `reservePass()` que valida disponibilidad y reserva
4. Crear método `confirmSale()` que convierte RESERVED → SOLD
5. Crear método `releaseReservation()` que libera reservas expiradas
6. Crear `@Scheduled` job para limpiar reservas expiradas cada minuto

**Código a implementar:**
```java
// Pass.java - Agregar campos
public enum PassStatus {
    AVAILABLE, RESERVED, SOLD, EXPIRED
}

@Enumerated(EnumType.STRING)
private PassStatus status = PassStatus.AVAILABLE;
private Long reservedByUserId;
private LocalDateTime reservedAt;
private LocalDateTime reservationExpiresAt;

// PassServiceImpl.java - Reservar pass
@Transactional
public PassDTO reservePass(Long passId, Long userId, int reservationMinutes) {
    Pass pass = passRepository.findById(passId).orElseThrow();
    
    if (pass.getStatus() != PassStatus.AVAILABLE) {
        throw new PassNotAvailableException("Pass no disponible");
    }
    
    pass.setStatus(PassStatus.RESERVED);
    pass.setReservedByUserId(userId);
    pass.setReservedAt(LocalDateTime.now());
    pass.setReservationExpiresAt(LocalDateTime.now().plusMinutes(reservationMinutes));
    
    Pass savedPass = passRepository.save(pass);
    return mapper.map(savedPass, PassDTO.class);
}

// PassReservationCleanupService.java - Job para liberar reservas
@Service
@RequiredArgsConstructor
@Slf4j
public class PassReservationCleanupService {
    
    private final PassRepository passRepository;
    
    @Scheduled(fixedRate = 60000) // Cada 1 minuto
    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Pass> expiredPasses = passRepository
            .findByStatusAndReservationExpiresAtBefore(PassStatus.RESERVED, now);
        
        expiredPasses.forEach(pass -> {
            pass.setStatus(PassStatus.AVAILABLE);
            pass.setReservedByUserId(null);
            pass.setReservedAt(null);
            pass.setReservationExpiresAt(null);
        });
        
        passRepository.saveAll(expiredPasses);
        log.info("🧹 Released {} expired pass reservations", expiredPasses.size());
    }
}
```

**Archivos a crear/modificar:**
- `event-service/src/main/java/com/packed_go/event_service/entities/Pass.java` (agregar campos)
- `event-service/src/main/java/com/packed_go/event_service/enums/PassStatus.java` (NUEVO)
- `event-service/src/main/java/com/packed_go/event_service/services/PassService.java`
- `event-service/src/main/java/com/packed_go/event_service/services/PassServiceImpl.java`
- **NUEVO:** `event-service/src/main/java/com/packed_go/event_service/services/PassReservationCleanupService.java`
- `event-service/src/main/java/com/packed_go/event_service/repositories/PassRepository.java` (agregar query)

---

### 1.6 Endpoint de Verificación y Reserva de Stock
**Archivo:** `event-service/controllers/PassController.java`

**Problema:** ORDER-SERVICE necesita verificar disponibilidad antes de checkout.

**Tareas:**
1. Crear endpoint `POST /passes/check-and-reserve`
2. Valida disponibilidad de N passes para un evento
3. Reserva los passes si están disponibles
4. Retorna lista de códigos de pass reservados

**Código a implementar:**
```java
@PostMapping("/check-and-reserve")
public ResponseEntity<PassReservationResponse> checkAndReserve(
    @RequestBody PassReservationRequest request) {
    
    log.info("Verificando y reservando {} passes para evento {}", 
        request.getQuantity(), request.getEventId());
    
    PassReservationResponse response = passService.checkAndReserve(
        request.getEventId(),
        request.getUserId(),
        request.getQuantity(),
        15 // 15 minutos de reserva
    );
    
    return ResponseEntity.ok(response);
}

// PassServiceImpl.java
@Transactional
public PassReservationResponse checkAndReserve(Long eventId, Long userId, int quantity, int minutes) {
    // Buscar passes disponibles
    List<Pass> availablePasses = passRepository
        .findByEventIdAndStatus(eventId, PassStatus.AVAILABLE, PageRequest.of(0, quantity));
    
    if (availablePasses.size() < quantity) {
        throw new InsufficientStockException(
            "Solo hay " + availablePasses.size() + " entradas disponibles"
        );
    }
    
    // Reservar todos
    List<String> reservedCodes = new ArrayList<>();
    for (Pass pass : availablePasses) {
        pass.setStatus(PassStatus.RESERVED);
        pass.setReservedByUserId(userId);
        pass.setReservedAt(LocalDateTime.now());
        pass.setReservationExpiresAt(LocalDateTime.now().plusMinutes(minutes));
        reservedCodes.add(pass.getCode());
    }
    
    passRepository.saveAll(availablePasses);
    
    return new PassReservationResponse(true, reservedCodes, LocalDateTime.now().plusMinutes(minutes));
}
```

**Archivos a crear/modificar:**
- `event-service/src/main/java/com/packed_go/event_service/controllers/PassController.java`
- `event-service/src/main/java/com/packed_go/event_service/services/PassService.java`
- `event-service/src/main/java/com/packed_go/event_service/services/PassServiceImpl.java`
- **NUEVO:** `event-service/src/main/java/com/packed_go/event_service/dtos/pass/PassReservationRequest.java`
- **NUEVO:** `event-service/src/main/java/com/packed_go/event_service/dtos/pass/PassReservationResponse.java`

---

### ✅ Checklist Fase 1
- [ ] Agregar `createdBy` a Consumption + migración DB
- [ ] Validar propiedad en ConsumptionController (create, update, delete)
- [ ] Validar `createdBy` en EventController (update, delete)
- [ ] Crear endpoint `GET /my-events` y `GET /my-consumptions`
- [ ] Implementar `PassGenerationService`
- [ ] Llamar generación de passes al crear evento
- [ ] Proteger edición de `maxCapacity` (solo aumentar)
- [ ] Agregar campos de reserva a Pass (status, reservedBy, etc.)
- [ ] Implementar métodos de reserva en PassService
- [ ] Crear job `PassReservationCleanupService`
- [ ] Crear endpoint `POST /passes/check-and-reserve`
- [ ] Testing de concurrencia (2 usuarios comprando último pass)

---

## 📦 FASE 2: COMPLETAR ORDER-SERVICE
**Duración:** 1 semana  
**Prioridad:** 🔴 ALTA  
**Objetivo:** Implementar entidad Order y proceso de checkout

### 2.1 Crear Entidad Order
**Archivo:** `order-service/entity/Order.java`

**Tareas:**
1. Crear entidad Order con campos: `orderNumber`, `userId`, `status`, `total`, `paymentId`
2. Crear enum `OrderStatus` (PENDING, RESERVED, PAID, EXPIRED, CANCELLED, REFUNDED)
3. Relación OneToMany con OrderItem
4. Generar `orderNumber` único formato: `ORD-YYYYMMDD-XXXXXX`

**Código a implementar:**
```java
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String orderNumber; // ORD-20251028-123456
    
    @Column(nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal taxes = BigDecimal.ZERO;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;
    
    private String paymentId; // ID de MercadoPago
    
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = LocalDateTime.now().plusMinutes(15); // 15 min para pagar
        if (orderNumber == null) {
            orderNumber = generateOrderNumber();
        }
    }
    
    private String generateOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%06d", new Random().nextInt(999999));
        return "ORD-" + date + "-" + random;
    }
    
    public void markAsPaid(String mercadoPagoId) {
        this.status = OrderStatus.PAID;
        this.paymentId = mercadoPagoId;
        this.paidAt = LocalDateTime.now();
    }
}

public enum OrderStatus {
    PENDING,    // Orden creada, esperando pago
    RESERVED,   // Passes reservados, esperando pago
    PAID,       // Pagado exitosamente
    EXPIRED,    // Tiempo de pago expirado
    CANCELLED,  // Cancelada por el usuario
    REFUNDED    // Dinero devuelto
}
```

**Archivos a crear:**
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/entity/Order.java`
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/enums/OrderStatus.java`

---

### 2.2 Crear Entidad OrderItem
**Archivo:** `order-service/entity/OrderItem.java`

**Tareas:**
1. Representa cada entrada (ticket) en la orden
2. Incluye información del evento y precio del momento de compra
3. Relación OneToMany con OrderItemConsumption

**Código a implementar:**
```java
@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(nullable = false)
    private Long eventId;
    
    @Column(nullable = false)
    private String eventName;
    
    @Column(nullable = false)
    private String passCode; // Código del pass reservado
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;
    
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemConsumption> consumptions = new ArrayList<>();
    
    public BigDecimal calculateTotal() {
        BigDecimal consumptionsTotal = consumptions.stream()
            .map(OrderItemConsumption::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ticketPrice.add(consumptionsTotal);
    }
}
```

**Archivos a crear:**
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/entity/OrderItem.java`
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/entity/OrderItemConsumption.java`

---

### 2.3 Implementar Proceso de Checkout
**Archivo:** `order-service/service/CheckoutService.java`

**Flujo completo:**
1. Usuario hace checkout desde carrito
2. Verificar disponibilidad y reservar passes en EVENT-SERVICE
3. Crear Order con status=RESERVED
4. Convertir CartItems → OrderItems
5. Retornar Order para pasarla a PAYMENT-SERVICE

**Código a implementar:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {
    
    private final ShoppingCartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final EventServiceClient eventServiceClient;
    
    @Transactional
    public CheckoutResponse checkout(Long userId, String authHeader) {
        // 1. Obtener carrito activo
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
            .orElseThrow(() -> new CartNotFoundException());
        
        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException();
        }
        
        // 2. Agrupar items por evento
        Map<Long, List<CartItem>> itemsByEvent = cart.getItems().stream()
            .collect(Collectors.groupingBy(CartItem::getEventId));
        
        // 3. Verificar y reservar passes para cada evento
        List<PassReservationResponse> reservations = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : itemsByEvent.entrySet()) {
            Long eventId = entry.getKey();
            int quantity = entry.getValue().size();
            
            PassReservationResponse reservation = eventServiceClient.checkAndReservePasses(
                eventId, userId, quantity, authHeader
            );
            
            if (!reservation.isSuccess()) {
                throw new InsufficientStockException(
                    "No hay suficientes entradas para el evento " + eventId
                );
            }
            
            reservations.add(reservation);
        }
        
        // 4. Crear Order
        Order order = Order.builder()
            .userId(userId)
            .status(OrderStatus.RESERVED)
            .subtotal(cart.getTotalAmount())
            .taxes(BigDecimal.ZERO)
            .total(cart.getTotalAmount())
            .build();
        
        // 5. Crear OrderItems con pass codes reservados
        int passIndex = 0;
        for (CartItem cartItem : cart.getItems()) {
            String passCode = getPassCodeForEvent(reservations, cartItem.getEventId(), passIndex++);
            
            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .eventId(cartItem.getEventId())
                .eventName(cartItem.getEventName())
                .passCode(passCode)
                .ticketPrice(cartItem.getUnitPrice())
                .build();
            
            // Copiar consumiciones
            for (CartItemConsumption cartCons : cartItem.getConsumptions()) {
                OrderItemConsumption orderCons = OrderItemConsumption.builder()
                    .orderItem(orderItem)
                    .consumptionId(cartCons.getConsumptionId())
                    .consumptionName(cartCons.getConsumptionName())
                    .quantity(cartCons.getQuantity())
                    .unitPrice(cartCons.getUnitPrice())
                    .subtotal(cartCons.getSubtotal())
                    .build();
                orderItem.getConsumptions().add(orderCons);
            }
            
            order.getItems().add(orderItem);
        }
        
        Order savedOrder = orderRepository.save(order);
        
        // 6. Marcar carrito como CHECKED_OUT
        cart.markAsCheckedOut();
        cartRepository.save(cart);
        
        log.info("✅ Checkout completado. Order: {}, Total: ${}", 
            savedOrder.getOrderNumber(), savedOrder.getTotal());
        
        return new CheckoutResponse(
            savedOrder.getOrderNumber(),
            savedOrder.getTotal(),
            savedOrder.getExpiresAt()
        );
    }
    
    private String getPassCodeForEvent(List<PassReservationResponse> reservations, 
                                       Long eventId, int index) {
        return reservations.stream()
            .filter(r -> r.getEventId().equals(eventId))
            .findFirst()
            .map(r -> r.getReservedCodes().get(index))
            .orElseThrow();
    }
}
```

**Archivos a crear/modificar:**
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/service/CheckoutService.java`
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/controller/CheckoutController.java`
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/external/EventServiceClient.java` (WebClient)
- **NUEVO:** `order-service/src/main/java/com/packed_go/order_service/dto/CheckoutResponse.java`

---

### 2.4 Cliente HTTP para EVENT-SERVICE
**Archivo:** `order-service/external/EventServiceClient.java`

**Tareas:**
1. Usar WebClient para comunicarse con EVENT-SERVICE
2. Métodos: `checkAndReservePasses()`, `getEventDetails()`

**Código a implementar:**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceClient {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${services.event-service.url:http://event-service:8086}")
    private String eventServiceUrl;
    
    public PassReservationResponse checkAndReservePasses(
            Long eventId, Long userId, int quantity, String authHeader) {
        
        PassReservationRequest request = PassReservationRequest.builder()
            .eventId(eventId)
            .userId(userId)
            .quantity(quantity)
            .build();
        
        return webClientBuilder.build()
            .post()
            .uri(eventServiceUrl + "/api/event-service/passes/check-and-reserve")
            .header("Authorization", authHeader)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PassReservationResponse.class)
            .block();
    }
}
```

---

### ✅ Checklist Fase 2
- [ ] Crear entidad Order
- [ ] Crear enum OrderStatus
- [ ] Crear entidad OrderItem
- [ ] Crear entidad OrderItemConsumption
- [ ] Implementar CheckoutService
- [ ] Crear EventServiceClient (WebClient)
- [ ] Crear CheckoutController con endpoint `POST /checkout`
- [ ] Testing de checkout completo
- [ ] Validar que carrito se marca CHECKED_OUT

---

## 📦 FASE 3: SISTEMA DE EMPLEADOS
**Duración:** 1 semana  
**Prioridad:** 🟡 MEDIA  
**Objetivo:** Permitir que empleados validen QRs en eventos

### 3.1 Agregar Rol EMPLOYEE a AUTH-SERVICE
**Archivo:** `auth-service/entities/AuthUser.java`

**Tareas:**
1. Agregar `EMPLOYEE` al enum de roles
2. Crear endpoint de registro de empleados
3. Empleados se autentican igual que admins (email + password)

**Código a implementar:**
```java
public enum UserRole {
    CUSTOMER,      // Cliente que compra entradas
    ADMIN,         // Organizador de eventos
    EMPLOYEE,      // Empleado que valida QRs
    SUPER_ADMIN    // Administrador de la plataforma
}
```

---

### 3.2 Crear Entidad EventEmployee
**Archivo:** `event-service/entity/EventEmployee.java`

**Tareas:**
1. Asocia un empleado (userId) con un evento específico
2. Admin asigna empleados a sus eventos
3. Empleado solo puede ver/validar eventos donde está asignado

**Código a implementar:**
```java
@Entity
@Table(name = "event_employees")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEmployee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long eventId;
    
    @Column(nullable = false)
    private Long employeeUserId; // ID de auth_users (rol EMPLOYEE)
    
    @Column(nullable = false)
    private Long assignedBy; // ID del admin que lo asignó
    
    private LocalDateTime assignedAt;
    
    @Column(nullable = false)
    private boolean active = true;
    
    // Permisos
    private boolean canValidateEntry = true;
    private boolean canValidateConsumptions = true;
    
    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}
```

---

### 3.3 Endpoints de Gestión de Empleados
**Archivo:** `event-service/controllers/EmployeeController.java`

**Endpoints:**
- `POST /events/{eventId}/employees` - Admin asigna empleado a evento
- `GET /events/{eventId}/employees` - Listar empleados de un evento
- `DELETE /events/{eventId}/employees/{employeeId}` - Remover empleado
- `GET /employees/my-events` - Empleado ve sus eventos asignados

---

### ✅ Checklist Fase 3
- [ ] Agregar rol EMPLOYEE a AUTH-SERVICE
- [ ] Crear entidad EventEmployee
- [ ] Endpoint para asignar empleado a evento
- [ ] Endpoint para listar empleados de evento
- [ ] Endpoint para que empleado vea sus eventos
- [ ] Validar que solo el admin dueño del evento puede asignar empleados

---

## 📦 FASE 4: CONSUMPTION-SERVICE (QR)
**Duración:** 1-2 semanas  
**Prioridad:** 🔴 ALTA  
**Objetivo:** Generación y validación de QR codes

### 4.1 Generar QR Codes para Tickets
**Tecnología:** Librería `zxing` (Google)

**Tareas:**
1. Al crear ticket, generar QR único
2. QR contiene: `{ticketId}-{eventId}-{userId}-{hash}`
3. Almacenar QR como imagen Base64 o generar on-the-fly

**Dependencia Maven:**
```xml
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

**Código a implementar:**
```java
@Service
@RequiredArgsConstructor
public class QRCodeGenerationService {
    
    public String generateQRCode(Ticket ticket) throws Exception {
        String qrContent = buildQRContent(ticket);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            qrContent, 
            BarcodeFormat.QR_CODE, 
            300, 300
        );
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        byte[] qrBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(qrBytes);
    }
    
    private String buildQRContent(Ticket ticket) {
        String data = String.format("%d-%d-%d", 
            ticket.getId(), 
            ticket.getPass().getEvent().getId(),
            ticket.getUserId()
        );
        String hash = DigestUtils.sha256Hex(data + SECRET_KEY);
        return data + "-" + hash;
    }
}
```

---

### 4.2 Validar QR al Ingresar al Evento
**Endpoint:** `POST /consumption-service/qr/validate-entry`

**Flujo:**
1. Empleado escanea QR
2. CONSUMPTION-SERVICE valida formato y firma
3. Llama a EVENT-SERVICE para marcar `Ticket.redeemed = true`
4. Retorna datos del usuario y del evento

---

### 4.3 Validar QR al Canjear Consumición
**Endpoint:** `POST /consumption-service/qr/redeem-consumption`

**Flujo:**
1. Empleado escanea QR + selecciona consumición
2. CONSUMPTION-SERVICE valida QR
3. Llama a EVENT-SERVICE para marcar `TicketConsumptionDetail.redeem = true`
4. Verifica que esa consumición específica no haya sido canjeada

---

### ✅ Checklist Fase 4
- [ ] Agregar dependencia zxing
- [ ] Implementar QRCodeGenerationService
- [ ] Generar QR al crear ticket (en EVENT-SERVICE o CONSUMPTION-SERVICE)
- [ ] Endpoint de validación de entrada (CONSUMPTION-SERVICE)
- [ ] Endpoint de canje de consumición (CONSUMPTION-SERVICE)
- [ ] Cliente HTTP para comunicarse con EVENT-SERVICE
- [ ] Testing de validación de QR

---

## 📦 FASE 5: INTEGRACIÓN CON PAYMENT-SERVICE
**Duración:** Variable (depende del compañero)  
**Prioridad:** 🔴 CRÍTICA  
**Objetivo:** Completar flujo de pago con MercadoPago

**NOTA:** Esta fase la está desarrollando tu compañero David. Aquí defino la integración esperada.

### 5.1 Contrato de Integración ORDER ↔ PAYMENT

**ORDER-SERVICE llama a PAYMENT-SERVICE:**
```
POST /payment-service/create-preference
Headers: Authorization: Bearer {jwt}
Body: {
  "orderId": 123,
  "userId": 456,
  "amount": 15000.00,
  "description": "Orden ORD-20251028-123456",
  "items": [
    {
      "title": "Entrada - Evento Rock Fest",
      "quantity": 2,
      "unit_price": 5000.00
    }
  ]
}

Response: {
  "preferenceId": "12345-abcde-67890",
  "initPoint": "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=...",
  "sandboxInitPoint": "https://sandbox.mercadopago.com.ar/checkout/v1/..."
}
```

**Frontend redirige al usuario a `initPoint`**

---

### 5.2 Webhook de MercadoPago

**MercadoPago notifica a PAYMENT-SERVICE:**
```
POST /payment-service/webhook/mercadopago
Body: {
  "action": "payment.created",
  "data": {
    "id": "1234567890"
  }
}
```

**PAYMENT-SERVICE debe:**
1. Validar webhook con `x-signature` de MercadoPago
2. Consultar estado del pago: `GET https://api.mercadopago.com/v1/payments/{id}`
3. Si `status == "approved"`:
   - Actualizar Order: `status = PAID`, `paymentId = mercadoPagoId`
   - Llamar a EVENT-SERVICE: `POST /tickets/create-from-order`
   - Llamar a CONSUMPTION-SERVICE: `POST /qr/generate-for-tickets`
4. Si `status == "rejected"`:
   - Liberar reserva de passes (llamar a EVENT-SERVICE)
   - Actualizar Order: `status = CANCELLED`

---

### 5.3 Endpoint EVENT-SERVICE para Crear Tickets
**Endpoint:** `POST /event-service/tickets/create-from-order`

**Body:**
```json
{
  "orderId": 123,
  "userId": 456,
  "items": [
    {
      "eventId": 1,
      "passCode": "PKG-1-1698765432-A1B2C3D4",
      "consumptions": [
        { "consumptionId": 1, "quantity": 2 },
        { "consumptionId": 2, "quantity": 1 }
      ]
    }
  ]
}
```

**Lógica:**
1. Para cada item:
   - Buscar Pass por código
   - Marcar Pass como SOLD (`confirmSale()`)
   - Crear TicketConsumption con sus detalles
   - Crear Ticket vinculando Pass + TicketConsumption
2. Retornar lista de Tickets creados

---

### ✅ Checklist Fase 5
- [ ] Endpoint `POST /payment-service/create-preference` (David)
- [ ] Webhook handler en PAYMENT-SERVICE (David)
- [ ] Endpoint `POST /tickets/create-from-order` en EVENT-SERVICE
- [ ] Cliente HTTP en PAYMENT-SERVICE para llamar a EVENT-SERVICE
- [ ] Cliente HTTP en PAYMENT-SERVICE para llamar a CONSUMPTION-SERVICE
- [ ] Manejo de errores (pago rechazado, webhook fallido)
- [ ] Testing end-to-end: Checkout → Pago → Tickets creados

---

## 📦 FASE 6: INTEGRACIÓN FRONTEND
**Duración:** 1 semana  
**Prioridad:** 🟡 MEDIA  
**Objetivo:** Conectar Angular con flujo completo de compra

### 6.1 Flujo de Checkout en Angular

**Páginas a implementar:**
1. **Carrito:** Ver items, editar cantidades, ir a checkout
2. **Checkout:** Resumen de orden, botón "Pagar con MercadoPago"
3. **Procesando Pago:** Spinner mientras espera webhook
4. **Confirmación:** Mostrar tickets con QR codes

**Servicios Angular:**
```typescript
// cart.service.ts
checkout(): Observable<CheckoutResponse> {
  return this.http.post<CheckoutResponse>(`${API}/cart/checkout`, {});
}

// payment.service.ts
createPreference(orderId: string): Observable<PaymentPreferenceResponse> {
  return this.http.post<PaymentPreferenceResponse>(
    `${API}/payment/create-preference`, 
    { orderId }
  );
}

// Redirigir a MercadoPago
window.location.href = response.initPoint;
```

---

### 6.2 Pantalla "Mis Entradas"

**Componente:** `customer-tickets.component.ts`

**Funcionalidad:**
- Listar todos los tickets del usuario (llamar a EVENT-SERVICE)
- Mostrar QR code de cada ticket
- Indicar si fue canjeado o no
- Mostrar consumiciones incluidas y cuáles ya se canjearon

---

### 6.3 Panel de Empleado

**Componente:** `employee-dashboard.component.ts`

**Funcionalidad:**
- Listar eventos asignados al empleado
- Botón "Escanear QR" (usando librería de escaneo o input manual)
- Validar entrada al evento
- Validar canje de consumiciones

---

### ✅ Checklist Fase 6
- [ ] Página de checkout en Angular
- [ ] Integración con MercadoPago Checkout
- [ ] Página de confirmación de pago
- [ ] Componente "Mis Entradas" con QR codes
- [ ] Panel de empleado para validar QRs
- [ ] Testing end-to-end en frontend

---

## 📦 FASE 7: TESTING Y REFINAMIENTO
**Duración:** 1 semana  
**Prioridad:** 🟢 BAJA  
**Objetivo:** Asegurar calidad y estabilidad

### 7.1 Tests de Integración
- Flujo completo: Registro → Login → Agregar al carrito → Checkout → Pago → Ver tickets
- Concurrencia: 2 usuarios comprando último pass
- Reservas expiradas se liberan correctamente
- Validación multi-tenant (admin no puede modificar eventos de otro)

### 7.2 Optimizaciones
- Índices en base de datos (eventos.createdBy, passes.eventId, etc.)
- Cache de eventos públicos (opcional)
- Paginación en listados

### 7.3 Documentación
- README de cada microservicio con endpoints
- Postman collection con todos los endpoints
- Diagramas de flujo actualizados

---

## 📦 FASE 8: API GATEWAY (OPCIONAL)
**Duración:** 1 semana  
**Prioridad:** 🟢 BAJA  
**Objetivo:** Centralizar requests

**Tecnología:** Nginx o Spring Cloud Gateway

**Configuración Nginx:**
```nginx
upstream auth-service {
    server auth-service:8081;
}

upstream event-service {
    server event-service:8086;
}

server {
    listen 8080;
    
    location /api/auth/ {
        proxy_pass http://auth-service/;
    }
    
    location /api/events/ {
        proxy_pass http://event-service/api/event-service/;
    }
}
```

---

## 📅 CRONOGRAMA ESTIMADO

| Fase | Duración | Prioridad | Inicio Sugerido |
|------|----------|-----------|-----------------|
| Fase 1: Fixes Críticos | 1-2 semanas | 🔴 CRÍTICA | Inmediato |
| Fase 2: ORDER-SERVICE | 1 semana | 🔴 ALTA | Después de Fase 1 |
| Fase 3: Empleados | 1 semana | 🟡 MEDIA | Paralelo con Fase 4 |
| Fase 4: QR Service | 1-2 semanas | 🔴 ALTA | Después de Fase 2 |
| Fase 5: PAYMENT (David) | Variable | 🔴 CRÍTICA | En progreso |
| Fase 6: Frontend | 1 semana | 🟡 MEDIA | Después de Fase 5 |
| Fase 7: Testing | 1 semana | 🟢 BAJA | Al final |
| Fase 8: Gateway | 1 semana | 🟢 BAJA | Opcional |

**TOTAL ESTIMADO:** 6-8 semanas de desarrollo activo

---

## 🎯 HITOS IMPORTANTES

### Hito 1: MVP Funcional (Semana 4)
- ✅ Usuario puede comprar entradas con consumiciones
- ✅ Pago con MercadoPago funcional
- ✅ Tickets con QR generados
- ✅ Multi-tenant seguro

### Hito 2: Sistema Completo (Semana 6)
- ✅ Empleados pueden validar QRs
- ✅ Frontend completamente integrado
- ✅ Email con QRs enviado

### Hito 3: Producción Ready (Semana 8)
- ✅ Tests de integración pasando
- ✅ Documentación completa
- ✅ API Gateway funcionando
- ✅ Sistema optimizado

---

## 🚨 RIESGOS Y MITIGACIONES

### Riesgo 1: Concurrencia en Venta de Passes
**Mitigación:** Optimistic locking con `@Version` + reserva temporal

### Riesgo 2: Webhook de MercadoPago perdido
**Mitigación:** Job scheduled que consulta estado de órdenes PENDING después de 1 hora

### Riesgo 3: Integración con PAYMENT-SERVICE
**Mitigación:** Definir contrato claro de APIs, mock de PAYMENT-SERVICE para testing

### Riesgo 4: Seguridad Multi-Tenant
**Mitigación:** Validación obligatoria de `createdBy` en TODOS los endpoints de admin

---

## 📞 COORDINACIÓN CON DAVID (PAYMENT-SERVICE)

### Necesitas de David:
1. **Endpoint:** `POST /payment/create-preference` (recibe Order, retorna preferenceId)
2. **Webhook:** Validación de firma de MercadoPago
3. **Integración:** Llamar a EVENT-SERVICE al confirmar pago
4. **Testing:** Ambiente sandbox de MercadoPago configurado

### David necesita de ti:
1. **Contrato:** Definir estructura de Order y OrderItem
2. **Endpoint:** `POST /tickets/create-from-order` en EVENT-SERVICE
3. **Documentación:** Cómo llamar a EVENT-SERVICE (URLs, headers, body)
4. **Testing:** Crear órdenes de prueba para testing de pagos

---

## 📝 PRÓXIMOS PASOS INMEDIATOS

### Esta Semana:
1. ✅ Agregar `createdBy` a Consumption
2. ✅ Implementar PassGenerationService
3. ✅ Validar propiedad en EventController y ConsumptionController
4. ✅ Crear sistema de reserva de Passes

### Próxima Semana:
1. Crear entidades Order y OrderItem
2. Implementar CheckoutService
3. Testing de checkout + reserva de passes

### Reunión con David:
- Definir contrato de integración ORDER ↔ PAYMENT
- Acordar formato de webhooks y callbacks
- Planificar testing integrado

---

## 🎓 CONSIDERACIONES ACADÉMICAS

Este plan está diseñado para:
- ✅ Demostrar dominio de arquitectura de microservicios
- ✅ Implementar patrones de diseño (Repository, Service, DTO)
- ✅ Manejar concurrencia y transacciones distribuidas
- ✅ Seguridad multi-tenant
- ✅ Integración con APIs externas (MercadoPago)
- ✅ Testing de integración

**Recomendación para la presentación:**
- Documentar decisiones arquitectónicas (por qué microservicios, por qué reserva temporal, etc.)
- Diagramas de flujo de los procesos principales
- Métricas de performance (tiempo de respuesta, throughput)
- Demo en vivo del flujo completo

---

## 📚 RECURSOS ÚTILES

- [MercadoPago Developers](https://www.mercadopago.com.ar/developers/es/docs)
- [ZXing QR Code Library](https://github.com/zxing/zxing)
- [Spring WebClient](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client)
- [Optimistic Locking in JPA](https://www.baeldung.com/jpa-optimistic-locking)

---

**¡Éxito con el desarrollo! 🚀**

_Última actualización: Octubre 2025_
