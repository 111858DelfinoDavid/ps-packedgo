package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.eventCategory.CreateEventCategoryDTO;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDTO;
import com.packed_go.event_service.services.EventCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event-service/category")
@RequiredArgsConstructor
@Slf4j
public class EventCategoryController {
    private final EventCategoryService eventCategoryService;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<EventCategoryDTO> create(@RequestBody CreateEventCategoryDTO dto) {
        EventCategoryDTO created = eventCategoryService.create(dto);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventCategoryDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventCategoryService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<EventCategoryDTO>> getAll() {
        return ResponseEntity.ok(eventCategoryService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<EventCategoryDTO>> getAllActive() {
        return ResponseEntity.ok(eventCategoryService.findByActiveIsTrue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventCategoryDTO> update(@PathVariable Long id, @RequestBody CreateEventCategoryDTO dto) {
        return ResponseEntity.ok(eventCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logical/{id}")
    public ResponseEntity<EventCategoryDTO> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(eventCategoryService.deleteLogical(id), EventCategoryDTO.class));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<EventCategoryDTO> updateStatus(@PathVariable Long id) {
        return ResponseEntity.ok(eventCategoryService.updateStatus(id));
    }
}
