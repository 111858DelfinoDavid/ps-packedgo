package com.packed_go.users_service.controller;

import com.packed_go.users_service.dto.UserProfileDTO;
import com.packed_go.users_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.users_service.dto.request.UpdateUserProfileRequest;
import com.packed_go.users_service.model.UserProfile;
import com.packed_go.users_service.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/user-profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService service;
    private final ModelMapper modelMapper;


    @PostMapping
    public ResponseEntity<UserProfileDTO> create(@RequestBody UserProfileDTO dto) {
        UserProfile created = service.create(modelMapper.map(dto,UserProfile.class));
        if (created != null) {
            return ResponseEntity.ok(modelMapper.map(created,UserProfileDTO.class));
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @PostMapping("/from-auth")
    public ResponseEntity<UserProfileDTO> createFromAuthService(@RequestBody CreateProfileFromAuthRequest request) {
        try {
            log.info("Creating profile from auth-service for authUserId: {}", request.getAuthUserId());
            
            UserProfile created = service.createFromAuthService(request);
            
            log.info("Successfully created profile with ID: {} for authUserId: {}", 
                    created.getId(), request.getAuthUserId());
            
            return ResponseEntity.ok(modelMapper.map(created, UserProfileDTO.class));
            
        } catch (RuntimeException e) {
            log.warn("Failed to create profile from auth-service: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error creating profile from auth-service", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(service.getById(id),UserProfileDTO.class));
    }


    @GetMapping
    public ResponseEntity<List<UserProfile>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> update(@PathVariable Long id, @RequestBody UserProfile model) {
        return ResponseEntity.ok(service.update(id, model));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/logical/{id}")
    public ResponseEntity<UserProfileDTO> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(service.deleteLogical(id),UserProfileDTO.class));
    }


    @GetMapping("/active")
    public ResponseEntity<List<UserProfile>> getAllActive() {
        return ResponseEntity.ok(service.getAllActive());
    }


//    @GetMapping("/active/email/{email}")
//    public ResponseEntity<UserProfile> getByEmailActive(@PathVariable String email) {
//        return ResponseEntity.ok(service.getByEmailActive(email));
//    }


    @GetMapping("/active/{id}")
    public ResponseEntity<UserProfile> getByIdActive(@PathVariable Long id) {
        return ResponseEntity.ok(service.getByIdActive(id));
    }


    @GetMapping("/active/document/{document}")
    public ResponseEntity<UserProfile> getByDocumentActive(@PathVariable Long document) {
        return ResponseEntity.ok(service.getByDocumentActive(document));
    }

    @GetMapping("/by-auth-user/{authUserId}")
    public ResponseEntity<UserProfileDTO> getByAuthUserId(@PathVariable Long authUserId) {
        log.info("Getting user profile by authUserId: {}", authUserId);
        UserProfile profile = service.getByAuthUserId(authUserId);
        return ResponseEntity.ok(modelMapper.map(profile, UserProfileDTO.class));
    }

    @GetMapping("/active/by-auth-user/{authUserId}")
    public ResponseEntity<UserProfileDTO> getByAuthUserIdActive(@PathVariable Long authUserId) {
        log.info("Getting active user profile by authUserId: {}", authUserId);
        UserProfile profile = service.getByAuthUserIdActive(authUserId);
        return ResponseEntity.ok(modelMapper.map(profile, UserProfileDTO.class));
    }

    @PutMapping("/by-auth-user/{authUserId}")
    public ResponseEntity<UserProfileDTO> updateByAuthUserId(
            @PathVariable Long authUserId, 
            @Valid @RequestBody UpdateUserProfileRequest request) {
        
        log.info("Updating user profile by authUserId: {}", authUserId);
        
        try {
            UserProfile updated = service.updateByAuthUserId(authUserId, request);
            return ResponseEntity.ok(modelMapper.map(updated, UserProfileDTO.class));
        } catch (RuntimeException e) {
            log.warn("Failed to update profile for authUserId {}: {}", authUserId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
