package com.example.users_service.controller;

import com.example.users_service.dto.UserProfileDTO;
import com.example.users_service.model.UserProfile;
import com.example.users_service.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-profiles")
@RequiredArgsConstructor
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
}
