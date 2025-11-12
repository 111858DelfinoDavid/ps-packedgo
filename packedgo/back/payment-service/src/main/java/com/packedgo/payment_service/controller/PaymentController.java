package com.packedgo.payment_service.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packedgo.payment_service.dto.PaymentRequest;
import com.packedgo.payment_service.dto.PaymentResponse;
import com.packedgo.payment_service.dto.WebhookNotification;
import com.packedgo.payment_service.exception.ResourceNotFoundException;
import com.packedgo.payment_service.model.Payment;
import com.packedgo.payment_service.security.JwtTokenValidator;
import com.packedgo.payment_service.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final JwtTokenValidator jwtTokenValidator;

    /**
     * Endpoint para crear una preferencia de pago
     * SEGURIDAD: Requiere autenticación JWT
     * Valida que el usuario esté autenticado antes de crear preferencias de pago
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // SECURITY: Validar JWT token
            String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
            if (!jwtTokenValidator.validateToken(token)) {
                log.warn("Token JWT inválido en /payments/create");
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(PaymentResponse.builder()
                                .message("Token JWT inválido o expirado")
                                .build());
            }

            Long userIdFromToken = jwtTokenValidator.getUserIdFromToken(token);

            log.info("POST /api/payments/create - UserId: {}, AdminId: {}, OrderId: {}",
                    userIdFromToken, request.getAdminId(), request.getOrderId());

            // Validar que el adminId esté presente
            if (request.getAdminId() == null) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(PaymentResponse.builder()
                                .message("AdminId es requerido")
                                .build());
            }

            // Usuario autenticado puede crear pagos para cualquier admin
            // (normal en checkout cuando customer compra eventos de diferentes admins)

            PaymentResponse response = paymentService.createPaymentPreference(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(PaymentResponse.builder()
                            .message("Error de autenticación: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Error creando pago", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.builder()
                            .message("Error al crear el pago: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Webhook para recibir notificaciones de MercadoPago
     * MercadoPago envía notificaciones cuando cambia el estado de un pago
     * SEGURIDAD: El adminId se obtiene del payment lookup, no del query param
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody WebhookNotification notification) {

        log.info("POST /api/payments/webhook - Type: {}, Data: {}",
                notification.getType(), notification.getData());

        try {
            // Solo procesar notificaciones de pagos
            if ("payment".equals(notification.getType())) {
                Long paymentId = Long.valueOf(notification.getData().getId());

                // SECURITY FIX: processWebhookNotification ahora busca el payment
                // y obtiene el adminId de ahí, no del query param
                paymentService.processWebhookNotification(null, paymentId);

                return ResponseEntity.ok(Map.of("status", "processed"));
            }

            return ResponseEntity.ok(Map.of("status", "ignored"));

        } catch (NumberFormatException e) {
            log.error("ID de pago inválido: {}", notification.getData().getId());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "ID de pago inválido"));
        } catch (Exception e) {
            log.error("Error procesando webhook", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint para consultar el estado de un pago por preferenceId
     * POLLING: Este endpoint es usado por el frontend para hacer polling del estado
     * Autenticación OPCIONAL para permitir polling desde browser
     */
    @GetMapping("/status/{preferenceId}")
    public ResponseEntity<?> getPaymentByPreferenceId(
            @PathVariable String preferenceId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            // Si hay authHeader, validar JWT (opcional)
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
                    if (jwtTokenValidator.validateToken(token)) {
                        Long userId = jwtTokenValidator.getUserIdFromToken(token);
                        log.info("GET /api/payments/status/{} - User authenticated: {}", preferenceId, userId);
                    }
                } catch (Exception e) {
                    log.warn("Invalid or expired token in polling request, proceeding anyway");
                }
            } else {
                log.info("GET /api/payments/status/{} - Unauthenticated polling request", preferenceId);
            }

            Payment payment = paymentService.getPaymentByPreferenceId(preferenceId);
            
            PaymentResponse response = PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .status(payment.getStatus().name())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .preferenceId(payment.getPreferenceId())
                    .message("Payment found")
                    .build();
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting payment status for preference: {}", preferenceId, e);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pago no encontrado para la preferencia: " + preferenceId));
        }
    }

    /**
     * Endpoint para simular la aprobación de un pago (SOLO PARA TESTING)
     * Simula que MercadoPago aprobó el pago y dispara todo el flujo de generación de tickets
     * IMPORTANTE: Este endpoint NO debe estar habilitado en producción
     */
    @PostMapping("/simulate-approval/{preferenceId}")
    public ResponseEntity<?> simulatePaymentApproval(
            @PathVariable String preferenceId) {

        try {
            log.info("POST /api/payments/simulate-approval/{} - Simulando aprobación de pago", preferenceId);

            // Buscar el pago por preferenceId
            Payment payment = paymentService.getPaymentByPreferenceId(preferenceId);
            
            if (payment.getStatus() == Payment.PaymentStatus.APPROVED) {
                return ResponseEntity.ok(Map.of(
                    "status", "already_approved",
                    "message", "El pago ya estaba aprobado",
                    "orderId", payment.getOrderId()
                ));
            }

            // Simular el webhook de MercadoPago aprobando el pago
            // Usamos un mpPaymentId falso para testing
            Long fakeMpPaymentId = System.currentTimeMillis(); // ID único basado en timestamp
            
            log.info("Simulando aprobación de pago {} con mpPaymentId simulado: {}", 
                    payment.getId(), fakeMpPaymentId);
            
            // Llamar al método de simulación (NO consulta MercadoPago)
            paymentService.simulatePaymentApproval(preferenceId, fakeMpPaymentId);
            
            // Recargar el pago para obtener el estado actualizado
            payment = paymentService.getPaymentByPreferenceId(preferenceId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Pago simulado como aprobado exitosamente",
                "orderId", payment.getOrderId(),
                "paymentStatus", payment.getStatus().name(),
                "mpPaymentId", fakeMpPaymentId.toString()
            ));

        } catch (Exception e) {
            log.error("Error simulando aprobación de pago para preference: {}", preferenceId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al simular la aprobación: " + e.getMessage()));
        }
    }

    /**
     * Endpoint para consultar el estado de un pago por orderId
     * SEGURIDAD MULTI-TENANT: Valida que el usuario solo pueda ver sus propios pagos
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(
            @PathVariable String orderId,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader) {

        try {
            // SECURITY FIX: Validar JWT y ownership
            String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
            if (!jwtTokenValidator.validateToken(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token JWT inválido"));
            }

            Long adminIdFromToken = jwtTokenValidator.getUserIdFromToken(token);
            
            log.info("GET /api/payments/order/{} - AdminId from JWT: {}", orderId, adminIdFromToken);

            Payment payment = paymentService.getPaymentByOrderId(orderId);
            
            // OWNERSHIP CHECK: Verificar que el pago pertenece al admin autenticado
            if (!payment.getAdminId().equals(adminIdFromToken)) {
                log.warn("Admin {} intentó acceder al pago de Admin {}", adminIdFromToken, payment.getAdminId());
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para ver este pago"));
            }
            
            PaymentResponse response = PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .status(payment.getStatus().name())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .preferenceId(payment.getPreferenceId())
                    .message("Payment found")
                    .build();
            
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Error de autenticación: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting payment for order: {}", orderId, e);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pago no encontrado para la orden: " + orderId));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "payment-gateway",
                "version", "1.0.0"
        ));
    }

    /**
     * Endpoint para verificar manualmente el estado de un pago en MercadoPago
     * Útil cuando no se reciben webhooks automáticos
     * 
     * @param orderId Número de orden (ej: ORD-202511-123)
     * @return Estado actualizado del pago
     */
    @PostMapping("/verify/{orderId}")
    public ResponseEntity<?> verifyPayment(
            @PathVariable String orderId,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader) {

        try {
            // Validar JWT
            String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
            if (!jwtTokenValidator.validateToken(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token JWT inválido"));
            }

            Long userIdFromToken = jwtTokenValidator.getUserIdFromToken(token);
            
            log.info("POST /api/payments/verify/{} - UserId from JWT: {}", orderId, userIdFromToken);

            // Usar el nuevo método mejorado de verificación
            Payment payment = paymentService.verifyPaymentStatus(orderId);
            
            return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", payment.getStatus().name(),
                "verified", true,
                "hasMpPaymentId", payment.getMpPaymentId() != null,
                "message", payment.getMpPaymentId() != null 
                    ? "Payment status verified with MercadoPago" 
                    : "Payment found but not yet processed by MercadoPago. Complete the payment and try again."
            ));

        } catch (ResourceNotFoundException e) {
            log.error("Payment not found: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error de autenticación: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Error de autenticación: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error verifying payment for order: {}", orderId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar el pago: " + e.getMessage()));
        }
    }
}

