package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.event.CreateEventDto;
import com.packed_go.event_service.dtos.event.EventDto;
import com.packed_go.event_service.services.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/event-service")
@RequiredArgsConstructor
@Slf4j
public class EventController {
    private final EventService service;
    private final ModelMapper modelMapper;

    @PostMapping
    public ResponseEntity<EventDto> create(@RequestBody CreateEventDto dto){
        EventDto created=service.createEvent(dto);
        if(created!=null){
            return ResponseEntity.ok(created);
        }else{
            return ResponseEntity.status(409).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }


}
