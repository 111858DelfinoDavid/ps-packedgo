package com.packed_go.order_service.service.impl;

import com.packed_go.order_service.dto.external.ConsumptionDTO;
import com.packed_go.order_service.dto.external.EventDTO;
import com.packed_go.order_service.dto.request.AddToCartRequest;
import com.packed_go.order_service.dto.request.UpdateCartItemRequest;
import com.packed_go.order_service.dto.response.CartDTO;
import com.packed_go.order_service.dto.response.CartItemConsumptionDTO;
import com.packed_go.order_service.dto.response.CartItemDTO;
import com.packed_go.order_service.entity.CartItem;
import com.packed_go.order_service.entity.CartItemConsumption;
import com.packed_go.order_service.entity.ShoppingCart;
import com.packed_go.order_service.exception.CartExpiredException;
import com.packed_go.order_service.exception.CartNotFoundException;
import com.packed_go.order_service.exception.StockNotAvailableException;
import com.packed_go.order_service.external.EventServiceClient;
import com.packed_go.order_service.repository.CartItemRepository;
import com.packed_go.order_service.repository.ShoppingCartRepository;
import com.packed_go.order_service.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    
    // Constante: Máximo de entradas por persona
    private static final int MAX_TICKETS_PER_PERSON = 10;
    
    private final ShoppingCartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final EventServiceClient eventServiceClient;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional
    public CartDTO addToCart(Long userId, AddToCartRequest request) {
        log.info("Adding event {} to cart for user {}", request.getEventId(), userId);
        
        // 1. Validar que el evento existe y tiene stock disponible
        EventDTO event = eventServiceClient.getEventById(request.getEventId());
        if (!eventServiceClient.checkPassAvailability(request.getEventId())) {
            log.warn("Event {} has no available passes", request.getEventId());
            throw new StockNotAvailableException(request.getEventId());
        }
        
        // 2. Obtener o crear carrito activo para el usuario
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElse(null);
        
        // 3. Verificar si el carrito existe y si expiró
        if (cart != null && cart.isExpired()) {
            log.warn("Cart {} expired for user {}, creating new cart", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            // Crear un nuevo carrito después de marcar el anterior como expirado
            cart = createNewCart(userId);
        } else if (cart == null) {
            // No hay carrito activo, crear uno nuevo
            log.info("No active cart found for user {}, creating new cart", userId);
            cart = createNewCart(userId);
        }
        
        // 4. Obtener cantidad a agregar (default 1 si no viene en el request)
        int quantityToAdd = (request.getQuantity() != null && request.getQuantity() > 0) 
                ? request.getQuantity() 
                : 1;
        
        // 5. Verificar si el evento ya está en el carrito
        CartItem cartItem = cartItemRepository.findByCartIdAndEventId(cart.getId(), request.getEventId())
                .orElse(null);
        
        // Calcular la cantidad total que tendría el item después de agregar
        int currentQuantity = (cartItem != null) ? cartItem.getQuantity() : 0;
        int totalQuantity = currentQuantity + quantityToAdd;
        
        // Validar que no exceda el máximo permitido
        if (totalQuantity > MAX_TICKETS_PER_PERSON) {
            throw new IllegalArgumentException(
                String.format("Cannot add more than %d tickets per person (current: %d, adding: %d)", 
                    MAX_TICKETS_PER_PERSON, currentQuantity, quantityToAdd)
            );
        }
        
        if (cartItem == null) {
            // Crear nuevo item en el carrito con la cantidad especificada
            cartItem = CartItem.builder()
                    .cart(cart)
                    .eventId(request.getEventId())
                    .eventName(event.getName())
                    .quantity(quantityToAdd)
                    .unitPrice(event.getBasePrice())
                    .consumptions(new ArrayList<>())
                    .build();
            
            cart.getItems().add(cartItem);
            log.info("Created new cart item with {} tickets for event {}", quantityToAdd, request.getEventId());
        } else {
            // Si ya existe, sumar la cantidad
            cartItem.setQuantity(totalQuantity);
            log.info("Updated cart item quantity from {} to {} for event {}", 
                    currentQuantity, totalQuantity, request.getEventId());
        }
        
        // 6. Agregar o actualizar consumos
        if (request.getConsumptions() != null && !request.getConsumptions().isEmpty()) {
            List<ConsumptionDTO> consumptionsInfo = eventServiceClient.getEventConsumptions(request.getEventId());
            
            for (AddToCartRequest.ConsumptionRequest consumptionReq : request.getConsumptions()) {
                ConsumptionDTO consumptionInfo = consumptionsInfo.stream()
                        .filter(c -> c.getId().equals(consumptionReq.getConsumptionId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Consumption " + consumptionReq.getConsumptionId() + " not found"));
                
                // Buscar si el consumo ya existe en el item
                CartItemConsumption existingConsumption = cartItem.getConsumptions().stream()
                        .filter(c -> c.getConsumptionId().equals(consumptionReq.getConsumptionId()))
                        .findFirst()
                        .orElse(null);
                
                if (existingConsumption != null) {
                    // Actualizar cantidad existente
                    existingConsumption.setQuantity(existingConsumption.getQuantity() + consumptionReq.getQuantity());
                } else {
                    // Crear nuevo consumo
                    CartItemConsumption newConsumption = CartItemConsumption.builder()
                            .cartItem(cartItem)
                            .consumptionId(consumptionReq.getConsumptionId())
                            .consumptionName(consumptionInfo.getName())
                            .quantity(consumptionReq.getQuantity())
                            .unitPrice(consumptionInfo.getPrice())
                            .build();
                    
                    cartItem.getConsumptions().add(newConsumption);
                }
            }
        }
        
        // 7. Recalcular subtotales
        cartItem.calculateSubtotal();
        
        // 8. Guardar carrito
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Event {} added to cart {} successfully", request.getEventId(), savedCart.getId());
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @Transactional
    public CartDTO getActiveCart(Long userId) {
        log.info("Retrieving active cart for user {}", userId);
        
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElse(null);
        
        // Si no hay carrito o expiró, crear uno nuevo
        if (cart == null) {
            log.info("No active cart found for user {}, creating new cart", userId);
            cart = createNewCart(userId);
        } else if (cart.isExpired()) {
            log.info("Cart {} expired for user {}, marking as expired and creating new cart", cart.getId(), userId);
            cart.setStatus("EXPIRED");
            cartRepository.save(cart);
            cart = createNewCart(userId);
        }
        
        return convertToDTO(cart);
    }
    
    @Override
    @Transactional
    public CartDTO removeCartItem(Long userId, Long itemId) {
        log.info("Removing item {} from cart for user {}", itemId, userId);
        
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new CartNotFoundException(userId));
        
        // Verificar si expiró
        if (cart.isExpired()) {
            log.warn("Cart {} expired for user {}", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException(cart.getId());
        }
        
        // Eliminar el item
        boolean removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        
        if (!removed) {
            log.warn("Item {} not found in cart {}", itemId, cart.getId());
            throw new RuntimeException("Item not found in cart");
        }
        
        // Si el carrito quedó vacío, eliminarlo
        if (cart.getItems().isEmpty()) {
            log.info("Cart {} is now empty, deleting it", cart.getId());
            cartRepository.delete(cart);
            return null;
        }
        
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Item {} removed from cart {} successfully", itemId, savedCart.getId());
        
        return convertToDTO(savedCart);
    }
    
    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);
        
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new CartNotFoundException(userId));
        
        cartRepository.delete(cart);
        log.info("Cart {} cleared successfully", cart.getId());
    }
    
    @Override
    @Transactional
    public CartDTO updateCartItemQuantity(Long userId, Long itemId, UpdateCartItemRequest request) {
        log.info("Updating item {} quantity to {} for user {}", itemId, request.getQuantity(), userId);
        
        ShoppingCart cart = cartRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .orElseThrow(() -> new CartNotFoundException(userId));
        
        // Verificar si expiró
        if (cart.isExpired()) {
            log.warn("Cart {} expired for user {}", cart.getId(), userId);
            cart.markAsExpired();
            cartRepository.save(cart);
            throw new CartExpiredException(cart.getId());
        }
        
        // Buscar el item
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));
        
        // Validar que no exceda el máximo permitido
        if (request.getQuantity() > MAX_TICKETS_PER_PERSON) {
            throw new IllegalArgumentException(
                String.format("Cannot add more than %d tickets per person (requested: %d)", 
                    MAX_TICKETS_PER_PERSON, request.getQuantity())
            );
        }
        
        // Actualizar cantidad
        item.setQuantity(request.getQuantity());
        item.calculateSubtotal();
        
        ShoppingCart savedCart = cartRepository.save(cart);
        log.info("Item {} quantity updated successfully", itemId);
        
        return convertToDTO(savedCart);
    }
    
    /**
     * Crea un nuevo carrito para el usuario
     */
    private ShoppingCart createNewCart(Long userId) {
        log.info("Creating new cart for user {}", userId);
        
        ShoppingCart cart = ShoppingCart.builder()
                .userId(userId)
                .status("ACTIVE")
                .items(new ArrayList<>())
                .build();
        
        return cartRepository.save(cart);
    }
    
    /**
     * Convierte una entidad ShoppingCart a CartDTO
     */
    private CartDTO convertToDTO(ShoppingCart cart) {
        CartDTO dto = CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .expiresAt(cart.getExpiresAt())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .expired(cart.isExpired())
                .totalAmount(cart.getTotalAmount())
                .itemCount(cart.getItems().size())
                .items(cart.getItems().stream()
                        .map(this::convertItemToDTO)
                        .collect(Collectors.toList()))
                .build();
        
        return dto;
    }
    
    /**
     * Convierte un CartItem a CartItemDTO
     */
    private CartItemDTO convertItemToDTO(CartItem item) {
        return CartItemDTO.builder()
                .id(item.getId())
                .eventId(item.getEventId())
                .eventName(item.getEventName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .consumptions(item.getConsumptions().stream()
                        .map(this::convertConsumptionToDTO)
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * Convierte un CartItemConsumption a CartItemConsumptionDTO
     */
    private CartItemConsumptionDTO convertConsumptionToDTO(CartItemConsumption consumption) {
        return CartItemConsumptionDTO.builder()
                .id(consumption.getId())
                .consumptionId(consumption.getConsumptionId())
                .consumptionName(consumption.getConsumptionName())
                .quantity(consumption.getQuantity())
                .unitPrice(consumption.getUnitPrice())
                .subtotal(consumption.getSubtotal())
                .build();
    }
}
