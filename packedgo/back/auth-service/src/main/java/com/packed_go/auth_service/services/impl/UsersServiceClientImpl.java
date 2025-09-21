package com.packed_go.auth_service.services.impl;

import com.packed_go.auth_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.auth_service.services.UsersServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsersServiceClientImpl implements UsersServiceClient {

    @Qualifier("usersServiceWebClient")
    private final WebClient webClient;

    @Override
    public void createUserProfile(CreateProfileFromAuthRequest request) {
        try {
            log.info("Calling users-service to create profile for authUserId: {}", request.getAuthUserId());
            
            webClient.post()
                    .uri("/api/user-profiles/from-auth")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(); // Bloquear para hacer la llamada síncrona
                    
            log.info("Successfully created user profile for authUserId: {}", request.getAuthUserId());
            
        } catch (Exception e) {
            log.error("Failed to create user profile for authUserId: {}", request.getAuthUserId(), e);
            // No lanzamos la excepción para que el registro en auth-service continúe
            // En un entorno productivo podrías implementar retry logic o dead letter queue
        }
    }
}