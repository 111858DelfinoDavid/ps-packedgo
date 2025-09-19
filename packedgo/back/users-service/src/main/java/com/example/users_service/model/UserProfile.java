package com.example.users_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class UserProfile {

    private Long id;
    private Long auth_user_id;
    private String name;
    private String lastName;
    private String gender;
    private Long document;
    private LocalDate born_date;
    private Long telephone;
    private String profile_image_url;
    private String preferences;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private Boolean is_active;

}
