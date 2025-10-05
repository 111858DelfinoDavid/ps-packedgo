package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.event.CreateEventDTO;
import com.packed_go.event_service.dtos.event.EventDTO;
import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.EventCategory;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.EventCategoryRepository;
import com.packed_go.event_service.services.EventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        EventCategory category = eventCategoryRepository.findById(createEventDto.getCategoryId()).orElseThrow(() -> new RuntimeException("Category with id " + createEventDto.getCategoryId() + " not found"));

        Event event = modelMapper.map(createEventDto, Event.class);

        event.setActive(true);
        event.setCategory(category);

        Event savedEvent = eventRepository.save(event);

        return modelMapper.map(savedEvent, EventDTO.class);
    }


    @Override
    public EventDTO updateEvent(Long id, EventDTO eventDto) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            Event entity = modelMapper.map(eventDto, Event.class);
            entity.setId(id);
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
