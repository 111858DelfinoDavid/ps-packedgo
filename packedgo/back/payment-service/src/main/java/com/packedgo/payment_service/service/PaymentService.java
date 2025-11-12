package com.packedgo.payment_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferencePaymentMethodsRequest;
import com.mercadopago.client.preference.PreferencePaymentTypeRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import com.packedgo.payment_service.client.OrderServiceClient;
import com.packedgo.payment_service.dto.PaymentRequest;
import com.packedgo.payment_service.dto.PaymentResponse;
import com.packedgo.payment_service.exception.PaymentException;
import com.packedgo.payment_service.exception.ResourceNotFoundException;
import com.packedgo.payment_service.model.AdminCredential;
import com.packedgo.payment_service.model.Payment;
import com.packedgo.payment_service.repository.PaymentRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final AdminCredentialService credentialService;
    private final OrderServiceClient orderServiceClient;

    @Value("${mercadopago.webhook.url:}")
    private String webhookUrl;

    public PaymentService(PaymentRepository paymentRepository,
                         AdminCredentialService credentialService,
                         OrderServiceClient orderServiceClient) {
        this.paymentRepository = paymentRepository;
        this.credentialService = credentialService;
        this.orderServiceClient = orderServiceClient;
    }

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
            
            log.info("URLs de retorno configuradas - Success: {}, Failure: {}, Pending: {}", 
                    request.getSuccessUrl(), request.getFailureUrl(), request.getPendingUrl());

            // 6.5. Configurar métodos de pago permitidos
            PreferencePaymentMethodsRequest paymentMethods = PreferencePaymentMethodsRequest.builder()
                    .installments(1) // Solo pagos en 1 cuota
                    .build();

            // 7. Crear la preferencia de pago
            PreferenceRequest.PreferenceRequestBuilder preferenceBuilder = PreferenceRequest.builder()
                    .items(items)
                    .payer(payer)
                    .backUrls(backUrls)
                    .paymentMethods(paymentMethods)
                    // .autoReturn("approved") - Deshabilitado: MercadoPago Sandbox rechaza autoReturn con error 400
                    .externalReference(payment.getOrderId()) // Usar orderId como external_reference
                    .statementDescriptor("PackedGo");

            // Solo habilitar autoReturn si las URLs son HTTPS (producción)
            // En desarrollo con localhost, el polling del frontend se encarga de la redirección
            if (request.getSuccessUrl() != null && request.getSuccessUrl().startsWith("https://")) {
                preferenceBuilder.autoReturn("approved");
                log.info("autoReturn habilitado para URLs HTTPS");
            } else {
                log.info("autoReturn deshabilitado - usando polling del frontend para redirección");
            }

            // Agregar notificationUrl si está configurada y es HTTPS (requerido por MercadoPago en producción)
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                if (webhookUrl.startsWith("https://") || credential.getIsSandbox()) {
                    // En sandbox se permite HTTP, en producción solo HTTPS
                    String fullWebhookUrl = webhookUrl + "?adminId=" + request.getAdminId();
                    preferenceBuilder.notificationUrl(fullWebhookUrl);
                    log.info("Webhook configurado: {}", fullWebhookUrl);
                } else {
                    log.warn("Webhook URL debe ser HTTPS en producción: {}", webhookUrl);
                }
            } else {
                log.warn("Webhook URL no configurada - las notificaciones automáticas no funcionarán");
            }

            PreferenceRequest preferenceRequest = preferenceBuilder.build();

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
     * SECURITY FIX: adminId se obtiene del payment lookup, no del parámetro
     */
    @Transactional
    public void processWebhookNotification(Long adminId, Long mpPaymentId) {
        log.info("Procesando webhook para MercadoPago payment: {}", mpPaymentId);

        try {
            // PASO 1: Primero consultar MercadoPago para obtener el external_reference
            // Necesitamos hacer esto SIN credenciales específicas para obtener el orderId
            // NOTA: Por seguridad, primero buscamos el pago en nuestra BD por mpPaymentId si existe
            Payment existingPayment = paymentRepository.findByMpPaymentId(mpPaymentId).orElse(null);
            
            Long actualAdminId;
            String externalReference;
            
            if (existingPayment != null) {
                // SECURITY: Si ya existe el pago, usamos su adminId (no el del parámetro)
                actualAdminId = existingPayment.getAdminId();
                externalReference = existingPayment.getOrderId();
                log.info("Pago existente encontrado en BD: adminId={}, orderId={}", actualAdminId, externalReference);
            } else {
                // Si no existe, necesitamos consultar MercadoPago
                // Por ahora, si adminId es null (llamado desde webhook), lanzamos excepción
                if (adminId == null) {
                    log.error("No se puede procesar webhook sin adminId y sin pago existente para mpPaymentId: {}", mpPaymentId);
                    throw new PaymentException("No se puede determinar el admin para este pago");
                }
                actualAdminId = adminId;
                externalReference = null; // Se obtendrá de MercadoPago
            }

            // PASO 2: Obtener credenciales del admin REAL (no del parámetro)
            AdminCredential credential = credentialService.getValidatedCredentials(actualAdminId);
            MercadoPagoConfig.setAccessToken(credential.getAccessToken());

            // PASO 3: Consultar el estado del pago en MercadoPago
            PaymentClient client = new PaymentClient();
            com.mercadopago.resources.payment.Payment mpPayment = client.get(mpPaymentId);

            log.info("Pago de MercadoPago obtenido: ID={}, Status={}, ExternalRef={}",
                    mpPayment.getId(), mpPayment.getStatus(), mpPayment.getExternalReference());

            // PASO 4: Buscar el pago en nuestra BD por el external_reference (que es el orderId)
            Payment payment = paymentRepository
                    .findByOrderId(mpPayment.getExternalReference())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pago no encontrado para external_reference: " + mpPayment.getExternalReference()));

            // SECURITY CHECK: Verificar que el adminId del pago coincide con el que obtuvimos
            if (!payment.getAdminId().equals(actualAdminId)) {
                log.error("SECURITY ALERT: AdminId mismatch. Payment.adminId={}, actualAdminId={}", 
                    payment.getAdminId(), actualAdminId);
                throw new PaymentException("Security validation failed: admin mismatch");
            }

            // Guardar estado anterior para comparar
            Payment.PaymentStatus previousStatus = payment.getStatus();
            Payment.PaymentStatus newStatus = mapMercadoPagoStatus(mpPayment.getStatus());

            // Actualizar información del pago
            payment.setMpPaymentId(mpPayment.getId());
            payment.setPaymentMethod(mpPayment.getPaymentMethodId());
            payment.setPaymentTypeId(mpPayment.getPaymentTypeId());
            payment.setStatus(newStatus);
            payment.setStatusDetail(mpPayment.getStatusDetail());
            payment.setTransactionAmount(mpPayment.getTransactionAmount());

            // Si tiene información del pagador, actualizarla
            if (mpPayment.getPayer() != null) {
                payment.setPayerEmail(mpPayment.getPayer().getEmail());
                if (mpPayment.getPayer().getFirstName() != null) {
                    payment.setPayerName(mpPayment.getPayer().getFirstName() + " " +
                            (mpPayment.getPayer().getLastName() != null ? mpPayment.getPayer().getLastName() : ""));
                }
            }

            // Si el pago fue aprobado, guardar fecha de aprobación
            if (newStatus == Payment.PaymentStatus.APPROVED && mpPayment.getDateApproved() != null) {
                payment.setApprovedAt(mpPayment.getDateApproved().toLocalDateTime());
            }

            paymentRepository.save(payment);

            log.info("Webhook procesado. Pago {} actualizado: {} -> {}",
                    payment.getId(), previousStatus, newStatus);

            // NOTIFICAR A ORDER-SERVICE si el estado cambió
            if (previousStatus != newStatus) {
                notifyOrderService(payment, newStatus, mpPayment.getStatusDetail());
            }

        } catch (ResourceNotFoundException e) {
            log.error("Pago no encontrado en BD: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error procesando webhook", e);
            throw new PaymentException("Error procesando notificación: " + e.getMessage());
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
                        payment.getMpPaymentId());

                if (success) {
                    log.info("Order-service notificado exitosamente para orden: {}", payment.getOrderId());
                } else {
                    log.warn("Falló la notificación a order-service para orden: {}", payment.getOrderId());
                }

            } else if (newStatus == Payment.PaymentStatus.REJECTED) {
                log.info("Notificando rechazo de pago a order-service: orderId={}", payment.getOrderId());
                boolean success = orderServiceClient.notifyPaymentRejected(
                        payment.getOrderId(),
                        payment.getMpPaymentId(),
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

    /**
     * Simula la aprobación de un pago SIN consultar a MercadoPago (SOLO PARA TESTING)
     * Esto es útil cuando no se puede validar el email en MercadoPago sandbox
     */
    public void simulatePaymentApproval(String preferenceId, Long fakeMpPaymentId) {
        log.info("TESTING: Simulando aprobación de pago para preferencia: {}", preferenceId);

        try {
            // Buscar el pago por preferenceId
            Payment payment = paymentRepository
                    .findByPreferenceId(preferenceId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pago no encontrado para preferencia: " + preferenceId));

            if (payment.getStatus() == Payment.PaymentStatus.APPROVED) {
                log.info("El pago {} ya estaba aprobado", payment.getId());
                return;
            }

            // Guardar estado anterior
            Payment.PaymentStatus previousStatus = payment.getStatus();

            // Actualizar el pago como aprobado
            payment.setMpPaymentId(fakeMpPaymentId);
            payment.setStatus(Payment.PaymentStatus.APPROVED);
            payment.setStatusDetail("approved");
            payment.setPaymentMethod("visa"); // Simulado
            payment.setPaymentTypeId("credit_card"); // Simulado
            payment.setApprovedAt(LocalDateTime.now());
            payment.setPayerName("Test User");
            payment.setPayerEmail("test@example.com");

            paymentRepository.save(payment);

            log.info("TESTING: Pago {} simulado como aprobado: {} -> APPROVED",
                    payment.getId(), previousStatus);

            // Notificar a order-service (esto genera los tickets)
            notifyOrderService(payment, Payment.PaymentStatus.APPROVED, "approved");

        } catch (ResourceNotFoundException e) {
            log.error("Pago no encontrado: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error simulando aprobación de pago", e);
            throw new PaymentException("Error simulando aprobación: " + e.getMessage());
        }
    }

    /**
     * Obtiene el estado de un pago por orderId
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(String orderId) {
        log.info("Consultando pago para orden: {}", orderId);
        
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró pago para la orden: " + orderId));
    }

    /**
     * Verifica el estado de un pago consultando MercadoPago
     * Funciona incluso si no se ha recibido el webhook
     * 
     * @param orderId Número de orden (ej: ORD-202511-123)
     * @return Payment actualizado con el estado de MercadoPago
     */
    @Transactional
    public Payment verifyPaymentStatus(String orderId) {
        log.info("🔍 Verificando estado de pago para orden: {}", orderId);
        
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró pago para la orden: " + orderId));
        
        try {
            // Configurar credenciales del admin
            AdminCredential credential = credentialService.getValidatedCredentials(payment.getAdminId());
            MercadoPagoConfig.setAccessToken(credential.getAccessToken());
            
            // Si ya tenemos mpPaymentId, consultar directamente
            if (payment.getMpPaymentId() != null) {
                log.info("Consultando MercadoPago con mpPaymentId: {}", payment.getMpPaymentId());
                PaymentClient client = new PaymentClient();
                com.mercadopago.resources.payment.Payment mpPayment = client.get(payment.getMpPaymentId());
                
                return updatePaymentFromMercadoPago(payment, mpPayment);
            }
            
            // Si no tenemos mpPaymentId pero tenemos preferenceId, buscar pagos asociados
            if (payment.getPreferenceId() != null) {
                log.info("Buscando pagos de MercadoPago para preferenceId: {}", payment.getPreferenceId());
                
                // Intentar buscar el pago usando el external_reference
                // MercadoPago SDK no tiene método directo para buscar por external_reference
                // pero podemos usar el Search API si está disponible
                
                // Por ahora, marcar como que no se puede verificar automáticamente
                log.warn("⚠️ No se puede verificar automáticamente sin mpPaymentId. " +
                        "El usuario debe completar el pago y esperar el webhook o " +
                        "consultar manualmente en MercadoPago.");
                
                return payment;
            }
            
            log.warn("No hay suficiente información para verificar el pago en MercadoPago");
            return payment;
            
        } catch (MPApiException apiException) {
            log.error("Error de API de MercadoPago al verificar: {} - {}", 
                    apiException.getStatusCode(), apiException.getApiResponse().getContent());
            throw new PaymentException("Error al verificar pago en MercadoPago: " + apiException.getMessage());
            
        } catch (MPException mpException) {
            log.error("Error de MercadoPago al verificar: {}", mpException.getMessage());
            throw new PaymentException("Error de conexión con MercadoPago: " + mpException.getMessage());
            
        } catch (Exception e) {
            log.error("Error inesperado al verificar pago", e);
            throw new PaymentException("Error al verificar el pago: " + e.getMessage());
        }
    }
    
    /**
     * Obtiene el estado de un pago por preferenceId
     * Usado por el frontend para hacer polling del estado del pago
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByPreferenceId(String preferenceId) {
        log.info("Consultando pago para preferencia: {}", preferenceId);
        
        return paymentRepository.findByPreferenceId(preferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró pago para la preferencia: " + preferenceId));
    }
    
    /**
     * Actualiza un pago con datos de MercadoPago
     */
    private Payment updatePaymentFromMercadoPago(Payment payment, 
                                                  com.mercadopago.resources.payment.Payment mpPayment) {
        Payment.PaymentStatus previousStatus = payment.getStatus();
        Payment.PaymentStatus newStatus = mapMercadoPagoStatus(mpPayment.getStatus());
        
        log.info("Estado anterior: {} → Estado nuevo: {}", previousStatus, newStatus);
        
        // Actualizar información del pago
        payment.setMpPaymentId(mpPayment.getId());
        payment.setPaymentMethod(mpPayment.getPaymentMethodId());
        payment.setPaymentTypeId(mpPayment.getPaymentTypeId());
        payment.setStatus(newStatus);
        payment.setStatusDetail(mpPayment.getStatusDetail());
        payment.setTransactionAmount(mpPayment.getTransactionAmount());
        
        // Actualizar información del pagador si está disponible
        if (mpPayment.getPayer() != null) {
            payment.setPayerEmail(mpPayment.getPayer().getEmail());
            if (mpPayment.getPayer().getFirstName() != null) {
                payment.setPayerName(mpPayment.getPayer().getFirstName() + " " +
                        (mpPayment.getPayer().getLastName() != null ? mpPayment.getPayer().getLastName() : ""));
            }
        }
        
        // Si el pago fue aprobado, guardar fecha de aprobación
        if (newStatus == Payment.PaymentStatus.APPROVED && mpPayment.getDateApproved() != null) {
            payment.setApprovedAt(mpPayment.getDateApproved().toLocalDateTime());
        }
        
        payment = paymentRepository.save(payment);
        
        // Notificar a Order Service si el estado cambió
        if (previousStatus != newStatus) {
            log.info("✅ Estado de pago cambió, notificando a Order Service");
            notifyOrderService(payment, newStatus, mpPayment.getStatusDetail());
        }
        
        return payment;
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
