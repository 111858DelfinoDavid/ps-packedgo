package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.CreateEventDto;
import com.packed_go.event_service.dtos.EventDto;
import com.packed_go.event_service.entities.EventEntity;
import jakarta.transaction.Transactional;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventService {
    EventDto findById(Long id);

    List<EventDto> findAllByStatus(String status);

    List<EventDto> findAllByEventDate(LocalDateTime eventDate);

    List<EventDto> findAllByEventDateAndStatus(LocalDateTime eventDate, String status);

    List<EventDto> findByLocation(Point location);


    EventDto createEvent(CreateEventDto createEventDto);

    EventDto updateEvent(Long id, EventDto eventDto);

    void delete(Long id);

    @Transactional
    EventDto deleteLogical(Long id);
}
