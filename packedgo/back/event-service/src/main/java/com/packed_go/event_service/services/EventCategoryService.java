package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.eventCategory.CreateEventCategoryDto;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface EventCategoryService {

    EventCategoryDto findById(Long id);

    List<EventCategoryDto> findByActiveIsTrue();

    List<EventCategoryDto> findAll();

    EventCategoryDto create(CreateEventCategoryDto createEventCategoryDto);

    EventCategoryDto update(Long id, CreateEventCategoryDto createEventCategoryDto);

    void delete(Long id);

    @Transactional
    EventCategoryDto deleteLogical(Long id);

    @Transactional
    EventCategoryDto updateStatus(Long id);
}
