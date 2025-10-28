package com.packedgo.payment_service.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.packedgo.payment_service.exception.CredentialException;
import com.packedgo.payment_service.model.AdminCredential;
import com.packedgo.payment_service.repository.AdminCredentialRepository;

@ExtendWith(MockitoExtension.class)
class AdminCredentialServiceTest {

    @Mock
    private AdminCredentialRepository credentialRepository;

    @InjectMocks
    private AdminCredentialService adminCredentialService;

    private AdminCredential validCredential;

    @BeforeEach
    void setUp() {
        validCredential = AdminCredential.builder()
                .id(1L)
                .adminId(1L)
                .accessToken("TEST-123456789-010101-abc123def456-789012345")
                .publicKey("TEST-abc123def-456789-012345-678901-234567")
                .isActive(true)
                .isSandbox(true)
                .build();
    }

    @Test
    void getValidatedCredentials_WhenCredentialsExist_ShouldReturnCredential() {
        // Given
        when(credentialRepository.findByAdminIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(validCredential));

        // When
        AdminCredential result = adminCredentialService.getValidatedCredentials(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getAdminId());
        assertEquals(validCredential.getAccessToken(), result.getAccessToken());
        verify(credentialRepository, times(1)).findByAdminIdAndIsActiveTrue(1L);
    }

    @Test
    void getValidatedCredentials_WhenCredentialsNotFound_ShouldThrowException() {
        // Given
        when(credentialRepository.findByAdminIdAndIsActiveTrue(anyLong()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(CredentialException.class, () -> {
            adminCredentialService.getValidatedCredentials(1L);
        });
    }

    @Test
    void getValidatedCredentials_WhenAccessTokenIsBlank_ShouldThrowException() {
        // Given
        AdminCredential credentialWithEmptyToken = AdminCredential.builder()
                .id(1L)
                .adminId(1L)
                .accessToken("")
                .isActive(true)
                .build();

        when(credentialRepository.findByAdminIdAndIsActiveTrue(1L))
                .thenReturn(Optional.of(credentialWithEmptyToken));

        // When & Then
        assertThrows(CredentialException.class, () -> {
            adminCredentialService.getValidatedCredentials(1L);
        });
    }

    @Test
    void saveCredentials_ShouldSaveAndReturnCredential() {
        // Given
        when(credentialRepository.findByAdminIdAndIsActiveTrue(1L))
                .thenReturn(Optional.empty());
        when(credentialRepository.save(any(AdminCredential.class)))
                .thenReturn(validCredential);

        // When
        AdminCredential result = adminCredentialService.saveCredentials(
                1L,
                "TEST-123456789-010101-abc123def456-789012345",
                "TEST-abc123def-456789-012345-678901-234567",
                true
        );

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getAdminId());
        verify(credentialRepository, times(1)).save(any(AdminCredential.class));
    }

    @Test
    void hasCredentials_WhenCredentialsExist_ShouldReturnTrue() {
        // Given
        when(credentialRepository.existsByAdminId(1L)).thenReturn(true);

        // When
        boolean result = adminCredentialService.hasCredentials(1L);

        // Then
        assertTrue(result);
        verify(credentialRepository, times(1)).existsByAdminId(1L);
    }

    @Test
    void hasCredentials_WhenCredentialsNotExist_ShouldReturnFalse() {
        // Given
        when(credentialRepository.existsByAdminId(1L)).thenReturn(false);

        // When
        boolean result = adminCredentialService.hasCredentials(1L);

        // Then
        assertFalse(result);
        verify(credentialRepository, times(1)).existsByAdminId(1L);
    }
}
