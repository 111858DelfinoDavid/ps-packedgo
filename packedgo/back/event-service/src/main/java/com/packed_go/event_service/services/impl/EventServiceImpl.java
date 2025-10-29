package com.packed_go.event_service.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.packed_go.event_service.dtos.event.CreateEventDTO;
import com.packed_go.event_service.dtos.event.EventDTO;
import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.EventCategory;
import com.packed_go.event_service.exceptions.ResourceNotFoundException;
import com.packed_go.event_service.repositories.EventCategoryRepository;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.services.EventService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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
    public EventDTO findById(Long id) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            EventDTO event = modelMapper.map(eventExist.get(), EventDTO.class);
            return modelMapper.map(eventExist.get(), EventDTO.class);
        } else {
            throw new RuntimeException("Event with id" + id + " not found");
        }

    }

    @Override
    public List<EventDTO> findAll() {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }


    @Override
    public List<EventDTO> findAllByStatus(String status) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getStatus().equalsIgnoreCase(status)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }

    @Override
    public List<EventDTO> findAllByEventDate(LocalDateTime eventDate) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getEventDate().equals(eventDate)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }

    @Override
    public List<EventDTO> findAllByEventDateAndStatus(LocalDateTime eventDate, String status) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getStatus().equalsIgnoreCase(status) && entity.getEventDate().equals(eventDate)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }

    /*
    @Override
    public List<EventDTO> findByLocation(Point location) {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().filter(entity -> entity.getLocation().equals(location)) // filtramos por status
                .map(entity -> modelMapper.map(entity, EventDTO.class)).toList();
    }
    */

    @Override
    public EventDTO createEvent(CreateEventDTO createEventDto) {
        EventCategory category = eventCategoryRepository.findById(createEventDto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + createEventDto.getCategoryId() + " not found"));

        Event event = new Event(); // Constructor inicializa status="ACTIVE", active=true, timestamps
        
        System.out.println("AFTER Constructor - Event status: " + event.getStatus());
        
        // Mapeo MANUAL para evitar problemas con ModelMapper
        event.setName(createEventDto.getName());
        event.setDescription(createEventDto.getDescription());
        event.setEventDate(createEventDto.getEventDate());
        event.setLat(createEventDto.getLat());
        event.setLng(createEventDto.getLng());
        event.setMaxCapacity(createEventDto.getMaxCapacity());
        event.setCurrentCapacity(createEventDto.getCurrentCapacity() != null ? createEventDto.getCurrentCapacity() : 0);
        event.setBasePrice(createEventDto.getBasePrice());
        event.setImageUrl(createEventDto.getImageUrl());
        event.setCreatedBy(createEventDto.getCreatedBy());
        event.setCategory(category);
        
        System.out.println("BEFORE save - Event status: " + event.getStatus());
        System.out.println("BEFORE save - Event name: " + event.getName());
        
        // El constructor ya inicializa: status, active, timestamps, passes, etc.
        // No es necesario setearlos de nuevo a menos que el DTO los override

        Event savedEvent = eventRepository.save(event);

        return modelMapper.map(savedEvent, EventDTO.class);
    }


    @Override
    public EventDTO updateEvent(Long id, EventDTO eventDto) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            Event entity = eventExist.get();

            // Si viene una categoryId en el DTO, la buscamos y la asignamos
            if (eventDto.getCategoryId() != null) {
                EventCategory category = eventCategoryRepository.findById(eventDto.getCategoryId())
                        .orElseThrow(() -> new RuntimeException("Category with id " + eventDto.getCategoryId() + " not found"));
                entity.setCategory(category);
            }

            // Actualizamos los campos relevantes desde el DTO
            entity.setName(eventDto.getName());
            entity.setDescription(eventDto.getDescription());
            entity.setEventDate(eventDto.getEventDate());
            entity.setLat(eventDto.getLat());
            entity.setLng(eventDto.getLng());
            entity.setMaxCapacity(eventDto.getMaxCapacity());
            entity.setCurrentCapacity(eventDto.getCurrentCapacity());
            entity.setBasePrice(eventDto.getBasePrice());
            entity.setImageUrl(eventDto.getImageUrl());
            entity.setStatus(eventDto.getStatus());
            entity.setActive(eventDto.isActive());
            // No tocar createdAt/createdBy para preservar histórico; actualizamos updatedAt
            entity.setUpdatedAt(LocalDateTime.now());

            // Campos de Pass (si vienen en el DTO)
            if (eventDto.getTotalPasses() != null) entity.setTotalPasses(eventDto.getTotalPasses());
            if (eventDto.getAvailablePasses() != null) entity.setAvailablePasses(eventDto.getAvailablePasses());
            if (eventDto.getSoldPasses() != null) entity.setSoldPasses(eventDto.getSoldPasses());

            Event updatedEntity = eventRepository.save(entity);
            return modelMapper.map(updatedEntity, EventDTO.class);
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
    public EventDTO deleteLogical(Long id) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            Event entity = eventExist.get();
            entity.setActive(false);
            Event updatedentity = eventRepository.save(entity);
            return modelMapper.map(updatedentity, EventDTO.class);
        } else {
            throw new RuntimeException("Event con id" + id + " no encontrado");
        }

    }

}
