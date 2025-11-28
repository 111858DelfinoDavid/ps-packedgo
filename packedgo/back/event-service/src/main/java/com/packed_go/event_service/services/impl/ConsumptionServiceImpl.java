package com.packed_go.event_service.services.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDTO;
import com.packed_go.event_service.entities.Consumption;
import com.packed_go.event_service.entities.ConsumptionCategory;
import com.packed_go.event_service.repositories.ConsumptionCategoryRepository;
import com.packed_go.event_service.repositories.ConsumptionRepository;
import com.packed_go.event_service.services.ConsumptionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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
        return mapToDTO(consumption);
    }
    
    /**
     * 🔒 NUEVO: Busca una consumición validando ownership multi-tenant
     */
    public ConsumptionDTO findByIdAndCreatedBy(Long id, Long createdBy) {
        Consumption consumption = consumptionRepository.findByIdAndCreatedBy(id, createdBy)
                .orElseThrow(() -> new RuntimeException("Consumption not found or unauthorized"));
        return mapToDTO(consumption);
    }

    @Override
    public List<ConsumptionDTO> findAll() {
        // ⚠️ NOTA: Este método retorna TODAS las consumiciones sin filtrar
        // Se mantiene para compatibilidad pero debería usarse findByCreatedBy() en su lugar
        return consumptionRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * 🔒 NUEVO: Busca todas las consumiciones del usuario autenticado
     */
    public List<ConsumptionDTO> findByCreatedBy(Long createdBy) {
        return consumptionRepository.findByCreatedBy(createdBy).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public List<ConsumptionDTO> findAllByIsActive() {
        // ⚠️ NOTA: Este método retorna TODAS las consumiciones activas sin filtrar
        // Se mantiene para compatibilidad pero debería usarse findByCreatedByAndActive() en su lugar
        return consumptionRepository.findByActiveIsTrue().stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * 🔒 NUEVO: Busca consumiciones activas del usuario autenticado
     */
    public List<ConsumptionDTO> findByCreatedByAndActive(Long createdBy) {
        return consumptionRepository.findByCreatedByAndActiveIsTrue(createdBy).stream()
                .map(this::mapToDTO)
                .toList();
    }
    
    /**
     * Helper method para mapear Consumption entity a DTO con categoryId correcto
     */
    private ConsumptionDTO mapToDTO(Consumption consumption) {
        ConsumptionDTO dto = modelMapper.map(consumption, ConsumptionDTO.class);
        if (consumption.getCategory() != null) {
            dto.setCategoryId(consumption.getCategory().getId());
        }
        return dto;
    }

    @Override
    public ConsumptionDTO createConsumption(CreateConsumptionDTO createConsumptionDto) {
        // ⚠️ DEPRECADO: Este método no valida ownership multi-tenant
        // Usar createConsumption(CreateConsumptionDTO, Long createdBy) en su lugar
        
        // Validar categoría
        ConsumptionCategory category = consumptionCategoryRepository.findById(createConsumptionDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category with id " + createConsumptionDto.getCategoryId() + " not found"));

        // Mapear DTO a entidad
        Consumption consumption = modelMapper.map(createConsumptionDto, Consumption.class);
        consumption.setCategory(category);
        consumption.setActive(true); // siempre activo al crear
        Consumption savedConsumption = consumptionRepository.save(consumption);
        return mapToDTO(savedConsumption);
    }
    
    /**
     * 🔒 NUEVO: Crea una consumición con validación multi-tenant
     */
    public ConsumptionDTO createConsumption(CreateConsumptionDTO createConsumptionDto, Long createdBy) {
        // Validar categoría
        ConsumptionCategory category = consumptionCategoryRepository.findById(createConsumptionDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category with id " + createConsumptionDto.getCategoryId() + " not found"));

        // Mapear DTO a entidad
        Consumption consumption = modelMapper.map(createConsumptionDto, Consumption.class);
        consumption.setCategory(category);
        consumption.setCreatedBy(createdBy); // 🔒 Inyectar desde JWT
        consumption.setActive(true);
        
        Consumption savedConsumption = consumptionRepository.save(consumption);
        return mapToDTO(savedConsumption);
    }

    @Override
    public ConsumptionDTO updateConsumption(Long id, CreateConsumptionDTO dto) {
        // ⚠️ DEPRECADO: Este método no valida ownership multi-tenant
        // Usar updateConsumption(Long id, CreateConsumptionDTO dto, Long createdBy) en su lugar
        
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
        return mapToDTO(updatedConsumption);
    }
    
    /**
     * 🔒 NUEVO: Actualiza una consumición validando ownership
     */
    public ConsumptionDTO updateConsumption(Long id, CreateConsumptionDTO dto, Long createdBy) {
        // 🔒 Validar que la consumición pertenece al usuario
        Consumption existingConsumption = consumptionRepository.findByIdAndCreatedBy(id, createdBy)
                .orElseThrow(() -> new RuntimeException("Consumption not found or unauthorized"));

        // Validar categoría si se proporciona
        if (dto.getCategoryId() != null) {
            ConsumptionCategory category = consumptionCategoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category with id " + dto.getCategoryId() + " not found"));
            existingConsumption.setCategory(category);
        }

        // Mapear campos actualizables (excepto createdBy que no debe cambiar)
        existingConsumption.setName(dto.getName());
        existingConsumption.setDescription(dto.getDescription());
        existingConsumption.setPrice(dto.getPrice());
        existingConsumption.setImageUrl(dto.getImageUrl());
        existingConsumption.setActive(dto.isActive());

        Consumption updatedConsumption = consumptionRepository.save(existingConsumption);
        return mapToDTO(updatedConsumption);
    }

    @Override
    public void delete(Long id) {
        // ⚠️ DEPRECADO: Este método no valida ownership multi-tenant
        // Usar delete(Long id, Long createdBy) en su lugar
        
        if (!consumptionRepository.existsById(id)) {
            throw new RuntimeException("Consumption with id " + id + " not found");
        }
        consumptionRepository.deleteById(id);
    }
    
    /**
     * 🔒 NUEVO: Elimina una consumición validando ownership
     */
    public void delete(Long id, Long createdBy) {
        // 🔒 Validar que la consumición pertenece al usuario
        Consumption consumption = consumptionRepository.findByIdAndCreatedBy(id, createdBy)
                .orElseThrow(() -> new RuntimeException("Consumption not found or unauthorized"));
        
        consumptionRepository.delete(consumption);
    }

    @Transactional
    @Override
    public ConsumptionDTO deleteLogical(Long id) {
        // ⚠️ DEPRECADO: Este método no valida ownership multi-tenant
        // Usar deleteLogical(Long id, Long createdBy) en su lugar
        
        Consumption existingConsumption = consumptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consumption with id " + id + " not found"));

        existingConsumption.setActive(false);
        Consumption updatedConsumption = consumptionRepository.save(existingConsumption);

        return mapToDTO(updatedConsumption);
    }
    
    /**
     * 🔒 NUEVO: Desactiva una consumición validando ownership
     */
    @Transactional
    public ConsumptionDTO deleteLogical(Long id, Long createdBy) {
        // 🔒 Validar que la consumición pertenece al usuario
        Consumption existingConsumption = consumptionRepository.findByIdAndCreatedBy(id, createdBy)
                .orElseThrow(() -> new RuntimeException("Consumption not found or unauthorized"));

        existingConsumption.setActive(false);
        Consumption updatedConsumption = consumptionRepository.save(existingConsumption);

        return mapToDTO(updatedConsumption);
    }
}
