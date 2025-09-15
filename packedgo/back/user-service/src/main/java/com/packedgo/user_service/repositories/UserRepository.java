package com.packedgo.user_service.repositories;

import com.packedgo.user_service.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByDocument(Long document);

    Optional<UserEntity> findByEmail(String email);

    void deleteByDocument(Long document);
}
