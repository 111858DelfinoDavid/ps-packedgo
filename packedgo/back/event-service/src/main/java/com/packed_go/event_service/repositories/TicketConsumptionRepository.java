package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.TicketConsumption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketConsumptionRepository extends JpaRepository<TicketConsumption,Long> {
    
    // Buscar un ticket específico con bloqueo pesimista para evitar concurrencia
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT tc FROM TicketConsumption tc WHERE tc.id = :id")
    Optional<TicketConsumption> findByIdWithLock(@Param("id") Long id);
}
