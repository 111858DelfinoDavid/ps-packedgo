package com.packedgo.payment_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.packedgo.payment_service.exception.CredentialException;
import com.packedgo.payment_service.model.AdminCredential;
import com.packedgo.payment_service.repository.AdminCredentialRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCredentialService {
    private final AdminCredentialRepository credentialRepository;

    /**
     * Valida y obtiene las credenciales del admin de forma segura
     * NO recibe credenciales por parámetro, solo el adminId
     * Busca en la base de datos y valida que existan y estén activas
     */
    @Transactional(readOnly = true)
    public AdminCredential getValidatedCredentials(Long adminId) {
        log.info("Validando credenciales para admin: {}", adminId);

        AdminCredential credential = credentialRepository
                .findByAdminIdAndIsActiveTrue(adminId)
                .orElseThrow(() -> {
                    log.error("Credenciales no encontradas o inactivas para admin: {}", adminId);
                    return new CredentialException("Admin sin credenciales configuradas o credenciales inactivas");
                });

        // Validaciones adicionales de seguridad
        if (credential.getAccessToken() == null || credential.getAccessToken().isBlank()) {
            log.error("Access token vacío para admin: {}", adminId);
            throw new CredentialException("Credenciales incompletas");
        }

        log.info("Credenciales validadas exitosamente para admin: {} (Sandbox: {})",
                adminId, credential.getIsSandbox());

        return credential;
    }

    /**
     * Guarda o actualiza credenciales de un admin
     * Este método se llamaría desde el Dashboard del Admin
     */
    @Transactional
    public AdminCredential saveCredentials(Long adminId, String accessToken,
                                           String publicKey, Boolean isSandbox) {
        log.info("Guardando credenciales para admin: {}", adminId);

        AdminCredential credential = credentialRepository
                .findByAdminIdAndIsActiveTrue(adminId)
                .orElse(AdminCredential.builder()
                        .adminId(adminId)
                        .isActive(true)
                        .build());

        credential.setAccessToken(accessToken);
        credential.setPublicKey(publicKey);
        credential.setIsSandbox(isSandbox != null ? isSandbox : false);

        return credentialRepository.save(credential);
    }

    /**
     * Desactiva las credenciales de un admin
     */
    @Transactional
    public void deactivateCredentials(Long adminId) {
        credentialRepository.findByAdminIdAndIsActiveTrue(adminId)
                .ifPresent(credential -> {
                    credential.setIsActive(false);
                    credentialRepository.save(credential);
                    log.info("Credenciales desactivadas para admin: {}", adminId);
                });
    }

    /**
     * Verifica si un admin tiene credenciales configuradas
     */
    public boolean hasCredentials(Long adminId) {
        return credentialRepository.existsByAdminId(adminId);
    }
}
