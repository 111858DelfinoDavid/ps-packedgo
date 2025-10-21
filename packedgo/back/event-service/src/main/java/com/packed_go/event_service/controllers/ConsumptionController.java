package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.consumption.ConsumptionDTO;
import com.packed_go.event_service.dtos.consumption.CreateConsumptionDTO;
import com.packed_go.event_service.services.ConsumptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/event-service/consumption")
@RequiredArgsConstructor
@Slf4j
public class ConsumptionController {
    private final ConsumptionService service;
    private final ModelMapper modelMapper;


    @GetMapping("/{id}")
    public ResponseEntity<ConsumptionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }


    @GetMapping
    public ResponseEntity<List<ConsumptionDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<ConsumptionDTO> create(@RequestBody CreateConsumptionDTO dto) {
        ConsumptionDTO created = service.createConsumption(dto);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.status(409).build();
        }
    }


    @PutMapping("/{id}")
    public ResponseEntity<ConsumptionDTO> update(@PathVariable Long id, @RequestBody CreateConsumptionDTO dto) {
        return ResponseEntity.ok(service.updateConsumption(id, dto));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logical/{id}")
    public ResponseEntity<ConsumptionDTO> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(service.deleteLogical(id), ConsumptionDTO.class));
    }
}
