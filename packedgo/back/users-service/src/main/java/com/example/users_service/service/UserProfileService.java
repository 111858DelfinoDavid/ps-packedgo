package com.example.users_service.service;

import com.example.users_service.dto.UserProfileDTO;
import com.example.users_service.model.UserProfile;
import jakarta.transaction.Transactional;

import java.util.List;

public interface UserProfileService {
    UserProfile create(UserProfile model);

    UserProfile getById(Long id);

    UserProfile getByEmail(String email);

    UserProfile getByDocument(Long document);

    List<UserProfile> getAll();

    UserProfile update(Long id, UserProfile model);

    void delete(Long id);

    @Transactional
    UserProfile deleteLogical(Long id);

    List<UserProfile> getAllActive();

    UserProfile getByEmailActive(String email);

    UserProfile getByIdActive(Long id);

    UserProfile getByDocumentActive(Long document);
}