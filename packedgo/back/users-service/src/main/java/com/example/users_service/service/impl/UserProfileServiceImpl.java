package com.example.users_service.service.impl;

import com.example.users_service.dto.UserProfileDTO;
import com.example.users_service.entity.UserProfile;
import com.example.users_service.repository.UserProfileRepository;
import com.example.users_service.service.UserProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;

    @Override
    @Transactional
    public UserProfileDTO create(UserProfileDTO dto) {
        UserProfile entity = mapToEntity(dto);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        UserProfile saved = repository.save(entity);
        return mapToDTO(saved);
    }

    @Override
    public UserProfileDTO getById(Long id) {
        return repository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));
    }

    @Override
    public List<UserProfileDTO> getAll() {
        return repository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserProfileDTO update(Long id, UserProfileDTO dto) {
        UserProfile entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserProfile not found"));

        entity.setName(dto.getName());
        entity.setLastName(dto.getLastName());
        entity.setGender(dto.getGender());
        entity.setDocument(dto.getDocument());
        entity.setBornDate(dto.getBornDate());
        entity.setTelephone(dto.getTelephone());
        entity.setProfileImageUrl(dto.getProfileImageUrl());
        entity.setPreferences(dto.getPreferences() != null ? dto.getPreferences().toString() : null);
        entity.setIsActive(dto.getIsActive());
        entity.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    // =================== MAPPERS ===================

    private UserProfileDTO mapToDTO(UserProfile entity) {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(entity.getId());
        dto.setAuthUserId(entity.getAuthUserId());
        dto.setName(entity.getName());
        dto.setLastName(entity.getLastName());
        dto.setGender(entity.getGender());
        dto.setDocument(entity.getDocument());
        dto.setBornDate(entity.getBornDate());
        dto.setTelephone(entity.getTelephone());
        dto.setProfileImageUrl(entity.getProfileImageUrl());
        dto.setPreferences(Map.of("raw", entity.getPreferences())); // simplificado
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setIsActive(entity.getIsActive());
        return dto;
    }

    private UserProfile mapToEntity(UserProfileDTO dto) {
        UserProfile entity = new UserProfile();
        entity.setId(dto.getId());
        entity.setAuthUserId(dto.getAuthUserId());
        entity.setName(dto.getName());
        entity.setLastName(dto.getLastName());
        entity.setGender(dto.getGender());
        entity.setDocument(dto.getDocument());
        entity.setBornDate(dto.getBornDate());
        entity.setTelephone(dto.getTelephone());
        entity.setProfileImageUrl(dto.getProfileImageUrl());
        entity.setPreferences(dto.getPreferences() != null ? dto.getPreferences().toString() : null);
        entity.setIsActive(dto.getIsActive());
        return entity;
    }
}
