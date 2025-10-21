package com.packed_go.order_service.service;

import com.packed_go.order_service.dto.request.AddToCartRequest;
import com.packed_go.order_service.dto.request.UpdateCartItemRequest;
import com.packed_go.order_service.dto.response.CartDTO;

/**
 * Servicio para gestionar el carrito de compras
 */
public interface CartService {
    
    /**
     * Agregar un evento con consumos al carrito del usuario
     * Si el usuario no tiene carrito activo, se crea uno nuevo
     * Si el evento ya está en el carrito, se actualizan las cantidades
     * 
     * @param userId ID del usuario
     * @param request Datos del evento y consumos a agregar
     * @return Carrito actualizado
     * @throws EventNotFoundException Si el evento no existe
     * @throws StockNotAvailableException Si no hay pases disponibles
     * @throws ServiceCommunicationException Si hay error de comunicación con EVENT-SERVICE
     */
    CartDTO addToCart(Long userId, AddToCartRequest request);
    
    /**
     * Obtener el carrito activo del usuario
     * Verifica si el carrito expiró y lo marca como tal
     * 
     * @param userId ID del usuario
     * @return Carrito del usuario
     * @throws CartNotFoundException Si el usuario no tiene carrito activo
     * @throws CartExpiredException Si el carrito expiró
     */
    CartDTO getActiveCart(Long userId);
    
    /**
     * Eliminar un item específico del carrito
     * Si el carrito queda vacío, se elimina completamente
     * 
     * @param userId ID del usuario
     * @param itemId ID del item a eliminar
     * @return Carrito actualizado o null si se eliminó
     * @throws CartNotFoundException Si el usuario no tiene carrito activo
     */
    CartDTO removeCartItem(Long userId, Long itemId);
    
    /**
     * Vaciar completamente el carrito del usuario
     * 
     * @param userId ID del usuario
     * @throws CartNotFoundException Si el usuario no tiene carrito activo
     */
    void clearCart(Long userId);
    
    /**
     * Actualizar la cantidad de un item en el carrito
     * Recalcula el subtotal automáticamente
     * 
     * @param userId ID del usuario
     * @param itemId ID del item a actualizar
     * @param request Nueva cantidad
     * @return Carrito actualizado
     * @throws CartNotFoundException Si el usuario no tiene carrito activo
     */
    CartDTO updateCartItemQuantity(Long userId, Long itemId, UpdateCartItemRequest request);
}
