package com.example.users_service.service;

import com.example.users_service.dto.UserProfileDTO;

import java.util.List;

public interface UserProfileService {
    UserProfileDTO create(UserProfileDTO dto);
    UserProfileDTO getById(Long id);
    List<UserProfileDTO> getAll();
    UserProfileDTO update(Long id, UserProfileDTO dto);
    void delete(Long id);
}