package com.packedgo.payment_service.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.packedgo.payment_service.dto.PaymentRequest;
import com.packedgo.payment_service.dto.PaymentResponse;
import com.packedgo.payment_service.exception.PaymentException;
import com.packedgo.payment_service.exception.ResourceNotFoundException;
import com.packedgo.payment_service.model.AdminCredential;
import com.packedgo.payment_service.model.Payment;
import com.packedgo.payment_service.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AdminCredentialService credentialService;

    /**
     * Crea una preferencia de pago en MercadoPago
     * IMPORTANTE: Las credenciales se obtienen de la BD, NO del request
     */
    @Transactional
    public PaymentResponse createPaymentPreference(PaymentRequest request) {
        log.info("Iniciando creación de preferencia de pago para admin: {}, orden: {}",
                request.getAdminId(), request.getOrderId());

        try {
            // 1. VALIDACIÓN SEGURA: Obtener credenciales desde la BD
            AdminCredential credential = credentialService.getValidatedCredentials(request.getAdminId());

            // 2. Configurar MercadoPago con las credenciales del admin
            MercadoPagoConfig.setAccessToken(credential.getAccessToken());

            // 3. Crear el registro del pago en nuestra BD
            Payment payment = Payment.builder()
                    .adminId(request.getAdminId())
                    .orderId(request.getOrderId())
                    .amount(request.getAmount())
                    .currency("ARS")
                    .status(Payment.PaymentStatus.PENDING)
                    .payerEmail(request.getPayerEmail())
                    .description(request.getDescription())
                    .externalReference(request.getExternalReference())
                    .build();

            payment = paymentRepository.save(payment);
            log.info("Pago registrado en BD con ID: {}", payment.getId());

            // 4. Crear el item del pago
            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .id(request.getOrderId())
                    .title(request.getDescription())
                    .quantity(1)
                    .currencyId("ARS")
                    .unitPrice(request.getAmount())
                    .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(item);

            // 5. Configurar datos del pagador
            PreferencePayerRequest payer = PreferencePayerRequest.builder()
                    .email(request.getPayerEmail())
                    .name(request.getPayerName())
                    .build();

            // 6. Configurar URLs de retorno
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(request.getSuccessUrl())
                    .failure(request.getFailureUrl())
                    .pending(request.getPendingUrl())
                    .build();

            // 7. Crear la preferencia de pago
            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(payment.getId().toString())
                    .statementDescriptor("PackedGo")
                    .notificationUrl("https://tu-dominio.com/api/payments/webhook") // Configurar tu webhook
                    .build();

            // 8. Enviar a MercadoPago
            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            // 9. Actualizar el pago con el preference_id
            payment.setPreferenceId(preference.getId());
            paymentRepository.save(payment);

            log.info("Preferencia creada exitosamente: {} para orden: {}",
                    preference.getId(), request.getOrderId());

            // 10. Retornar respuesta
            return PaymentResponse.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .status(payment.getStatus().name())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .preferenceId(preference.getId())
                    .initPoint(preference.getInitPoint())
                    .sandboxInitPoint(preference.getSandboxInitPoint())
                    .message("Preferencia de pago creada exitosamente")
                    .build();

        } catch (MPApiException apiException) {
            log.error("Error de API de MercadoPago: {} - {}",
                    apiException.getStatusCode(), apiException.getApiResponse().getContent());
            throw new PaymentException("Error al crear preferencia de pago: " + apiException.getMessage());

        } catch (MPException mpException) {
            log.error("Error de MercadoPago: {}", mpException.getMessage());
            throw new PaymentException("Error de conexión con MercadoPago: " + mpException.getMessage());

        } catch (Exception e) {
            log.error("Error inesperado al crear preferencia de pago", e);
            throw new PaymentException("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * Procesa notificaciones de webhook de MercadoPago
     */
    @Transactional
    public void processWebhookNotification(Long adminId, Long paymentId) {
        log.info("Procesando webhook para admin: {}, payment: {}", adminId, paymentId);

        try {
            // Obtener credenciales del admin
            AdminCredential credential = credentialService.getValidatedCredentials(adminId);
            MercadoPagoConfig.setAccessToken(credential.getAccessToken());

            // Consultar el estado del pago en MercadoPago
            PaymentClient client = new PaymentClient();
            com.mercadopago.resources.payment.Payment mpPayment = client.get(paymentId);

            // Buscar el pago en nuestra BD
            Payment payment = paymentRepository
                    .findByPreferenceId(mpPayment.getExternalReference())
                    .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

            // Actualizar estado
            payment.setMpPaymentId(mpPayment.getId());
            payment.setPaymentMethod(mpPayment.getPaymentMethodId());
            payment.setStatus(mapMercadoPagoStatus(mpPayment.getStatus()));

            paymentRepository.save(payment);

            log.info("Webhook procesado. Pago {} actualizado a estado: {}",
                    payment.getId(), payment.getStatus());

        } catch (Exception e) {
            log.error("Error procesando webhook", e);
            throw new PaymentException("Error procesando notificación: " + e.getMessage());
        }
    }

    private Payment.PaymentStatus mapMercadoPagoStatus(String mpStatus) {
        return switch (mpStatus) {
            case "approved" -> Payment.PaymentStatus.APPROVED;
            case "rejected" -> Payment.PaymentStatus.REJECTED;
            case "cancelled" -> Payment.PaymentStatus.CANCELLED;
            case "refunded" -> Payment.PaymentStatus.REFUNDED;
            case "in_process" -> Payment.PaymentStatus.IN_PROCESS;
            default -> Payment.PaymentStatus.PENDING;
        };
    }
}
