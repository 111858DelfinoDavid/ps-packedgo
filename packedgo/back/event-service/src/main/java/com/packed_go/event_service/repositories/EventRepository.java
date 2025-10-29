package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findById(Long id);

    boolean existsById(Long id);

    Optional<List<Event>> findAllByStatus(String status);

    Optional<List<Event>> findAllByEventDate(LocalDateTime eventDate);

    Optional<List<Event>> findAllByEventDateAndStatus(LocalDateTime eventDate, String status);

    // Optional<List<Event>> findByLocation(Point location);

    // Métodos para gestión de Pass
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT e FROM Event e WHERE e.availablePasses > 0")
    List<Event> findEventsWithAvailablePasses();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.id = :eventId AND e.availablePasses > 0")
    Long hasAvailablePasses(@Param("eventId") Long eventId);

    // ========== QUERIES MULTI-TENANT (NUEVAS) ==========
    /**
     * 🔒 Encuentra un evento por ID y createdBy (validación de ownership)
     */
    Optional<Event> findByIdAndCreatedBy(Long id, Long createdBy);
    
    /**
     * 🔒 Encuentra todos los eventos de un usuario específico
     */
    List<Event> findByCreatedBy(Long createdBy);
    
    /**
     * 🔒 Encuentra eventos activos de un usuario
     */
    List<Event> findByCreatedByAndStatus(Long createdBy, String status);

}
