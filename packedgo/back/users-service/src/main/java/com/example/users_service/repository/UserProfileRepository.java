package com.example.users_service.repository;

import com.example.users_service.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    boolean existsByDocument(Long document);
    boolean existsByTelephone(Long telephone);
}
