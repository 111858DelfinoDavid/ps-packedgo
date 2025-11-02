package com.packed_go.consumption_service.dtos;
import lombok.*;
import jakarta.validation.constraints.*;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ValidateConsumptionRequest {
    @NotBlank(message = "QR code is required")
    private String qrCode;
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    private Long validatedBy;
}
