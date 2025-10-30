package com.packed_go.order_service.controller;

import com.packed_go.order_service.dto.MultiOrderCheckoutResponse;
import com.packed_go.order_service.dto.request.CheckoutRequest;
import com.packed_go.order_service.dto.request.PaymentCallbackRequest;
import com.packed_go.order_service.dto.response.CheckoutResponse;
import com.packed_go.order_service.entity.Order;
import com.packed_go.order_service.security.JwtTokenValidator;
import com.packed_go.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class OrderController {
    
    private final OrderService orderService;
    private final JwtTokenValidator jwtTokenValidator;
    
    /**
     * Procesar checkout del carrito
     * 
     * POST /api/orders/checkout
     * Headers: Authorization: Bearer {token}
     * Body: { "adminId": 1, "successUrl": "...", "failureUrl": "...", "pendingUrl": "..." }
     * 
     * @return 201 CREATED con la URL de pago de MercadoPago
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody CheckoutRequest request) {
        
        log.info("POST /api/orders/checkout - Processing checkout for adminId: {}", request.getAdminId());
        
        Long userId = extractUserId(authHeader);
        CheckoutResponse response = orderService.checkout(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Procesar checkout multitenant - crea múltiples órdenes si hay items de varios admins
     * 
     * POST /api/orders/checkout/multi
     * Headers: Authorization: Bearer {token}
     * 
     * @return 201 CREATED con la sesión y grupos de pago
     */
    @PostMapping("/checkout/multi")
    public ResponseEntity<MultiOrderCheckoutResponse> checkoutMulti(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("POST /api/orders/checkout/multi - Processing multi-order checkout");
        
        Long userId = extractUserId(authHeader);
        MultiOrderCheckoutResponse response = orderService.checkoutMulti(userId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Obtener el estado de una sesión de múltiples órdenes
     * 
     * GET /api/orders/sessions/{sessionId}
     * Headers: Authorization: Bearer {token}
     * 
     * @return 200 OK con el estado de la sesión
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<MultiOrderCheckoutResponse> getSessionStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String sessionId) {
        
        log.info("GET /api/orders/sessions/{} - Retrieving session status", sessionId);
        
        Long userId = extractUserId(authHeader);
        MultiOrderCheckoutResponse response = orderService.getSessionStatus(sessionId);
        
        // Validar que la sesión pertenece al usuario
        if (!response.getPaymentGroups().isEmpty() && 
            !response.getPaymentGroups().get(0).getOrderId().toString().contains(userId.toString())) {
            // Esta validación es básica, en producción valdría la pena tener userId en MultiOrderSession
            log.warn("User {} attempted to access session {}", userId, sessionId);
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Callback para actualizar orden después del pago
     * Este endpoint es llamado por payment-service después del webhook
     * 
     * POST /api/orders/payment-callback
     * Body: { "orderNumber": "ORD-202510-123", "paymentStatus": "APPROVED" }
     * 
     * @return 200 OK
     */
    @PostMapping("/payment-callback")
    public ResponseEntity<Void> paymentCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        
        log.info("POST /api/orders/payment-callback - Updating order: {} with status: {}", 
                request.getOrderNumber(), request.getPaymentStatus());
        
        orderService.updateOrderFromPaymentCallback(request);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Obtener todas las órdenes del usuario
     * 
     * GET /api/orders
     * Headers: Authorization: Bearer {token}
     * 
     * @return 200 OK con lista de órdenes
     */
    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(
            @RequestHeader("Authorization") String authHeader) {
        
        log.info("GET /api/orders - Retrieving user orders");
        
        Long userId = extractUserId(authHeader);
        List<Order> orders = orderService.getUserOrders(userId);
        
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Obtener una orden específica por número
     * 
     * GET /api/orders/{orderNumber}
     * Headers: Authorization: Bearer {token}
     * 
     * @return 200 OK con la orden
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<Order> getOrderByNumber(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String orderNumber) {
        
        log.info("GET /api/orders/{} - Retrieving order", orderNumber);
        
        Long userId = extractUserId(authHeader);
        Order order = orderService.getOrderByNumber(orderNumber);
        
        // Validar que la orden pertenece al usuario
        if (!order.getUserId().equals(userId)) {
            log.warn("User {} attempted to access order {} belonging to user {}", 
                    userId, orderNumber, order.getUserId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(order);
    }
    
    /**
     * Extrae el userId del token JWT
     */
    private Long extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        
        if (!jwtTokenValidator.validateToken(token)) {
            throw new RuntimeException("Invalid token");
        }
        
        return jwtTokenValidator.getUserIdFromToken(token);
    }
}
