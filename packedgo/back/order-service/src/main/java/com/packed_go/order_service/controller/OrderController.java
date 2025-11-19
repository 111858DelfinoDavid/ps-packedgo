package com.packed_go.order_service.controller;

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
    
    /**
     * Simple response class for messages
     */
    private record MessageResponse(String message) {}

    /**
     * Procesar checkout para un admin específico (Single Admin Checkout)
     * 
     * POST /api/orders/checkout/single
     * Headers: Authorization: Bearer {token}
     * Body: { "adminId": 123 }
     * 
     * @return 201 CREATED con la URL de pago
     */
    @PostMapping("/checkout/single")
    public ResponseEntity<CheckoutResponse> checkoutSingleAdmin(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CheckoutRequest request) {
        
        log.info("POST /api/orders/checkout/single - Processing checkout for adminId: {}", request.getAdminId());
        
        Long userId = extractUserId(authHeader);
        CheckoutResponse response = orderService.checkoutSingleAdmin(userId, request.getAdminId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    // DEPRECATED: Este endpoint ha sido reemplazado por /payment-callback
    // que recibe notificaciones del payment-service
    /*
    @PutMapping("/{orderNumber}/status/paid")
    public ResponseEntity<String> markOrderAsPaid(
            @PathVariable String orderNumber,
            @RequestHeader("X-Internal-Call") String internalCallHeader) {
        
        log.info("🔄 Marcando orden {} como PAID (llamada interna)", orderNumber);
        
        try {
            orderService.updateOrderStatus(orderNumber, "PAID");
            return ResponseEntity.ok("Orden marcada como PAID");
        } catch (Exception e) {
            log.error("Error marcando orden como PAID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
    */
}
