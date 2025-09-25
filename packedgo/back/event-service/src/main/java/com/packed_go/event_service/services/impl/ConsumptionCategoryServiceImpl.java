package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.consumptionCategory.ConsumptionCategoryDto;
import com.packed_go.event_service.dtos.consumptionCategory.CreateConsumptionCategoryDto;
import com.packed_go.event_service.entities.ConsumptionCategoryEntity;
import com.packed_go.event_service.repositories.ConsumptionCategoryRepository;
import com.packed_go.event_service.services.ConsumptionCategoryService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ConsumptionCategoryServiceImpl implements ConsumptionCategoryService {


    @Autowired
    private final ConsumptionCategoryRepository consumptionCategoryRepository;
    @Autowired
    private final ModelMapper modelMapper;

    @Override
    public ConsumptionCategoryDto findById(Long id) {
        Optional<ConsumptionCategoryEntity> consumptionExist = consumptionCategoryRepository.findById(id);
        if (consumptionExist.isPresent()) {
            return modelMapper.map(consumptionExist.get(), ConsumptionCategoryDto.class);
        } else {
            throw new RuntimeException("ConsumptionCategory with id " + id + " not found");
        }
    }

    @Override
    public List<ConsumptionCategoryDto> findByActiveIsTrue() {
        List<ConsumptionCategoryEntity> categoryEntities = consumptionCategoryRepository.findByActiveIsTrue();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, ConsumptionCategoryDto.class))
                .toList();
    }

    @Override
    public List<ConsumptionCategoryDto> findAll() {
        List<ConsumptionCategoryEntity> categoryEntities = consumptionCategoryRepository.findAll();
        return categoryEntities.stream()
                .map(entity -> modelMapper.map(entity, ConsumptionCategoryDto.class))
                .toList();
    }

    @Override
    public ConsumptionCategoryDto create(CreateConsumptionCategoryDto createConsumptionCategoryDto) {
        Optional<ConsumptionCategoryEntity> categoryExist = consumptionCategoryRepository.findByName(createConsumptionCategoryDto.getName());
        if (categoryExist.isPresent()) {
            throw new RuntimeException("Consumption category ya existe");
        } else {
            ConsumptionCategoryEntity entity = modelMapper.map(createConsumptionCategoryDto, ConsumptionCategoryEntity.class);
            entity.setActive(true);
            return modelMapper.map(consumptionCategoryRepository.save(entity), ConsumptionCategoryDto.class);
        }
    }

    @Override
    public ConsumptionCategoryDto update(Long id, CreateConsumptionCategoryDto createConsumptionCategoryDto) {
        Optional<ConsumptionCategoryEntity> categoryExist = consumptionCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            ConsumptionCategoryEntity entity = modelMapper.map(createConsumptionCategoryDto, ConsumptionCategoryEntity.class);
            entity.setId(id);
            return modelMapper.map(consumptionCategoryRepository.save(entity), ConsumptionCategoryDto.class);
        } else {
            throw new RuntimeException("Consumption category con id " + id + " no encontrado");
        }
    }

    @Override
    public void delete(Long id) {
        if (consumptionCategoryRepository.existsById(id)) {
            consumptionCategoryRepository.deleteById(id);
        } else {
            throw new RuntimeException("Consumption category category con id " + id + " no encontrado");
        }
    }

    @Override
    public ConsumptionCategoryDto deleteLogical(Long id) {
        Optional<ConsumptionCategoryEntity> categoryExist = consumptionCategoryRepository.findById(id);
        if (categoryExist.isPresent()) {
            ConsumptionCategoryEntity entity = categoryExist.get();
            entity.setActive(false);
            return modelMapper.map(consumptionCategoryRepository.save(entity), ConsumptionCategoryDto.class);

        } else {
            throw new RuntimeException("Event category con id " + id + " no encontrado");
        }
    }

    @Override
    public ConsumptionCategoryDto updateStatus(Long id) {
        ConsumptionCategoryEntity entity = consumptionCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consumption category not found with id: " + id));
        entity.setActive(!entity.isActive());
        ConsumptionCategoryEntity updatedEntity = consumptionCategoryRepository.save(entity);
        return modelMapper.map(updatedEntity, ConsumptionCategoryDto.class);
    }
}
