package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {
    Optional<EventEntity> findById(Long id);

    boolean existsById(Long id);

    Optional<List<EventEntity>> findAllByStatus(String status);

    Optional<List<EventEntity>> findAllByEventDate(LocalDateTime eventDate);

    Optional<List<EventEntity>> findAllByEventDateAndStatus(LocalDateTime eventDate, String status);

    Optional<List<EventEntity>> findByLocation(Point location);



}
