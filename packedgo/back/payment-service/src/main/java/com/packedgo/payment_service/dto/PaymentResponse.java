package com.packedgo.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private String orderId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String initPoint; // URL de MercadoPago para realizar el pago
    private String preferenceId;
    private String sandboxInitPoint;
    private String message;
}