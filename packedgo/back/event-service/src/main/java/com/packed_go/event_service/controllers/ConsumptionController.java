package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.consumption.ConsumptionDto;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDto;
import com.packed_go.event_service.services.ConsumptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-service/consumption")
@RequiredArgsConstructor
@Slf4j
public class ConsumptionController {
    private final ConsumptionService service;
    private final ModelMapper modelMapper;


    @GetMapping("/{id}")
    public ResponseEntity<ConsumptionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }


    @GetMapping
    public ResponseEntity<List<ConsumptionDto>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<ConsumptionDto> create(@RequestBody CreateConsumptionDto dto) {
        ConsumptionDto created = service.createConsumption(dto);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.status(409).build();
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<ConsumptionDto> update(@PathVariable Long id, @RequestBody CreateConsumptionDto dto) {
        return ResponseEntity.ok(service.updateConsumption(id, dto));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logical/{id}")
    public ResponseEntity<ConsumptionDto> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(service.deleteLogical(id), ConsumptionDto.class));
    }
}
