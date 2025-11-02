package com.packed_go.consumption_service.dtos;
import lombok.*;
import jakarta.validation.constraints.NotBlank;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ValidateEntryRequest {
    @NotBlank(message = "QR code is required")
    private String qrCode;
    private Long validatedBy;
}
