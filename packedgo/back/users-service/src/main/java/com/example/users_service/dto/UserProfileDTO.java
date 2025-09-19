package com.example.users_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class UserProfileDTO {
    private Long id;
    private Long authUserId;
    private String name;
    private String lastName;
    private String gender;
    private Long document;
    private LocalDate bornDate;
    private Long telephone;
    private String profileImageUrl;
    private Map<String, Object> preferences;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}
