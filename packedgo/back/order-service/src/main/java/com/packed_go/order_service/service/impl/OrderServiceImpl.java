package com.packed_go.order_service.service.impl;

import com.packed_go.order_service.client.PaymentServiceClient;
import com.packed_go.order_service.dto.MultiOrderCheckoutResponse;
import com.packed_go.order_service.dto.OrderItemDTO;
import com.packed_go.order_service.dto.PaymentGroupDTO;
import com.packed_go.order_service.dto.external.PaymentServiceRequest;
import com.packed_go.order_service.dto.external.PaymentServiceResponse;
import com.packed_go.order_service.dto.request.CheckoutRequest;
import com.packed_go.order_service.dto.request.PaymentCallbackRequest;
import com.packed_go.order_service.dto.response.CheckoutResponse;
import com.packed_go.order_service.entity.CartItem;
import com.packed_go.order_service.entity.MultiOrderSession;
import com.packed_go.order_service.entity.Order;
import com.packed_go.order_service.entity.OrderItem;
import com.packed_go.order_service.entity.ShoppingCart;
import com.packed_go.order_service.exception.CartExpiredException;
import com.packed_go.order_service.exception.CartNotFoundException;
import com.packed_go.order_service.repository.MultiOrderSessionRepository;
import com.packed_go.order_service.repository.OrderRepository;
import com.packed_go.order_service.repository.ShoppingCartRepository;
import com.packed_go.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final ShoppingCartRepository cartRepository;
    private final MultiOrderSessionRepository sessionRepository;
    private final PaymentServiceClient paymentServiceClient;
    
    @Override
    @Transactional
    public CheckoutResponse checkout(Long userId, CheckoutRequest request) {
        log.info("Processing checkout for user: {}", userId);
        
        // 1. Obtener carrito activo del usuario
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new CartNotFoundException("No active cart found for user"));
        
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
        
        // 1. Obtener carrito activo del usuario
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new CartNotFoundException("No active cart found for user"));
        
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
                .build();
        
        session = sessionRepository.save(session);
        
        // 6. Crear una orden por cada admin
        List<Order> orders = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : itemsByAdmin.entrySet()) {
            Long adminId = entry.getKey();
            List<CartItem> adminItems = entry.getValue();
            
            // Calcular total para este admin
            BigDecimal adminTotal = adminItems.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
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
        
        // 8. Marcar carrito como CHECKED_OUT
        cart.markAsCheckedOut();
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
                            .map(item -> OrderItemDTO.builder()
                                    .eventId(item.getEventId())
                                    .eventName(item.getEventName())
                                    .quantity(item.getQuantity())
                                    .price(item.getUnitPrice())
                                    .totalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                                    .build())
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
}
