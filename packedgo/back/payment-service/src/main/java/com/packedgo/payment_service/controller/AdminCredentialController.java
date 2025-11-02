package com.packedgo.payment_service.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.packedgo.payment_service.dto.CredentialRequest;
import com.packedgo.payment_service.model.AdminCredential;
import com.packedgo.payment_service.service.AdminCredentialService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para que los admins gestionen sus credenciales de MercadoPago
 * Este endpoint será usado desde el Dashboard del Admin
 * 
 * IMPORTANTE: En producción, agregar autenticación JWT y validar que el admin
 * solo pueda modificar sus propias credenciales
 */
@RestController
@RequestMapping("/admin/credentials")
@RequiredArgsConstructor
@Slf4j
public class AdminCredentialController {
    
    private final AdminCredentialService credentialService;

    /**
     * Guardar o actualizar credenciales de MercadoPago del admin
     * TODO: Agregar @PreAuthorize para validar JWT del admin
     * TODO: Validar que principal.adminId == request.adminId
     */
    @PostMapping
    public ResponseEntity<?> saveCredentials(
            @Valid @RequestBody CredentialRequest request) {
        
        log.info("Guardando credenciales para admin: {}", request.getAdminId());

        try {
            // TODO: En producción, validar JWT
            // if (!jwtUtils.getAdminIdFromToken(token).equals(request.getAdminId())) {
            //     throw new ForbiddenException("No autorizado");
            // }

            AdminCredential credential = credentialService.saveCredentials(
                    request.getAdminId(),
                    request.getAccessToken(),
                    request.getPublicKey(),
                    request.getIsSandbox()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Credenciales guardadas exitosamente",
                    "adminId", credential.getAdminId(),
                    "isSandbox", credential.getIsSandbox(),
                    "isActive", credential.getIsActive()
            ));

        } catch (Exception e) {
            log.error("Error guardando credenciales para admin: {}", request.getAdminId(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar credenciales: " + e.getMessage()));
        }
    }

    /**
     * Verificar si el admin tiene credenciales configuradas
     */
    @GetMapping("/check/{adminId}")
    public ResponseEntity<?> checkCredentials(@PathVariable Long adminId) {
        log.info("Verificando credenciales para admin: {}", adminId);

        boolean hasCredentials = credentialService.hasCredentials(adminId);

        return ResponseEntity.ok(Map.of(
                "adminId", adminId,
                "hasCredentials", hasCredentials
        ));
    }

    /**
     * Desactivar credenciales del admin
     * TODO: Agregar autenticación y autorización
     */
    @DeleteMapping("/{adminId}")
    public ResponseEntity<?> deactivateCredentials(@PathVariable Long adminId) {
        log.info("Desactivando credenciales para admin: {}", adminId);

        try {
            credentialService.deactivateCredentials(adminId);

            return ResponseEntity.ok(Map.of(
                    "message", "Credenciales desactivadas exitosamente",
                    "adminId", adminId
            ));

        } catch (Exception e) {
            log.error("Error desactivando credenciales para admin: {}", adminId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al desactivar credenciales: " + e.getMessage()));
        }
    }
}
