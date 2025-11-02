package com.packed_go.order_service.service;

import com.packed_go.order_service.entity.MultiOrderSession;
import com.packed_go.order_service.entity.Order;
import com.packed_go.order_service.entity.ShoppingCart;
import com.packed_go.order_service.repository.ShoppingCartRepository;
import com.packed_go.order_service.repository.MultiOrderSessionRepository;
import com.packed_go.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para manejar tareas programadas relacionadas con sesiones y carritos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionExpirationService {

    private final MultiOrderSessionRepository sessionRepository;
    private final ShoppingCartRepository cartRepository;
    private final OrderRepository orderRepository;

    /**
     * Ejecuta cada 1 minuto para procesar sesiones expiradas
     * Devuelve items al carrito si hay pagos parciales o ningún pago
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000) // Cada 60 segundos
    @Transactional
    public void processExpiredSessions() {
        log.debug("🕐 Running expired sessions cleanup job");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<MultiOrderSession> expiredSessions = sessionRepository.findExpiredSessions(now);

            if (expiredSessions.isEmpty()) {
                log.debug("✓ No expired sessions found");
                return;
            }

            log.info("⚠️ Found {} expired session(s) to process", expiredSessions.size());

            for (MultiOrderSession session : expiredSessions) {
                processExpiredSession(session);
            }

        } catch (Exception e) {
            log.error("❌ Error processing expired sessions: {}", e.getMessage(), e);
        }
    }

    /**
     * Procesa una sesión expirada individual
     */
    private void processExpiredSession(MultiOrderSession session) {
        log.info("📦 Processing expired session: {}", session.getSessionId());

        try {
            List<Order> orders = session.getOrders();
            
            // Contar órdenes pagadas vs no pagadas
            long paidCount = session.getPaidOrdersCount();
            long totalCount = orders.size();
            long unpaidCount = totalCount - paidCount;

            log.info("Session {}: {} paid, {} unpaid orders", 
                    session.getSessionId(), paidCount, unpaidCount);

            // Si hay órdenes sin pagar, intentar recuperar items al carrito
            if (unpaidCount > 0) {
                recoverUnpaidItemsToCart(session, orders);
            }

            // Marcar la sesión como expirada
            session.setSessionStatus("EXPIRED");
            sessionRepository.save(session);

            // Cancelar órdenes no pagadas
            orders.forEach(order -> {
                if (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    orderRepository.save(order);
                    log.info("Cancelled unpaid order: {}", order.getOrderNumber());
                }
            });

            log.info("✅ Session {} processed successfully", session.getSessionId());

        } catch (Exception e) {
            log.error("❌ Error processing session {}: {}", session.getSessionId(), e.getMessage(), e);
        }
    }

    /**
     * Recupera items no pagados al carrito del usuario
     */
    private void recoverUnpaidItemsToCart(MultiOrderSession session, List<Order> orders) {
        try {
            // Obtener el carrito asociado
            ShoppingCart cart = cartRepository.findById(session.getCartId())
                    .orElse(null);

            if (cart == null) {
                log.warn("⚠️ Cart {} not found for session {}", session.getCartId(), session.getSessionId());
                return;
            }

            // Solo procesar si el carrito está en IN_CHECKOUT
            if (!"IN_CHECKOUT".equals(cart.getStatus())) {
                log.debug("Cart {} is not in IN_CHECKOUT status (current: {}), skipping recovery", 
                        cart.getId(), cart.getStatus());
                return;
            }

            // Verificar si hay al menos una orden pagada
            boolean hasAnyPaidOrder = orders.stream()
                    .anyMatch(order -> order.getStatus() == Order.OrderStatus.PAID);

            if (hasAnyPaidOrder) {
                // Si hay pagos parciales, devolver items no pagados al carrito
                log.info("🔄 Session {} has partial payments. Recovering unpaid items to cart", 
                        session.getSessionId());
                
                // Reactivar el carrito (volver a ACTIVE y extender expiración)
                cart.reactivate();
                cartRepository.save(cart);
                
                log.info("✅ Cart {} reactivated with unpaid items for session {}", 
                        cart.getId(), session.getSessionId());
            } else {
                // Si NO hay pagos, simplemente reactivar el carrito con todos los items
                log.info("🔄 Session {} has no payments. Reactivating full cart", 
                        session.getSessionId());
                
                cart.reactivate();
                cartRepository.save(cart);
                
                log.info("✅ Full cart {} reactivated for session {}", 
                        cart.getId(), session.getSessionId());
            }

        } catch (Exception e) {
            log.error("❌ Error recovering items to cart for session {}: {}", 
                    session.getSessionId(), e.getMessage(), e);
        }
    }
}
