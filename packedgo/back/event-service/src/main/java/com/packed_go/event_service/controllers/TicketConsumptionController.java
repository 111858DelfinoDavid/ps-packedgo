package com.packed_go.event_service.controllers;

import com.packed_go.event_service.dtos.ticketConsumption.TicketConsumptionDTO;
import com.packed_go.event_service.services.TicketConsumptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-service/ticket-consumption")
@RequiredArgsConstructor
@Slf4j
public class TicketConsumptionController {
    private final TicketConsumptionService ticketService;

    @PostMapping
    public ResponseEntity<TicketConsumptionDTO> createTicket(@RequestBody TicketConsumptionDTO dto) {
        TicketConsumptionDTO savedTicket = ticketService.create(dto);
        return ResponseEntity.ok(savedTicket);
    }
}
