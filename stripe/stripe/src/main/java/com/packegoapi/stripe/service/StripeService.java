package com.packegoapi.stripe.service;

import com.packegoapi.stripe.dto.CheckoutResponse;
import com.packegoapi.stripe.dto.CreateCheckoutRequest;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${app.base.url}")
    private String baseUrl;

    /**
     * Crea una sesión de checkout de Stripe
     */
    public CheckoutResponse createCheckoutSession(CreateCheckoutRequest request) throws StripeException {
        log.info("Creando sesión de checkout para: {}", request.getCustomerEmail());

        // Construir los line items
        List<SessionCreateParams.LineItem> lineItems = request.getItems().stream()
                .map(this::buildLineItem)
                .collect(Collectors.toList());

        // Builder de la sesión
        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(request.getCancelUrl())
                .addAllLineItem(lineItems);

        // Agregar email del cliente si está presente
        if (request.getCustomerEmail() != null && !request.getCustomerEmail().isEmpty()) {
            sessionBuilder.setCustomerEmail(request.getCustomerEmail());
        }

        // Agregar metadata con referencia externa
        if (request.getExternalReference() != null) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("external_reference", request.getExternalReference());
            sessionBuilder.putAllMetadata(metadata);
        }

        // Crear la sesión
        SessionCreateParams params = sessionBuilder.build();
        Session session = Session.create(params);

        log.info("Sesión de checkout creada exitosamente. ID: {}", session.getId());

        return CheckoutResponse.builder()
                .sessionId(session.getId())
                .url(session.getUrl())
                .externalReference(request.getExternalReference())
                .build();
    }

    /**
     * Obtiene información de una sesión de checkout
     */
    public Session getCheckoutSession(String sessionId) throws StripeException {
        log.info("Obteniendo sesión de checkout: {}", sessionId);
        return Session.retrieve(sessionId);
    }

    /**
     * Obtiene información de un PaymentIntent
     */
    public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Obteniendo PaymentIntent: {}", paymentIntentId);
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Construye un line item para Stripe
     */
    private SessionCreateParams.LineItem buildLineItem(CreateCheckoutRequest.CheckoutItem item) {
        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(item.getName())
                        .setDescription(item.getDescription())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(item.getCurrency())
                        .setUnitAmount(item.getUnitAmount())
                        .setProductData(productData)
                        .build();

        return SessionCreateParams.LineItem.builder()
                .setPriceData(priceData)
                .setQuantity(item.getQuantity())
                .build();
    }
}
