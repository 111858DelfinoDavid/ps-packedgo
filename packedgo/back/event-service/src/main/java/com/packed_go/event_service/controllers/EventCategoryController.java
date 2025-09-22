package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.eventCategory.CreateEventCategoryDto;
import com.packed_go.event_service.dtos.eventCategory.EventCategoryDto;
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
    public ResponseEntity<EventCategoryDto> create(@RequestBody CreateEventCategoryDto dto) {
        EventCategoryDto created = eventCategoryService.create(dto);
        if (created != null) {
            return ResponseEntity.ok(created);
        } else {
            return ResponseEntity.status(409).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventCategoryDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventCategoryService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<EventCategoryDto>> getAll() {
        return ResponseEntity.ok(eventCategoryService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<EventCategoryDto>> getAllActive() {
        return ResponseEntity.ok(eventCategoryService.findByActiveIsTrue());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventCategoryDto> update(@PathVariable Long id, @RequestBody CreateEventCategoryDto dto) {
        return ResponseEntity.ok(eventCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/logical/{id}")
    public ResponseEntity<EventCategoryDto> deleteLogical(@PathVariable Long id) {
        return ResponseEntity.ok(modelMapper.map(eventCategoryService.deleteLogical(id), EventCategoryDto.class));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<EventCategoryDto> updateStatus(@PathVariable Long id) {
        return ResponseEntity.ok(eventCategoryService.updateStatus(id));
    }
}
