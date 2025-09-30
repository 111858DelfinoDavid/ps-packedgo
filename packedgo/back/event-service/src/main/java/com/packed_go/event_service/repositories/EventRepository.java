package com.packed_go.event_service.repositories;

import com.packed_go.event_service.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.*;
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

    Optional<List<Event>> findByLocation(Point location);



}
