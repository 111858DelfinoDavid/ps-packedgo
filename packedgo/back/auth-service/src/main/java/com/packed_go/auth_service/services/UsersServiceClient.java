package com.packed_go.auth_service.services;

import com.packed_go.auth_service.dto.request.CreateProfileFromAuthRequest;

public interface UsersServiceClient {
    
    void createUserProfile(CreateProfileFromAuthRequest request);
}