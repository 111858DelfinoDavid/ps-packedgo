package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.consumptionCategory.ConsumptionCategoryDTO;
import com.packed_go.event_service.dtos.consumptionCategory.CreateConsumptionCategoryDTO;
import com.packed_go.event_service.services.ConsumptionCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event-service/consumption-category")
@RequiredArgsConstructor
@Slf4j
public class ConsumptionCategoryController {
    private final ConsumptionCategoryService consumptionCategoryService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateConsumptionCategoryDTO dto) {
        try {
            ConsumptionCategoryDTO created = consumptionCategoryService.create(dto);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(consumptionCategoryService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<ConsumptionCategoryDTO>> getAll() {
        return ResponseEntity.ok(consumptionCategoryService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ConsumptionCategoryDTO>> getAllActive() {
        return ResponseEntity.ok(consumptionCategoryService.findByActiveIsTrue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> update(@PathVariable Long id, @RequestBody CreateConsumptionCategoryDTO dto) {
        return ResponseEntity.ok(consumptionCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        consumptionCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logical/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(consumptionCategoryService.deleteLogical(id), ConsumptionCategoryDTO.class));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<ConsumptionCategoryDTO> updateStatus(@PathVariable Long id) {
        return ResponseEntity.ok(consumptionCategoryService.updateStatus(id));
    }
}
