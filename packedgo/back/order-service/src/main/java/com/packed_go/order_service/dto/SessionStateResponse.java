package com.packed_go.order_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Backend State Authority: Estado completo de una sesión de checkout
 * El frontend NO necesita guardar nada, solo consumir este DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStateResponse {
    
    // Session info
    private String sessionId;
    private String sessionStatus; // PENDING, PARTIAL, COMPLETED, EXPIRED, CANCELLED
    private BigDecimal totalAmount;
    private LocalDateTime expiresAt;
    private LocalDateTime lastAccessedAt;
    private Integer attemptCount;
    
    // Session health
    private boolean isExpired;
    private boolean isActive;
    private boolean isCompleted;
    private long secondsUntilExpiration;
    
    // Payment groups (cada admin)
    private List<PaymentGroupInfo> paymentGroups;
    
    // Stats
    private int totalGroups;
    private int paidGroups;
    private int pendingGroups;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentGroupInfo {
        private Long adminId;
        private String adminName;
        private String orderId;
        private String orderNumber;
        private BigDecimal amount;
        private String paymentStatus; // PENDING, PAID, FAILED, CANCELLED
        private String initPoint; // URL de MercadoPago
        private List<OrderItemInfo> items;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {
        private Long eventId;
        private String eventName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
