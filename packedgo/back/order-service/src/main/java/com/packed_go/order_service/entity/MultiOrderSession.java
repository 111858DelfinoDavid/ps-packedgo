package com.packed_go.order_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sesión que agrupa múltiples órdenes creadas en un mismo checkout
 * Útil cuando un cliente compra eventos de diferentes admins
 */
@Entity
@Table(name = "multi_order_sessions", indexes = {
    @Index(name = "idx_session_user_id", columnList = "user_id"),
    @Index(name = "idx_session_status", columnList = "session_status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiOrderSession {

    @Id
    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "cart_id")
    private Long cartId;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(name = "session_status", nullable = false, length = 30)
    private String sessionStatus = "PENDING"; // PENDING, PARTIAL, COMPLETED, EXPIRED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @OneToMany(mappedBy = "multiOrderSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // La sesión expira en 30 minutos
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Agrega una orden a la sesión
     */
    public void addOrder(Order order) {
        orders.add(order);
        order.setMultiOrderSession(this);
    }

    /**
     * Verifica si todas las órdenes están pagadas
     */
    public boolean allOrdersPaid() {
        if (orders.isEmpty()) {
            return false;
        }
        return orders.stream().allMatch(Order::isPaid);
    }

    /**
     * Cuenta cuántas órdenes están pagadas
     */
    public long getPaidOrdersCount() {
        return orders.stream().filter(Order::isPaid).count();
    }

    /**
     * Calcula el monto total pagado
     */
    public BigDecimal getTotalPaid() {
        return orders.stream()
                .filter(Order::isPaid)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcula el monto total pendiente
     */
    public BigDecimal getTotalPending() {
        return orders.stream()
                .filter(order -> !order.isPaid())
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Actualiza el estado de la sesión basado en las órdenes
     */
    public void updateSessionStatus() {
        if (orders.isEmpty()) {
            this.sessionStatus = "PENDING";
            return;
        }

        long paidCount = getPaidOrdersCount();
        long totalCount = orders.size();

        if (paidCount == 0) {
            this.sessionStatus = "PENDING";
        } else if (paidCount < totalCount) {
            this.sessionStatus = "PARTIAL";
        } else {
            this.sessionStatus = "COMPLETED";
        }
    }

    /**
     * Verifica si la sesión ha expirado
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
