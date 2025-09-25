package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.consumption.ConsumptionDto;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDto;
import com.packed_go.event_service.dtos.consumptionCategory.ConsumptionCategoryDto;
import com.packed_go.event_service.dtos.consumptionCategory.CreateConsumptionCategoryDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ConsumptionCategoryService {
    ConsumptionCategoryDto findById(Long id);

    List<ConsumptionCategoryDto> findByActiveIsTrue();

    List<ConsumptionCategoryDto> findAll();

    ConsumptionCategoryDto create(CreateConsumptionCategoryDto createConsumptionCategoryDto);

    ConsumptionCategoryDto update(Long id, CreateConsumptionCategoryDto createConsumptionCategoryDto);

    void delete(Long id);

    @Transactional
    ConsumptionCategoryDto deleteLogical(Long id);

    @Transactional
    ConsumptionCategoryDto updateStatus(Long id);
}

