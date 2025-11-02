package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.event.CreateEventDTO;
import com.packed_go.event_service.dtos.event.EventDTO;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface EventService {
    EventDTO findById(Long id);
    List<EventDTO> findAll();

    List<EventDTO> findAllByStatus(String status);

    List<EventDTO> findAllByEventDate(LocalDateTime eventDate);

    List<EventDTO> findAllByEventDateAndStatus(LocalDateTime eventDate, String status);

    // List<EventDTO> findByLocation(Point location); // Comentado - usar lat/lng en su lugar


    EventDTO createEvent(CreateEventDTO createEventDto);

    EventDTO updateEvent(Long id, EventDTO eventDto);

    void delete(Long id);

    @Transactional
    EventDTO deleteLogical(Long id);
    
    List<ConsumptionDTO> getEventConsumptions(Long eventId);
}
