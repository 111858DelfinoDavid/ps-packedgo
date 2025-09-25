package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.event.CreateEventDto;
import com.packed_go.event_service.dtos.event.EventDto;
import com.packed_go.event_service.entities.EventEntity;
import com.packed_go.event_service.entities.EventCategoryEntity;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.EventCategoryRepository;
import com.packed_go.event_service.services.EventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    @Autowired
    private final EventRepository eventRepository;

    @Autowired
    private final ModelMapper modelMapper;

    @Autowired
    private final EventCategoryRepository eventCategoryRepository;

    @Override
    public EventDto findById(Long id) {
        Optional<EventEntity> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            EventDto event = modelMapper.map(eventExist.get(), EventDto.class);
            return modelMapper.map(eventExist.get(), EventDto.class);
        } else {
            throw new RuntimeException("Event with id" + id + " not found");
        }

    }

    @Override
    public List<EventDto> findAll() {
        List<EventEntity> eventEntities = eventRepository.findAll();
        return eventEntities.stream().map(entity -> modelMapper.map(entity, EventDto.class)).toList();
    }


    @Override
    public List<EventDto> findAllByStatus(String status) {
        List<EventEntity> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getStatus().equalsIgnoreCase(status)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDto.class)).toList();
    }

    @Override
    public List<EventDto> findAllByEventDate(LocalDateTime eventDate) {
        List<EventEntity> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getEventDate().equals(eventDate)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDto.class)).toList();
    }

    @Override
    public List<EventDto> findAllByEventDateAndStatus(LocalDateTime eventDate, String status) {
        List<EventEntity> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getStatus().equalsIgnoreCase(status) && entity.getEventDate().equals(eventDate)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDto.class)).toList();
    }

    @Override
    public List<EventDto> findByLocation(Point location) {
        List<EventEntity> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getLocation().equals(location)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDto.class)).toList();
    }

    @Override
    public EventDto createEvent(CreateEventDto createEventDto) {
        EventCategoryEntity category = eventCategoryRepository.findById(createEventDto.getCategoryId()).orElseThrow(() -> new RuntimeException("Category with id " + createEventDto.getCategoryId() + " not found"));

        EventEntity eventEntity = modelMapper.map(createEventDto, EventEntity.class);

        eventEntity.setActive(true);
        eventEntity.setCategory(category);

        EventEntity savedEvent = eventRepository.save(eventEntity);

        return modelMapper.map(savedEvent, EventDto.class);
    }


    @Override
    public EventDto updateEvent(Long id, EventDto eventDto) {
        Optional<EventEntity> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            EventEntity entity = modelMapper.map(eventDto, EventEntity.class);
            entity.setId(id);
            EventEntity updatedEntity = eventRepository.save(entity);
            return modelMapper.map(updatedEntity, EventDto.class);
        } else {
            throw new RuntimeException("Event con id " + id + " no encontrado");
        }
    }

    @Override
    public void delete(Long id) {
        if (eventRepository.existsById(id)) {
            eventRepository.deleteById(id);
        } else {
            throw new RuntimeException("Event con id" + id + " no encontrado");
        }
    }

    @Transactional
    @Override
    public EventDto deleteLogical(Long id) {
        Optional<EventEntity> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            EventEntity entity = eventExist.get();
            entity.setActive(false);
            EventEntity updatedentity = eventRepository.save(entity);
            return modelMapper.map(updatedentity, EventDto.class);
        } else {
            throw new RuntimeException("Event con id" + id + " no encontrado");
        }

    }

}
