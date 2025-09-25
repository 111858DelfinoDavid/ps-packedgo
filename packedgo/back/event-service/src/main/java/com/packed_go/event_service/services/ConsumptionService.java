package com.packed_go.event_service.services;

import com.packed_go.event_service.dtos.consumption.ConsumptionDto;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDto;
import com.packed_go.event_service.dtos.event.CreateEventDto;
import com.packed_go.event_service.dtos.event.EventDto;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

public interface ConsumptionService {
    ConsumptionDto findById(Long id);

    List<ConsumptionDto> findAll();

    List<ConsumptionDto> findAllByIsActive();

    ConsumptionDto createConsumption(CreateConsumptionDto createEventDto);

    ConsumptionDto updateConsumption(Long id, CreateConsumptionDto eventDto);

    void delete(Long id);

    @Transactional
    ConsumptionDto deleteLogical(Long id);

}
