package com.packegoapi.stripe.controller;

import com.packegoapi.stripe.dto.CheckoutResponse;
import com.packegoapi.stripe.dto.CreateCheckoutRequest;
import com.packegoapi.stripe.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;

    /**
     * Crea una sesión de checkout
     * POST http://localhost:8081/api/stripe/create-checkout-session
     */
    @PostMapping("/create-checkout-session")
    public ResponseEntity<CheckoutResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutRequest request) {
        try {
            log.info("Recibida solicitud para crear sesión de checkout");
            CheckoutResponse response = stripeService.createCheckoutSession(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (StripeException e) {
            log.error("Error de Stripe: {}", e.getMessage());
            throw new RuntimeException("Error al crear sesión de checkout: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene información de una sesión
     * GET http://localhost:8081/api/stripe/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        try {
            log.info("Consultando sesión: {}", sessionId);
            Session session = stripeService.getCheckoutSession(sessionId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", session.getId());
            response.put("status", session.getStatus());
            response.put("payment_status", session.getPaymentStatus());
            response.put("customer_email", session.getCustomerEmail());
            response.put("amount_total", session.getAmountTotal());
            response.put("currency", session.getCurrency());
            response.put("metadata", session.getMetadata());

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Error al consultar sesión: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Sesión no encontrada"));
        }
    }

    /**
     * Obtiene información de un PaymentIntent
     * GET http://localhost:8081/api/stripe/payment/{paymentIntentId}
     */
    @GetMapping("/payment/{paymentIntentId}")
    public ResponseEntity<Map<String, Object>> getPayment(@PathVariable String paymentIntentId) {
        try {
            log.info("Consultando pago: {}", paymentIntentId);
            PaymentIntent payment = stripeService.getPaymentIntent(paymentIntentId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", payment.getId());
            response.put("status", payment.getStatus());
            response.put("amount", payment.getAmount());
            response.put("currency", payment.getCurrency());
            response.put("metadata", payment.getMetadata());

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Error al consultar pago: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Pago no encontrado"));
        }
    }

    /**
     * Health check
     * GET http://localhost:8081/api/stripe/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "API de Stripe funcionando correctamente"
        ));
    }
}
