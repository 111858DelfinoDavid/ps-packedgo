package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.consumptionCategory.ConsumptionCategoryDto;
import com.packed_go.event_service.dtos.consumptionCategory.CreateConsumptionCategoryDto;
import com.packed_go.event_service.services.ConsumptionCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-service/consumption-category")
@RequiredArgsConstructor
@Slf4j
public class ConsumptionCategoryController {
    private final ConsumptionCategoryService consumptionCategoryService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<ConsumptionCategoryDto> create(@RequestBody CreateConsumptionCategoryDto dto) {
        ConsumptionCategoryDto created = consumptionCategoryService.create(dto);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsumptionCategoryDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(consumptionCategoryService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<ConsumptionCategoryDto>> getAll() {
        return ResponseEntity.ok(consumptionCategoryService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<ConsumptionCategoryDto>> getAllActive() {
        return ResponseEntity.ok(consumptionCategoryService.findByActiveIsTrue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsumptionCategoryDto> update(@PathVariable Long id, @RequestBody CreateConsumptionCategoryDto dto) {
        return ResponseEntity.ok(consumptionCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        consumptionCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logical/{id}")
    public ResponseEntity<ConsumptionCategoryDto> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(consumptionCategoryService.deleteLogical(id), ConsumptionCategoryDto.class));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<ConsumptionCategoryDto> updateStatus(@PathVariable Long id) {
        return ResponseEntity.ok(consumptionCategoryService.updateStatus(id));
    }
}
