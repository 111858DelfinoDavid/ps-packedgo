package com.packegoapi.stripe.controller;

import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    /**
     * Endpoint para recibir webhooks de Stripe
     * POST http://localhost:8081/api/webhooks/stripe
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload) {
        log.info("Webhook recibido de Stripe");

        try {
            Event event = Event.GSON.fromJson(payload, Event.class);
            
            log.info("Tipo de evento: {}", event.getType());

            // Manejar diferentes tipos de eventos
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                    
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                    
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                    
                default:
                    log.info("Evento no manejado: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook procesado");

        } catch (Exception e) {
            log.error("Error procesando webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok("Error procesado");
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session != null) {
            log.info("Checkout completado - Session ID: {}, Email: {}", 
                    session.getId(), 
                    session.getCustomerEmail());
            
            // Aquí puedes agregar lógica de negocio:
            // - Actualizar estado del pedido en base de datos
            // - Enviar email de confirmación
            // - Generar tickets
            // etc.
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        log.info("Pago exitoso recibido");
        // Lógica para pago exitoso
    }

    private void handlePaymentIntentFailed(Event event) {
        log.info("Pago fallido recibido");
        // Lógica para pago fallido
    }
}
