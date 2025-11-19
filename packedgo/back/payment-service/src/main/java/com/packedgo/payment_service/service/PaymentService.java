package com.packedgo.payment_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.packedgo.payment_service.client.OrderServiceClient;
import com.packedgo.payment_service.dto.PaymentRequest;
import com.packedgo.payment_service.dto.PaymentResponse;
import com.packedgo.payment_service.dto.StripeCheckoutRequest;
import com.packedgo.payment_service.dto.StripeCheckoutResponse;
import com.packedgo.payment_service.exception.PaymentException;
import com.packedgo.payment_service.model.Payment;
import com.packedgo.payment_service.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;
    private final StripeService stripeService;

    public PaymentService(PaymentRepository paymentRepository,
                         OrderServiceClient orderServiceClient,
                         StripeService stripeService) {
        this.paymentRepository = paymentRepository;
        this.orderServiceClient = orderServiceClient;
        this.stripeService = stripeService;
    }

    /**
     * Crea un pago usando Stripe Checkout
     */
    @Transactional
    public PaymentResponse createPaymentWithStripe(PaymentRequest request) {
        log.info("🔄 Creando pago con Stripe para orderId: {}", request.getOrderId());
        
        // ✅ VERIFICAR SI YA EXISTE UN PAGO PARA ESTA ORDEN
        Payment existingPayment = paymentRepository.findByOrderId(request.getOrderId()).orElse(null);
        
        if (existingPayment != null) {
            log.info("✅ Pago ya existe para orderId: {}. Retornando pago existente.", request.getOrderId());
            
            // Si ya tiene una sesión de Stripe, retornar la URL existente
            if (existingPayment.getStripeSessionId() != null) {
                String checkoutUrl = stripeService.getCheckoutUrlFromSession(existingPayment.getStripeSessionId());
                
                return PaymentResponse.builder()
                    .id(existingPayment.getId())
                    .orderId(existingPayment.getOrderId())
                    .amount(existingPayment.getAmount())
                    .status(existingPayment.getStatus().toString())
                    .checkoutUrl(checkoutUrl)
                    .sessionId(existingPayment.getStripeSessionId())
                    .createdAt(existingPayment.getCreatedAt().toString())
                    .build();
            }
        }
        
        // Crear nueva entidad de pago
        Payment payment = new Payment();
        payment.setAdminId(request.getAdminId()); // ✅ AGREGADO: Asignar adminId del request
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setDescription(request.getDescription());
        payment.setPaymentProvider("STRIPE");
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        
        // Preparar items para Stripe
        List<StripeCheckoutRequest.ItemDTO> items = new ArrayList<>();
        items.add(StripeCheckoutRequest.ItemDTO.builder()
            .name(request.getDescription())
            .description(request.getDescription())
            .quantity(1)
            .unitPrice(request.getAmount())
            .build());
        
        StripeCheckoutRequest stripeRequest = StripeCheckoutRequest.builder()
            .orderId(request.getOrderId())
            .items(items)
            .build();
        
        try {
            // Crear sesión de Stripe Checkout
            StripeCheckoutResponse stripeResponse = stripeService.createCheckoutSession(stripeRequest);
            
            // Actualizar payment con datos de Stripe
            payment.setStripeSessionId(stripeResponse.getSessionId());
            payment = paymentRepository.save(payment);
            
            log.info(" Pago Stripe creado exitosamente. PaymentId: {}, SessionId: {}", 
                payment.getId(), stripeResponse.getSessionId());
            
            // Construir respuesta (compatibilidad con frontend: initPoint + preferenceId)
            PaymentResponse response = PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .status(payment.getStatus().toString())
                .checkoutUrl(stripeResponse.getCheckoutUrl())
                .initPoint(stripeResponse.getCheckoutUrl())       // Legacy field used by frontend
                .preferenceId(stripeResponse.getSessionId())     // Map sessionId -> preferenceId for frontend
                .sessionId(stripeResponse.getSessionId())
                .paymentProvider("STRIPE")
                .createdAt(payment.getCreatedAt().toString())
                .build();
            
            return response;
            
        } catch (Exception e) {
            log.error(" Error creando sesión de Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear pago con Stripe: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica el estado del pago en Stripe y actualiza si es necesario
     */
    @Transactional
    public PaymentResponse verifyPaymentStatus(String orderId) {
        log.info("🔍 Verificando estado de pago para orderId: {}", orderId);
        
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Pago no encontrado para orden: " + orderId));
            
        if (payment.getStripeSessionId() != null) {
            try {
                com.stripe.model.checkout.Session session = stripeService.retrieveSession(payment.getStripeSessionId());
                
                if ("complete".equals(session.getStatus()) || "paid".equals(session.getPaymentStatus())) {
                    if (payment.getStatus() != Payment.PaymentStatus.APPROVED) {
                        log.info("✅ Pago verificado en Stripe como completado. Actualizando estado local.");
                        handleStripePaymentSuccess(payment.getStripeSessionId());
                        // Recargar pago actualizado
                        payment = paymentRepository.findById(payment.getId()).orElse(payment);
                    }
                }
            } catch (Exception e) {
                log.error("Error verificando estado en Stripe: {}", e.getMessage());
                // No lanzamos error, devolvemos el estado actual
            }
        }
        
        return PaymentResponse.builder()
            .id(payment.getId())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .status(payment.getStatus().toString())
            .sessionId(payment.getStripeSessionId())
            .paymentProvider(payment.getPaymentProvider())
            .createdAt(payment.getCreatedAt().toString())
            .build();
    }

    /**
     * Maneja el éxito de un pago de Stripe (llamado por webhook)
     */
    @Transactional
    public void handleStripePaymentSuccess(String sessionId) {
        log.info(" Manejando pago exitoso de Stripe para sessionId: {}", sessionId);
        
        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
            .orElseThrow(() -> new RuntimeException("Pago no encontrado para sessionId: " + sessionId));
        
        try {
            // Obtener detalles de la sesión de Stripe
            com.stripe.model.checkout.Session session = stripeService.retrieveSession(sessionId);
            String paymentIntentId = session.getPaymentIntent();
            
            // Actualizar payment
            Payment.PaymentStatus previousStatus = payment.getStatus();
            payment.setStatus(Payment.PaymentStatus.APPROVED);
            payment.setStripePaymentIntentId(paymentIntentId);
            payment.setApprovedAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            payment = paymentRepository.save(payment);
            
            log.info(" Pago Stripe aprobado. PaymentId: {}, PaymentIntentId: {}", 
                payment.getId(), paymentIntentId);
            
            // Notificar a Order Service si el estado cambió
            if (previousStatus != Payment.PaymentStatus.APPROVED) {
                notifyOrderService(payment, Payment.PaymentStatus.APPROVED, "approved");
            }
            
        } catch (Exception e) {
            log.error(" Error procesando pago exitoso de Stripe: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando pago de Stripe: " + e.getMessage(), e);
        }
    }

    /**
     * Notifica a order-service sobre el cambio de estado del pago
     */
    private void notifyOrderService(Payment payment, Payment.PaymentStatus newStatus, String statusDetail) {
        try {
            if (newStatus == Payment.PaymentStatus.APPROVED) {
                log.info("Notificando aprobación de pago a order-service: orderId={}", payment.getOrderId());
                boolean success = orderServiceClient.notifyPaymentApproved(
                        payment.getOrderId(),
                        payment.getId());

                if (success) {
                    log.info("Order-service notificado exitosamente para orden: {}", payment.getOrderId());
                } else {
                    log.warn("Falló la notificación a order-service para orden: {}", payment.getOrderId());
                }

            } else if (newStatus == Payment.PaymentStatus.REJECTED) {
                log.info("Notificando rechazo de pago a order-service: orderId={}", payment.getOrderId());
                boolean success = orderServiceClient.notifyPaymentRejected(
                        payment.getOrderId(),
                        payment.getId(),
                        statusDetail);

                if (success) {
                    log.info("Order-service notificado de rechazo para orden: {}", payment.getOrderId());
                } else {
                    log.warn("Falló la notificación de rechazo a order-service para orden: {}", payment.getOrderId());
                }
            }

        } catch (Exception e) {
            log.error("Error al notificar a order-service: {}", e.getMessage());
            // No lanzamos excepción para no fallar el webhook
        }
    }
}
