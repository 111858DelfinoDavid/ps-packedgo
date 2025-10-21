package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.event.CreateEventDTO;
import com.packed_go.event_service.dtos.event.EventDTO;
import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.entities.Event;
import com.packed_go.event_service.entities.EventCategory;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.repositories.EventCategoryRepository;
import com.packed_go.event_service.repositories.EventRepository;
import com.packed_go.event_service.repositories.ConsumptionRepository;
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

    @Autowired
    private final ConsumptionRepository consumptionRepository;

    @Override
    @Transactional
    public EventDTO findById(Long id) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            Event event = eventExist.get();
            EventDTO eventDTO = modelMapper.map(event, EventDTO.class);
            
            // Asegurar que el categoryId se mapee correctamente
            if (event.getCategory() != null) {
                eventDTO.setCategoryId(event.getCategory().getId());
            }
            
            // Mapear los consumos disponibles si existen
            if (event.getAvailableConsumptions() != null && !event.getAvailableConsumptions().isEmpty()) {
                List<ConsumptionDTO> consumptionDTOs = event.getAvailableConsumptions().stream()
                    .map(consumption -> {
                        ConsumptionDTO dto = modelMapper.map(consumption, ConsumptionDTO.class);
                        dto.setCategoryId(consumption.getCategory().getId());
                        return dto;
                    })
                    .toList();
                eventDTO.setAvailableConsumptions(consumptionDTOs);
            }
            
            return eventDTO;
        } else {
            throw new RuntimeException("Event with id" + id + " not found");
        }

    }

    @Override
    @Transactional
    public List<EventDTO> findAll() {
        List<Event> eventEntities = eventRepository.findAll();
        return eventEntities.stream().map(entity -> {
            EventDTO eventDTO = modelMapper.map(entity, EventDTO.class);
            
            // Asegurar que el categoryId se mapee correctamente
            if (entity.getCategory() != null) {
                eventDTO.setCategoryId(entity.getCategory().getId());
            }
            
            // Mapear los consumos disponibles si existen
            if (entity.getAvailableConsumptions() != null && !entity.getAvailableConsumptions().isEmpty()) {
                List<ConsumptionDTO> consumptionDTOs = entity.getAvailableConsumptions().stream()
                    .map(consumption -> {
                        ConsumptionDTO dto = modelMapper.map(consumption, ConsumptionDTO.class);
                        dto.setCategoryId(consumption.getCategory().getId());
                        return dto;
                    })
                    .toList();
                eventDTO.setAvailableConsumptions(consumptionDTOs);
            }
            
            return eventDTO;
        }).toList();
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
                .orElseThrow(() -> new RuntimeException("Category with id " + createEventDto.getCategoryId() + " not found"));

        Event event = modelMapper.map(createEventDto, Event.class);

        event.setActive(true);
        event.setCategory(category);
        
        // Inicializar passes disponibles igual a la capacidad máxima
        if (event.getMaxCapacity() != null && event.getMaxCapacity() > 0) {
            event.setTotalPasses(event.getMaxCapacity());
            event.setAvailablePasses(event.getMaxCapacity());
        }
        
        // Procesamos los consumptionIds si vienen en el DTO
        if (createEventDto.getConsumptionIds() != null && !createEventDto.getConsumptionIds().isEmpty()) {
            List<Consumption> consumptions = consumptionRepository.findAllById(createEventDto.getConsumptionIds());
            if (consumptions.size() != createEventDto.getConsumptionIds().size()) {
                throw new RuntimeException("Some consumption IDs were not found");
            }
            consumptions.forEach(event::addAvailableConsumption);
        }

        Event savedEvent = eventRepository.save(event);

        // Mapear el evento a DTO
        EventDTO eventDTO = modelMapper.map(savedEvent, EventDTO.class);
        
        // Mapear los consumos disponibles si existen
        if (savedEvent.getAvailableConsumptions() != null && !savedEvent.getAvailableConsumptions().isEmpty()) {
            List<ConsumptionDTO> consumptionDTOs = savedEvent.getAvailableConsumptions().stream()
                .map(consumption -> {
                    ConsumptionDTO dto = modelMapper.map(consumption, ConsumptionDTO.class);
                    dto.setCategoryId(consumption.getCategory().getId());
                    return dto;
                })
                .toList();
            eventDTO.setAvailableConsumptions(consumptionDTOs);
        }

        return eventDTO;
    }


    @Override
    public EventDTO updateEvent(Long id, EventDTO eventDto) {
        Optional<Event> eventExist = eventRepository.findById(id);
        if (eventExist.isPresent()) {
            Event existingEvent = eventExist.get();
            
            // Actualizar campos básicos
            existingEvent.setName(eventDto.getName());
            existingEvent.setDescription(eventDto.getDescription());
            existingEvent.setEventDate(eventDto.getEventDate());
            existingEvent.setLat(eventDto.getLat());
            existingEvent.setLng(eventDto.getLng());
            existingEvent.setMaxCapacity(eventDto.getMaxCapacity());
            existingEvent.setBasePrice(eventDto.getBasePrice());
            existingEvent.setImageUrl(eventDto.getImageUrl());
            existingEvent.setStatus(eventDto.getStatus());
            // NO actualizar 'active' aquí - se maneja con deleteLogical/activateLogical
            existingEvent.setUpdatedAt(LocalDateTime.now());
            
            // Actualizar categoría si cambió
            if (eventDto.getCategoryId() != null && 
                (existingEvent.getCategory() == null || !existingEvent.getCategory().getId().equals(eventDto.getCategoryId()))) {
                EventCategory category = eventCategoryRepository.findById(eventDto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category with id " + eventDto.getCategoryId() + " not found"));
                existingEvent.setCategory(category);
            }
            
            // Actualizar consumos disponibles
            // Primero limpiar las relaciones existentes
            existingEvent.getAvailableConsumptions().clear();
            
            // Agregar los nuevos consumos si existen en el DTO
            // Usar consumptionIds si está disponible, sino usar availableConsumptions
            List<Long> consumptionIds = null;
            
            if (eventDto.getConsumptionIds() != null && !eventDto.getConsumptionIds().isEmpty()) {
                // Usar los IDs directamente desde consumptionIds
                consumptionIds = eventDto.getConsumptionIds();
            } else if (eventDto.getAvailableConsumptions() != null && !eventDto.getAvailableConsumptions().isEmpty()) {
                // Fallback: extraer IDs desde availableConsumptions
                consumptionIds = eventDto.getAvailableConsumptions().stream()
                    .map(ConsumptionDTO::getId)
                    .toList();
            }
            
            if (consumptionIds != null && !consumptionIds.isEmpty()) {
                List<Consumption> consumptions = consumptionRepository.findAllById(consumptionIds);
                if (consumptions.size() != consumptionIds.size()) {
                    throw new RuntimeException("Some consumption IDs were not found");
                }
                consumptions.forEach(existingEvent::addAvailableConsumption);
            }
            
            Event updatedEntity = eventRepository.save(existingEvent);
            
            // Mapear el evento actualizado a DTO
            EventDTO resultDTO = modelMapper.map(updatedEntity, EventDTO.class);
            
            // Mapear los consumos disponibles si existen
            if (updatedEntity.getAvailableConsumptions() != null && !updatedEntity.getAvailableConsumptions().isEmpty()) {
                List<ConsumptionDTO> consumptionDTOs = updatedEntity.getAvailableConsumptions().stream()
                    .map(consumption -> {
                        ConsumptionDTO dto = modelMapper.map(consumption, ConsumptionDTO.class);
                        dto.setCategoryId(consumption.getCategory().getId());
                        return dto;
                    })
                    .toList();
                resultDTO.setAvailableConsumptions(consumptionDTOs);
            }
            
            return resultDTO;
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
