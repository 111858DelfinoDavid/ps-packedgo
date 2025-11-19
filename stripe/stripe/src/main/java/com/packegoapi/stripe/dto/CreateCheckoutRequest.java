package com.packegoapi.stripe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Data
public class CreateCheckoutRequest {
    
    @NotNull(message = "Items no puede ser null")
    private List<CheckoutItem> items;
    
    @NotBlank(message = "Success URL es requerida")
    private String successUrl;
    
    @NotBlank(message = "Cancel URL es requerida")
    private String cancelUrl;
    
    private String customerEmail;
    private String externalReference;
    
    @Data
    public static class CheckoutItem {
        @NotBlank(message = "Nombre del producto es requerido")
        private String name;
        
        private String description;
        
        @NotNull(message = "Cantidad es requerida")
        @Positive(message = "Cantidad debe ser mayor a 0")
        private Long quantity;
        
        @NotNull(message = "Precio es requerido")
        @Positive(message = "Precio debe ser mayor a 0")
        private Long unitAmount; // Precio en centavos (ej: 1000 = $10.00)
        
        private String currency = "usd";
    }
}
