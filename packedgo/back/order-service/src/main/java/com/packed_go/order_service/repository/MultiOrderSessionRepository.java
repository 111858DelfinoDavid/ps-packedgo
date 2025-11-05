package com.packed_go.order_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.packed_go.order_service.entity.MultiOrderSession;

@Repository
public interface MultiOrderSessionRepository extends JpaRepository<MultiOrderSession, String> {

    /**
     * Busca una sesión por ID
     */
    Optional<MultiOrderSession> findBySessionId(String sessionId);

    /**
     * Busca una sesión por token de recuperación
     */
    Optional<MultiOrderSession> findBySessionToken(String sessionToken);

    /**
     * Busca todas las sesiones de un usuario
     */
    List<MultiOrderSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Busca sesiones pendientes de un usuario
     */
    @Query("SELECT s FROM MultiOrderSession s WHERE s.userId = ?1 AND s.sessionStatus IN ('PENDING', 'PARTIAL') AND s.expiresAt > ?2")
    List<MultiOrderSession> findPendingSessionsByUser(Long userId, LocalDateTime now);

    /**
     * Busca sesiones expiradas que aún no han sido marcadas como tal
     */
    @Query("SELECT s FROM MultiOrderSession s WHERE s.sessionStatus IN ('PENDING', 'PARTIAL') AND s.expiresAt < ?1")
    List<MultiOrderSession> findExpiredSessions(LocalDateTime now);
}
