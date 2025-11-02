package com.packed_go.consumption_service.controllers;
import com.packed_go.consumption_service.dtos.*;
import com.packed_go.consumption_service.services.*;
import jakarta.validation.Valid;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/consumption/tickets")
@RequiredArgsConstructor
@Slf4j
public class TicketController {
    private final TicketGenerationService ticketGenerationService;
    private final TicketValidationService ticketValidationService;
    @PostMapping("/generate")
    public ResponseEntity<GenerateTicketsResponse> generateTickets(@RequestParam Long orderId) {
        GenerateTicketsResponse response = ticketGenerationService.generateTicketsForOrder(orderId);
        return response.isSuccess() ? ResponseEntity.status(HttpStatus.CREATED).body(response) : ResponseEntity.badRequest().body(response);
    }
    @PostMapping("/validate-entry")
    public ResponseEntity<EntryValidationResponse> validateEntry(@Valid @RequestBody ValidateEntryRequest request) {
        EntryValidationResponse response = ticketValidationService.validateEntry(request);
        return response.isValid() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    @PostMapping("/validate-consumption")
    public ResponseEntity<ConsumptionValidationResponse> validateConsumption(@Valid @RequestBody ValidateConsumptionRequest request) {
        ConsumptionValidationResponse response = ticketValidationService.validateConsumption(request);
        return response.isValid() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Consumption Service UP");
    }
}