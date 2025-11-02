package com.packed_go.order_service.service;

import java.util.List;

import com.packed_go.order_service.dto.MultiOrderCheckoutResponse;
import com.packed_go.order_service.dto.request.CheckoutRequest;
import com.packed_go.order_service.dto.request.PaymentCallbackRequest;
import com.packed_go.order_service.dto.response.CheckoutResponse;
import com.packed_go.order_service.entity.MultiOrderSession;
import com.packed_go.order_service.entity.Order;

public interface OrderService {
    
    /**
     * Procesa el checkout del carrito activo del usuario
     */
    CheckoutResponse checkout(Long userId, CheckoutRequest request);
    
    /**
     * Procesa el checkout multitenant creando múltiples órdenes si hay items de varios admins
     */
    MultiOrderCheckoutResponse checkoutMulti(Long userId);
    
    /**
     * Obtiene el estado de una sesión de múltiples órdenes
     */
    MultiOrderCheckoutResponse getSessionStatus(String sessionId);
    
    /**
     * Abandona una sesión de checkout y devuelve items al carrito si no hay pagos completados
     */
    void abandonSession(String sessionId, Long userId);
    
    /**
     * Actualiza el estado de una orden basado en el callback del pago
     */
    void updateOrderFromPaymentCallback(PaymentCallbackRequest request);
    
    /**
     * Obtiene todas las órdenes de un usuario
     */
    List<Order> getUserOrders(Long userId);
    
    /**
     * Obtiene una orden por su número
     */
    Order getOrderByNumber(String orderNumber);
    
    /**
     * Obtiene una orden por ID
     */
    Order getOrderById(Long orderId);
}
