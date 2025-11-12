package com.packed_go.order_service.service.impl;

import com.packed_go.order_service.client.PaymentServiceClient;
import com.packed_go.order_service.dto.ConsumptionDTO;
import com.packed_go.order_service.dto.MultiOrderCheckoutResponse;
import com.packed_go.order_service.dto.OrderItemDTO;
import com.packed_go.order_service.dto.PaymentGroupDTO;
import com.packed_go.order_service.dto.SessionStateResponse;
import com.packed_go.order_service.dto.external.CreateTicketWithConsumptionsRequest;
import com.packed_go.order_service.dto.external.PaymentServiceRequest;
import com.packed_go.order_service.dto.external.PaymentServiceResponse;
import com.packed_go.order_service.dto.external.TicketConsumptionDTO;
import com.packed_go.order_service.dto.external.TicketWithConsumptionsResponse;
import com.packed_go.order_service.dto.request.CheckoutRequest;
import com.packed_go.order_service.dto.request.PaymentCallbackRequest;
import com.packed_go.order_service.dto.response.CheckoutResponse;
import com.packed_go.order_service.entity.CartItem;
import com.packed_go.order_service.entity.CartItemConsumption;
import com.packed_go.order_service.entity.MultiOrderSession;
import com.packed_go.order_service.entity.Order;
import com.packed_go.order_service.entity.OrderItem;
import com.packed_go.order_service.entity.ShoppingCart;
import com.packed_go.order_service.exception.CartExpiredException;
import com.packed_go.order_service.exception.CartNotFoundException;
import com.packed_go.order_service.external.EventServiceClient;
import com.packed_go.order_service.repository.MultiOrderSessionRepository;
import com.packed_go.order_service.repository.OrderRepository;
import com.packed_go.order_service.repository.ShoppingCartRepository;
import com.packed_go.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final ShoppingCartRepository cartRepository;
    private final MultiOrderSessionRepository sessionRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final EventServiceClient eventServiceClient;
    
    @Override
    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        log.info("Processing checkout for user: {}", userId);
        
        // 1. Obtener carrito activo del usuario (con items eager loaded)
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException("No active cart found for user");
        }
        ShoppingCart cart = activeCarts.get(0); // Tomar el más reciente (por updatedAt DESC)
        
        // 2. Validar que el carrito no esté expirado
        if (cart.isExpired()) {
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException("Cart has expired");
        }
        
        // 3. Validar que el carrito tenga items
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        
        // 4. Crear la orden desde el carrito
        Order order = createOrderFromCart(cart, request.getAdminId());
        order = orderRepository.save(order);
        
        log.info("Order created: {} with total: {}", order.getOrderNumber(), order.getTotalAmount());
        
        // 5. Crear el pago en payment-service
        PaymentServiceRequest paymentRequest = PaymentServiceRequest.builder()
                .adminId(request.getAdminId())
                .orderId(order.getOrderNumber())
                .amount(order.getTotalAmount())
                .description("Orden " + order.getOrderNumber() + " - PackedGo Events")
                .successUrl(request.getSuccessUrl() != null ? request.getSuccessUrl() : "http://localhost:3000/payment/success")
                .failureUrl(request.getFailureUrl() != null ? request.getFailureUrl() : "http://localhost:3000/payment/failure")
                .pendingUrl(request.getPendingUrl() != null ? request.getPendingUrl() : "http://localhost:3000/payment/pending")
                .build();
        
        PaymentServiceResponse paymentResponse = paymentServiceClient.createPayment(paymentRequest);
        
        // 6. Actualizar orden con datos del pago
        order.setPaymentId(paymentResponse.getPaymentId());
        order.setPaymentPreferenceId(paymentResponse.getPreferenceId());
        orderRepository.save(order);
        
        // 7. Marcar carrito como CHECKED_OUT
        cart.markAsCheckedOut();
        cartRepository.save(cart);
        
        log.info("Checkout completed successfully. Redirecting to: {}", paymentResponse.getInitPoint());
        
        // 8. Retornar respuesta con URL de pago
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .paymentUrl(paymentResponse.getSandboxInitPoint() != null ? 
                        paymentResponse.getSandboxInitPoint() : paymentResponse.getInitPoint())
                .preferenceId(paymentResponse.getPreferenceId())
                .message("Checkout successful. Redirect to payment gateway.")
                .build();
    }
    
    @Override
    @Transactional
    public void updateOrderFromPaymentCallback(PaymentCallbackRequest request) {
        log.info("Updating order {} with payment status: {}", request.getOrderNumber(), request.getPaymentStatus());
        
        Order order = orderRepository.findByOrderNumber(request.getOrderNumber())
                .orElseThrow(() -> new RuntimeException("Order not found: " + request.getOrderNumber()));
        
        // Actualizar estado según el resultado del pago
        switch (request.getPaymentStatus().toUpperCase()) {
            case "APPROVED":
                order.markAsPaid();
                log.info("Order {} marked as PAID", order.getOrderNumber());
                
                // 🎟️ GENERAR TICKETS cuando el pago es aprobado
                try {
                    generateTicketsForOrder(order);
                } catch (Exception e) {
                    log.error("Failed to generate tickets for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
                    // No lanzamos excepción para no revertir la transacción de la orden
                    // Los tickets se pueden generar manualmente después si es necesario
                }
                break;
            case "REJECTED":
            case "CANCELLED":
                order.markAsCancelled();
                log.info("Order {} marked as CANCELLED", order.getOrderNumber());
                break;
            case "IN_PROCESS":
            case "PENDING":
                // Mantener como PENDING_PAYMENT
                log.info("Order {} still PENDING_PAYMENT", order.getOrderNumber());
                break;
            default:
                log.warn("Unknown payment status: {} for order {}", request.getPaymentStatus(), order.getOrderNumber());
        }
        
        orderRepository.save(order);
        
        // Si esta orden pertenece a una sesión múltiple, actualizar el estado de la sesión
        if (order.getMultiOrderSession() != null) {
            MultiOrderSession session = order.getMultiOrderSession();
            session.updateSessionStatus();
            sessionRepository.save(session);
            log.info("Updated session {} status to: {}", session.getSessionId(), session.getSessionStatus());
            
            // Si la sesión está COMPLETA, marcar el carrito como CHECKED_OUT definitivamente
            if ("COMPLETED".equals(session.getSessionStatus())) {
                ShoppingCart cart = cartRepository.findById(session.getCartId()).orElse(null);
                if (cart != null && "IN_CHECKOUT".equals(cart.getStatus())) {
                    cart.markAsCheckedOut();
                    cartRepository.save(cart);
                    log.info("✅ All payments completed. Cart {} marked as CHECKED_OUT", cart.getId());
                }
            }
        }
    }
    
    @Override
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Override
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));
    }
    
    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
    }
    
    @Override
    @Transactional
    public MultiOrderCheckoutResponse checkoutMulti(Long userId) {
        log.info("Processing multi-order checkout for user: {}", userId);
        
        // 1. Obtener carrito activo del usuario (con items eager loaded)
        List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
        if (activeCarts.isEmpty()) {
            throw new CartNotFoundException("No active cart found for user");
        }
        ShoppingCart cart = activeCarts.get(0); // Tomar el más reciente (por updatedAt DESC)
        
        // 2. Validar que el carrito no esté expirado
        if (cart.isExpired()) {
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException("Cart has expired");
        }
        
        // 3. Validar que el carrito tenga items
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }
        
        // 4. Agrupar items por adminId
        Map<Long, List<CartItem>> itemsByAdmin = cart.getItems().stream()
                .collect(Collectors.groupingBy(CartItem::getAdminId));
        
        log.info("Cart contains items from {} different admins", itemsByAdmin.size());
        
        // 5. Crear MultiOrderSession
        MultiOrderSession session = MultiOrderSession.builder()
                .userId(userId)
                .cartId(cart.getId())
                .totalAmount(cart.getTotalAmount())
                .sessionStatus("PENDING")
                .sessionToken(UUID.randomUUID().toString()) // Generar token explícitamente
                .build();
        
        session = sessionRepository.save(session);
        
        // 6. Crear una orden por cada admin
        List<Order> orders = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : itemsByAdmin.entrySet()) {
            Long adminId = entry.getKey();
            List<CartItem> adminItems = entry.getValue();
            
            // Calcular total para este admin (incluyendo consumiciones)
            BigDecimal adminTotal = adminItems.stream()
                    .map(CartItem::getSubtotal) // Usa subtotal que incluye consumiciones
                    .filter(subtotal -> subtotal != null) // Validación por seguridad
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Crear orden
            Order order = Order.builder()
                    .userId(userId)
                    .cartId(cart.getId())
                    .adminId(adminId)
                    .totalAmount(adminTotal)
                    .status(Order.OrderStatus.PENDING_PAYMENT)
                    .multiOrderSession(session)
                    .build();
            
            // Agregar items
            for (CartItem cartItem : adminItems) {
                OrderItem orderItem = OrderItem.fromCartItem(cartItem);
                order.addItem(orderItem);
            }
            
            order = orderRepository.save(order);
            orders.add(order);
            session.addOrder(order);
            
            log.info("Created order {} for admin {} with {} items, total: {}", 
                    order.getOrderNumber(), adminId, adminItems.size(), adminTotal);
        }
        
        // 7. Actualizar sesión con órdenes
        sessionRepository.save(session);
        
        // 8. Marcar carrito como IN_CHECKOUT (no CHECKED_OUT para permitir recuperación)
        cart.markAsInCheckout();
        cartRepository.save(cart);
        
        // 9. Construir respuesta
        return buildMultiOrderResponse(session, orders);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MultiOrderCheckoutResponse getSessionStatus(String sessionId) {
        log.info("Getting status for session: {}", sessionId);
        
        MultiOrderSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        // Actualizar estado de la sesión basado en el estado de las órdenes
        session.updateSessionStatus();
        
        List<Order> orders = session.getOrders();
        return buildMultiOrderResponse(session, orders);
    }
    
    @Override
    @Transactional(readOnly = true)
    public MultiOrderCheckoutResponse recoverSessionByToken(String sessionToken) {
        log.info("Recovering session by token");
        
        MultiOrderSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Session not found for provided token"));
        
        // Verificar que la sesión no ha expirado
        if (session.isExpired()) {
            log.warn("Session {} has expired", session.getSessionId());
            throw new RuntimeException("Session has expired");
        }
        
        // Actualizar estado de la sesión
        session.updateSessionStatus();
        
        List<Order> orders = session.getOrders();
        return buildMultiOrderResponse(session, orders);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Object> getSessionTickets(String sessionId) {
        log.info("Getting tickets for session: {}", sessionId);
        
        MultiOrderSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        // Obtener todas las órdenes de la sesión que estén pagadas
        List<Order> paidOrders = session.getOrders().stream()
                .filter(Order::isPaid)
                .collect(Collectors.toList());
        
        if (paidOrders.isEmpty()) {
            log.warn("No paid orders found for session: {}", sessionId);
            return List.of();
        }
        
        // Obtener tickets de cada orden pagada
        // Nota: Asumimos que los tickets están en event-service
        // Por ahora devolvemos los order IDs para que el frontend pueda buscar tickets
        return paidOrders.stream()
                .map(order -> Map.of(
                    "orderId", order.getId(),
                    "orderNumber", order.getOrderNumber(),
                    "eventId", order.getItems().get(0).getEventId(),
                    "totalAmount", order.getTotalAmount()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * Backend State Authority: El método más importante de toda la aplicación
     * Busca o crea la sesión actual del usuario SIN que el frontend guarde nada
     * NUNCA falla, siempre retorna una sesión válida
     */
    @Override
    @Transactional
    public SessionStateResponse getCurrentCheckoutState(Long userId) {
        log.info("Getting current checkout state for user: {}", userId);
        
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Buscar sesión activa (PENDING/PARTIAL no expirada)
        Optional<MultiOrderSession> activeSession = sessionRepository.findActiveSessionByUserId(userId, now);
        
        MultiOrderSession session;
        boolean wasCreated = false;
        
        if (activeSession.isPresent()) {
            session = activeSession.get();
            log.info("Found active session: {} for user: {}", session.getSessionId(), userId);
            
            // Actualizar tracking
            session.touch();
            sessionRepository.save(session);
            
        } else {
            // 2. No hay sesión activa, crear nueva desde cart
            log.info("No active session found, creating new session from cart for user: {}", userId);
            
            // Buscar carrito activo
            List<ShoppingCart> activeCarts = cartRepository.findByUserIdAndStatusWithItems(userId, "ACTIVE");
            if (activeCarts.isEmpty()) {
                // Carrito vacío, retornar sesión vacía indicando que debe agregar items
                return SessionStateResponse.builder()
                        .sessionStatus("NO_CART")
                        .isActive(false)
                        .isExpired(false)
                        .isCompleted(false)
                        .totalGroups(0)
                        .paidGroups(0)
                        .pendingGroups(0)
                        .paymentGroups(List.of())
                        .build();
            }
            
            // Crear sesión desde cart (reusar lógica de checkoutMulti)
            MultiOrderCheckoutResponse checkoutResponse = checkoutMulti(userId);
            
            // Buscar la sesión recién creada
            session = sessionRepository.findBySessionId(checkoutResponse.getSessionId())
                    .orElseThrow(() -> new RuntimeException("Failed to create session"));
            
            wasCreated = true;
            log.info("Created new session: {} for user: {}", session.getSessionId(), userId);
        }
        
        // 3. Construir response con estado completo
        return buildSessionStateResponse(session, wasCreated);
    }
    
    /**
     * Construye el response con TODO el estado de la sesión
     */
    private SessionStateResponse buildSessionStateResponse(MultiOrderSession session, boolean wasJustCreated) {
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = session.isExpired();
        long secondsUntilExpiration = java.time.Duration.between(now, session.getExpiresAt()).getSeconds();
        
        // Obtener órdenes
        List<Order> orders = session.getOrders();
        
        // Agrupar por admin y construir payment groups
        List<SessionStateResponse.PaymentGroupInfo> paymentGroups = orders.stream()
                .map(order -> {
                    List<SessionStateResponse.OrderItemInfo> items = order.getItems().stream()
                            .map(item -> SessionStateResponse.OrderItemInfo.builder()
                                    .eventId(item.getEventId())
                                    .eventName(item.getEventName())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                    .build())
                            .collect(Collectors.toList());
                    
                    return SessionStateResponse.PaymentGroupInfo.builder()
                            .adminId(order.getAdminId())
                            .adminName("Admin " + order.getAdminId()) // TODO: Obtener nombre real
                            .orderId(order.getId().toString())
                            .orderNumber(order.getOrderNumber())
                            .amount(order.getTotalAmount())
                            .paymentStatus(order.getStatus().toString())
                            .initPoint(null) // TODO: Agregar campo paymentInitPoint a Order entity
                            .items(items)
                            .build();
                })
                .toList();
        
        // Stats
        int totalGroups = paymentGroups.size();
        int paidGroups = (int) paymentGroups.stream()
                .filter(g -> "PAID".equals(g.getPaymentStatus()) || "COMPLETED".equals(g.getPaymentStatus()))
                .count();
        int pendingGroups = totalGroups - paidGroups;
        
        return SessionStateResponse.builder()
                .sessionId(session.getSessionId())
                .sessionStatus(session.getSessionStatus())
                .totalAmount(session.getTotalAmount())
                .expiresAt(session.getExpiresAt())
                .lastAccessedAt(session.getLastAccessedAt())
                .attemptCount(session.getAttemptCount())
                .isExpired(isExpired)
                .isActive(!isExpired && !"COMPLETED".equals(session.getSessionStatus()) && !"CANCELLED".equals(session.getSessionStatus()))
                .isCompleted("COMPLETED".equals(session.getSessionStatus()))
                .secondsUntilExpiration(Math.max(0, secondsUntilExpiration))
                .paymentGroups(paymentGroups)
                .totalGroups(totalGroups)
                .paidGroups(paidGroups)
                .pendingGroups(pendingGroups)
                .build();
    }
    
    @Override
    @Transactional
    public void abandonSession(String sessionId, Long userId) {
        log.info("Abandoning session: {} for user: {}", sessionId, userId);
        
        // 1. Obtener la sesión
        MultiOrderSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        
        // 2. Verificar que la sesión pertenece al usuario
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("Session does not belong to user");
        }
        
        // 3. Verificar que no hay pagos completados
        List<Order> orders = session.getOrders();
        boolean hasAnyPaidOrder = orders.stream()
                .anyMatch(order -> order.getStatus() == Order.OrderStatus.PAID);
        
        if (hasAnyPaidOrder) {
            throw new IllegalStateException("Cannot abandon session with paid orders. Please complete the checkout or navigate away.");
        }
        
        // 4. Obtener el carrito asociado
        ShoppingCart cart = cartRepository.findById(session.getCartId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        // 5. Verificar que el carrito está en IN_CHECKOUT
        if (!"IN_CHECKOUT".equals(cart.getStatus())) {
            log.warn("Cart {} is not in IN_CHECKOUT status (current: {})", cart.getId(), cart.getStatus());
        }
        
        // 6. Reactivar el carrito (volver a ACTIVE y extender expiración)
        cart.reactivate();
        cartRepository.save(cart);
        
        // 7. Marcar la sesión como cancelada
        session.setSessionStatus("CANCELLED");
        sessionRepository.save(session);
        
        // 8. Cancelar todas las órdenes pendientes
        orders.forEach(order -> {
            if (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);
            }
        });
        
        log.info("Session {} abandoned successfully. Cart {} reactivated.", sessionId, cart.getId());
    }
    
    // ============================================
    // Helper Methods
    // ============================================
    
    /**
     * Construye la respuesta del checkout múltiple
     */
    private MultiOrderCheckoutResponse buildMultiOrderResponse(MultiOrderSession session, List<Order> orders) {
        List<PaymentGroupDTO> paymentGroups = orders.stream()
                .map(order -> {
                    List<OrderItemDTO> itemDTOs = order.getItems().stream()
                            .map(item -> {
                                // Mapear consumiciones
                                List<ConsumptionDTO> consumptionDTOs = item.getConsumptions().stream()
                                        .map(cons -> ConsumptionDTO.builder()
                                                .consumptionId(cons.getConsumptionId())
                                                .name(cons.getConsumptionName())
                                                .quantity(cons.getQuantity())
                                                .price(cons.getUnitPrice())
                                                .subtotal(cons.getSubtotal())
                                                .build())
                                        .toList();
                                
                                return OrderItemDTO.builder()
                                        .eventId(item.getEventId())
                                        .eventName(item.getEventName())
                                        .quantity(item.getQuantity())
                                        .price(item.getUnitPrice())
                                        .totalPrice(item.getSubtotal()) // Usar subtotal que incluye consumiciones
                                        .consumptions(consumptionDTOs)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    
                    return PaymentGroupDTO.builder()
                            .adminId(order.getAdminId())
                            .orderNumber(order.getOrderNumber())
                            .orderId(order.getId())
                            .amount(order.getTotalAmount())
                            .status(order.getStatus().name())
                            .paymentPreferenceId(order.getPaymentPreferenceId())
                            .items(itemDTOs)
                            .build();
                })
                .collect(Collectors.toList());
        
        return MultiOrderCheckoutResponse.builder()
                .sessionId(session.getSessionId())
                .sessionToken(session.getSessionToken())
                .totalAmount(session.getTotalAmount())
                .sessionStatus(session.getSessionStatus())
                .expiresAt(session.getExpiresAt())
                .totalOrders(orders.size())
                .paidOrders((int) session.getPaidOrdersCount())
                .totalPaid(session.getTotalPaid())
                .totalPending(session.getTotalPending())
                .paymentGroups(paymentGroups)
                .message(session.isExpired() ? "Session has expired" : 
                        "Create payment for each group to complete checkout")
                .build();
    }
    
    // ============================================
    // Helper Methods
    // ============================================
    
    /**
     * Crea una orden desde un carrito
     */
    private Order createOrderFromCart(ShoppingCart cart, Long adminId) {
        Order order = Order.builder()
                .userId(cart.getUserId())
                .cartId(cart.getId())
                .totalAmount(cart.getTotalAmount())
                .adminId(adminId)
                .status(Order.OrderStatus.PENDING_PAYMENT)
                .build();
        
        // Copiar items del carrito a la orden
        cart.getItems().forEach(cartItem -> {
            OrderItem orderItem = OrderItem.fromCartItem(cartItem);
            order.addItem(orderItem);
        });
        
        return order;
    }

    /**
     * Genera tickets en event-service para cada entrada de la orden
     * Se ejecuta automáticamente cuando una orden es marcada como PAID
     */
    private void generateTicketsForOrder(Order order) {
        log.info("🎟️ Generating tickets for order: {}", order.getOrderNumber());
        
        int ticketsGenerated = 0;
        int ticketsFailed = 0;
        
        // Por cada OrderItem (que representa entradas de un evento)
        for (OrderItem orderItem : order.getItems()) {
            Long eventId = orderItem.getEventId();
            Integer quantity = orderItem.getQuantity();
            
            log.info("Generating {} ticket(s) for event: {}", quantity, eventId);
            
            // Generar un ticket por cada entrada
            for (int i = 0; i < quantity; i++) {
                try {
                    // Preparar las consumiciones si existen
                    List<TicketConsumptionDTO> consumptions = new ArrayList<>();
                    if (orderItem.getConsumptions() != null && !orderItem.getConsumptions().isEmpty()) {
                        consumptions = orderItem.getConsumptions().stream()
                                .map(cons -> TicketConsumptionDTO.builder()
                                        .consumptionId(cons.getConsumptionId())
                                        .consumptionName(cons.getConsumptionName())
                                        .priceAtPurchase(cons.getUnitPrice())
                                        .quantity(cons.getQuantity())
                                        .build())
                                .collect(Collectors.toList());
                    }
                    
                    // Crear ticket con consumiciones
                    CreateTicketWithConsumptionsRequest ticketRequest = CreateTicketWithConsumptionsRequest.builder()
                            .userId(order.getUserId())
                            .eventId(eventId)
                            .consumptions(consumptions)
                            .build();
                    
                    TicketWithConsumptionsResponse response = eventServiceClient.createTicketWithConsumptions(ticketRequest);
                    
                    if (response.getSuccess()) {
                        ticketsGenerated++;
                        log.info("✅ Ticket #{} generated: ID={}, QR={}", 
                                (i + 1), response.getTicketId(), response.getQrCode());
                    } else {
                        ticketsFailed++;
                        log.error("❌ Failed to generate ticket #{}: {}", (i + 1), response.getMessage());
                    }
                    
                } catch (Exception e) {
                    ticketsFailed++;
                    log.error("❌ Error generating ticket #{} for event {}: {}", 
                            (i + 1), eventId, e.getMessage(), e);
                }
            }
        }
        
        log.info("🎟️ Ticket generation completed for order {}: {} successful, {} failed",
                order.getOrderNumber(), ticketsGenerated, ticketsFailed);
        
        if (ticketsFailed > 0) {
            log.warn("⚠️ Some tickets failed to generate. Manual intervention may be required.");
        }
    }
}

