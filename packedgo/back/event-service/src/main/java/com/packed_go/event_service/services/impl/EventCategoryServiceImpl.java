package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.eventCategory.CreateEventCategoryDto;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDto;
import com.packed_go.event_service.entities.EventCategoryEntity;
import com.packed_go.event_service.repositories.EventCategoryRepository;
import com.packed_go.event_service.services.EventCategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventCategoryServiceImpl implements EventCategoryService {

    @Autowired
    private final EventCategoryRepository eventCategoryRepository;
    @Autowired
    private final ModelMapper modelMapper;

    @Override
    public EventCategoryDto findById(Long id) {
        Optional<EventCategoryEntity> categoryExist = eventCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            return modelMapper.map(categoryExist, EventCategoryDto.class);
        } else {
            throw new RuntimeException("Event con id " + id + " no encontrado");
        }
    }

    @Override
    public List<EventCategoryDto> findByActiveIsTrue() {
        List<EventCategoryEntity> categoryEntities = eventCategoryRepository.findByActiveIsTrue();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, EventCategoryDto.class))
                .toList();
    }

    @Override
    public List<EventCategoryDto> findAll() {
        List<EventCategoryEntity> categoryEntities = eventCategoryRepository.findAll();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, EventCategoryDto.class))
                .toList();
    }

    @Override
    public EventCategoryDto create(CreateEventCategoryDto createEventCategoryDto) {
        Optional<EventCategoryEntity> categoryExist = eventCategoryRepository.findByName(createEventCategoryDto.getName());
        if (categoryExist.isPresent()) {
            throw new RuntimeException("Event category ya existe");
        } else {
            EventCategoryEntity entity = modelMapper.map(createEventCategoryDto, EventCategoryEntity.class);
            entity.setActive(true);
            return modelMapper.map(eventCategoryRepository.save(entity), EventCategoryDto.class);
        }
    }

    @Override
    public EventCategoryDto update(Long id, CreateEventCategoryDto createEventCategoryDto) {
        Optional<EventCategoryEntity> categoryExist = eventCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            EventCategoryEntity entity = modelMapper.map(createEventCategoryDto, EventCategoryEntity.class);
            entity.setId(id);
            return modelMapper.map(eventCategoryRepository.save(entity), EventCategoryDto.class);
        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }
    }

    @Override
    public void delete(Long id) {
        if (eventCategoryRepository.existsById(id)) {
            eventCategoryRepository.deleteById(id);
        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }
    }

    @Override
    @Transactional
    public EventCategoryDto deleteLogical(Long id) {
        Optional<EventCategoryEntity> categoryExist = eventCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            EventCategoryEntity entity = categoryExist.get();
            entity.setActive(false);
            return modelMapper.map(eventCategoryRepository.save(entity), EventCategoryDto.class);

        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }

    }

    @Transactional
    @Override
    public EventCategoryDto updateStatus(Long id) {
        EventCategoryEntity entity = eventCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        entity.setActive(!entity.isActive());
        EventCategoryEntity updatedEntity = eventCategoryRepository.save(entity);
        return modelMapper.map(updatedEntity, EventCategoryDto.class);
    }

}
