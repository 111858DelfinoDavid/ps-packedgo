package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.consumption.ConsumptionDto;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDto;
import com.packed_go.event_service.entities.ConsumptionCategoryEntity;
import com.packed_go.event_service.entities.ConsumptionEntity;
import com.packed_go.event_service.repositories.ConsumptionCategoryRepository;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.services.ConsumptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConsumptionServiceImpl implements ConsumptionService {

    @Autowired
    public ConsumptionRepository consumptionRepository;
    @Autowired
    public ModelMapper modelMapper;
    @Autowired
    public ConsumptionCategoryRepository consumptionCategoryRepository;


    @Override
    public ConsumptionDto findById(Long id) {
        Optional<ConsumptionEntity> consumptionExist = consumptionRepository.findById(id);
        if (consumptionExist.isPresent()) {
            return modelMapper.map(consumptionExist.get(), ConsumptionDto.class);
        } else {
            throw new RuntimeException("Consumption with id " + id + " not found");
        }
    }

    @Override
    public List<ConsumptionDto> findAll() {
        List<ConsumptionEntity> consumptionEntities = consumptionRepository.findAll();
        return consumptionEntities.stream()
                .map(entity -> modelMapper.map(entity, ConsumptionDto.class))
                .toList();
    }

    @Override
    public List<ConsumptionDto> findAllByIsActive() {
        List<ConsumptionEntity> consumptionEntities = consumptionRepository.findByActiveIsTrue();
        return consumptionEntities.stream()
                .map(entity -> modelMapper.map(entity, ConsumptionDto.class))
                .toList();
    }

    @Override
    public ConsumptionDto createConsumption(CreateConsumptionDto createConsumptionDto) {
        ConsumptionCategoryEntity category = consumptionCategoryRepository.findById(createConsumptionDto.getCategoryId()).orElseThrow(() -> new RuntimeException("Category with id " + createConsumptionDto.getCategoryId() + " not found"));


        createConsumptionDto.setActive(true);
        createConsumptionDto.setCategoryId(category.getId());
        ConsumptionEntity consumptionEntity = modelMapper.map(createConsumptionDto, ConsumptionEntity.class);

        ConsumptionEntity savedConsumption = consumptionRepository.save(consumptionEntity);
        return modelMapper.map(savedConsumption, ConsumptionDto.class);
    }

    @Override
    public ConsumptionDto updateConsumption(Long id, CreateConsumptionDto dto) {
        Optional<ConsumptionEntity> consumptionExist = consumptionRepository.findById(id);
        if (consumptionExist.isPresent()) {
            ConsumptionEntity entity = modelMapper.map(dto, ConsumptionEntity.class);
            entity.setId(id);
            return modelMapper.map(consumptionRepository.save(entity), ConsumptionDto.class);
        }else{
            throw new RuntimeException("Consumption con id "+id+" no existe");
        }
    }

    @Override
    public void delete(Long id) {
        if(consumptionRepository.existsById(id)){
            consumptionRepository.deleteById(id);
        }else{
            throw new RuntimeException("Consumption con id "+id+" no encontrado");
        }
    }

    @Transactional
    @Override
    public ConsumptionDto deleteLogical(Long id) {
        Optional<ConsumptionEntity> consumptionExist = consumptionRepository.findById(id);
        if (consumptionExist.isPresent()) {
            ConsumptionEntity entity = consumptionExist.get();
            entity.setActive(false);
            ConsumptionEntity updatedEntity = consumptionRepository.save(entity);
            return modelMapper.map(updatedEntity, ConsumptionDto.class);
        } else {
            throw new RuntimeException("Consumption con id " + id + " no encontrado");
        }
    }
}
