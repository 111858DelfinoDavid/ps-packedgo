package com.packedgo.payment_service.repository;

import com.packedgo.payment_service.model.AdminCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminCredentialRepository extends JpaRepository<AdminCredential, Long> {
    Optional<AdminCredential> findByAdminIdAndIsActiveTrue(Long adminId);

    boolean existsByAdminId(Long adminId);
}
