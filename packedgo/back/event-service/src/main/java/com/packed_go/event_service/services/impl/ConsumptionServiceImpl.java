package com.packed_go.event_service.services.impl;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.ConsumptionCategory;
import com.packed_go.event_service.repositories.ConsumptionCategoryRepository;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.services.ConsumptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsumptionServiceImpl implements ConsumptionService {

    private final ConsumptionRepository consumptionRepository;
    private final ConsumptionCategoryRepository consumptionCategoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public ConsumptionDTO findById(Long id) {
        Consumption consumption = consumptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consumption with id " + id + " not found"));
        return modelMapper.map(consumption, ConsumptionDTO.class);
    }

    @Override
    public List<ConsumptionDTO> findAll() {
        return consumptionRepository.findAll().stream()
                .map(entity -> modelMapper.map(entity, ConsumptionDTO.class))
                .toList();
    }

    @Override
    public List<ConsumptionDTO> findAllByIsActive() {
        return consumptionRepository.findByActiveIsTrue().stream()
                .map(entity -> modelMapper.map(entity, ConsumptionDTO.class))
                .toList();
    }

    @Override
    public ConsumptionDTO createConsumption(CreateConsumptionDTO createConsumptionDto) {
        // Validar categoría
        ConsumptionCategory category = consumptionCategoryRepository.findById(createConsumptionDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category with id " + createConsumptionDto.getCategoryId() + " not found"));

        // Mapear DTO a entidad
        Consumption consumption = modelMapper.map(createConsumptionDto, Consumption.class);
        consumption.setCategory(category);
        consumption.setActive(true); // siempre activo al crear
        Consumption savedConsumption = consumptionRepository.save(consumption);
        ConsumptionDTO consumptionDTO=modelMapper.map(savedConsumption,ConsumptionDTO.class);
        consumptionDTO.setCategoryId(savedConsumption.getCategory().getId());
        return consumptionDTO;
    }

    @Override
    public ConsumptionDTO updateConsumption(Long id, CreateConsumptionDTO dto) {
        Consumption existingConsumption = consumptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consumption with id " + id + " not found"));

        // Validar categoría si se proporciona
        if (dto.getCategoryId() != null) {
            ConsumptionCategory category = consumptionCategoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category with id " + dto.getCategoryId() + " not found"));
            existingConsumption.setCategory(category);
        }

        // Mapear campos actualizables del DTO a la entidad existente
        modelMapper.map(dto, existingConsumption);

        Consumption updatedConsumption = consumptionRepository.save(existingConsumption);
        return modelMapper.map(updatedConsumption, ConsumptionDTO.class);
    }

    @Override
    public void delete(Long id) {
        if (!consumptionRepository.existsById(id)) {
            throw new RuntimeException("Consumption with id " + id + " not found");
        }
        consumptionRepository.deleteById(id);
    }

    @Transactional
    @Override
    public ConsumptionDTO deleteLogical(Long id) {
        Consumption existingConsumption = consumptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consumption with id " + id + " not found"));

        existingConsumption.setActive(false);
        Consumption updatedConsumption = consumptionRepository.save(existingConsumption);

        return modelMapper.map(updatedConsumption, ConsumptionDTO.class);
    }
}
