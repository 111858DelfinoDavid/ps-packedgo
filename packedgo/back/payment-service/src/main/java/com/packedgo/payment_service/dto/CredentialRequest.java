package com.packedgo.payment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para que los admins configuren sus credenciales de MercadoPago
 * desde el Dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialRequest {

    @NotNull(message = "El ID del admin es requerido")
    private Long adminId;

    @NotBlank(message = "El Access Token es requerido")
    private String accessToken;

    private String publicKey;

    private Boolean isSandbox;
}
