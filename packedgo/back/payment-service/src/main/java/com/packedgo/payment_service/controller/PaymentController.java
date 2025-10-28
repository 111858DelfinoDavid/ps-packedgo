package com.packedgo.payment_service.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.packedgo.payment_service.dto.PaymentRequest;
import com.packedgo.payment_service.dto.PaymentResponse;
import com.packedgo.payment_service.dto.WebhookNotification;
import com.packedgo.payment_service.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * Endpoint para crear una preferencia de pago
     * IMPORTANTE: Solo recibe el adminId, las credenciales se obtienen internamente
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {

        log.info("POST /api/payments/create - AdminId: {}, OrderId: {}",
                request.getAdminId(), request.getOrderId());

        try {
            PaymentResponse response = paymentService.createPaymentPreference(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

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
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody WebhookNotification notification,
            @RequestParam(required = false) Long adminId) {

        log.info("POST /api/payments/webhook - Type: {}, Data: {}",
                notification.getType(), notification.getData());

        try {
            // Solo procesar notificaciones de pagos
            if ("payment".equals(notification.getType())) {
                Long paymentId = Long.valueOf(notification.getData().getId());

                // Procesar el webhook
                paymentService.processWebhookNotification(adminId, paymentId);

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
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(@PathVariable String orderId) {
        log.info("GET /api/payments/order/{}", orderId);

        try {
            // Implementar lógica para consultar pago
            return ResponseEntity.ok(Map.of("message", "Endpoint de consulta"));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pago no encontrado"));
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
