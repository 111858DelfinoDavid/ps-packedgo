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
import com.packedgo.payment_service.model.Payment;
import com.packedgo.payment_service.service.PaymentService;
import com.packedgo.payment_service.security.JwtTokenValidator;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final JwtTokenValidator jwtTokenValidator;

    /**
     * Endpoint para crear una preferencia de pago
     * SEGURIDAD MULTI-TENANT: El adminId se extrae del JWT, no del request body
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authHeader) {

        try {
            // SECURITY FIX: Extraer adminId del JWT token
            String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
            if (!jwtTokenValidator.validateToken(token)) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(PaymentResponse.builder()
                                .message("Token JWT inválido")
                                .build());
            }

            Long adminIdFromToken = jwtTokenValidator.getUserIdFromToken(token);
            
            // Inyectar el adminId desde el token (no confiar en el request body)
            request.setAdminId(adminIdFromToken);

            log.info("POST /api/payments/create - AdminId from JWT: {}, OrderId: {}",
                    adminIdFromToken, request.getOrderId());

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
}
