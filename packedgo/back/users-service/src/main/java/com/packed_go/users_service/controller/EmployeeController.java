package com.packed_go.users_service.controller;

import com.packed_go.users_service.dto.EmployeeDTO.*;
import com.packed_go.users_service.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @Qualifier("eventServiceWebClient")
    private final WebClient eventServiceWebClient;

    @GetMapping("/assigned-events")
    public ResponseEntity<Map<String, Object>> getAssignedEvents(Authentication authentication) {
        try {
            Long employeeId = extractEmployeeId(authentication);
            List<AssignedEventInfo> events = employeeService.getAssignedEvents(employeeId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", events);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching assigned events", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/validate-ticket")
    public ResponseEntity<Map<String, Object>> validateTicket(
            @RequestBody ValidateTicketRequest request,
            Authentication authentication) {

        try {
            Long employeeId = extractEmployeeId(authentication);

            // Verificar que el empleado tiene acceso al evento
            if (!employeeService.hasAccessToEvent(employeeId, request.getEventId())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Unauthorized: You don't have access to this event"
                ));
            }

            // Llamar a event-service para validar el ticket
            log.info("🎫 Employee {} validating ticket for event {}", employeeId, request.getEventId());

            Map<String, Object> eventServiceRequest = new HashMap<>();
            eventServiceRequest.put("qrCode", request.getQrCode());
            eventServiceRequest.put("eventId", request.getEventId());

            ValidateTicketResponse ticketResponse = eventServiceWebClient.post()
                    .uri("/event-service/qr-validation/validate-entry")
                    .bodyValue(eventServiceRequest)
                    .retrieve()
                    .bodyToMono(ValidateTicketResponse.class)
                    .block();

            if (ticketResponse == null || !ticketResponse.getValid()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", ticketResponse != null ? ticketResponse.getMessage() : "Error al validar el ticket"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ticketResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating ticket", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/register-consumption")
    public ResponseEntity<Map<String, Object>> registerConsumption(
            @RequestBody RegisterConsumptionRequest request,
            Authentication authentication) {

        try {
            Long employeeId = extractEmployeeId(authentication);

            // Verificar que el empleado tiene acceso al evento
            if (!employeeService.hasAccessToEvent(employeeId, request.getEventId())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Unauthorized: You don't have access to this event"
                ));
            }

            // Llamar a event-service para validar y canjear la consumición
            log.info("🍺 Employee {} registering consumption for event {}", employeeId, request.getEventId());

            Map<String, Object> eventServiceRequest = new HashMap<>();
            eventServiceRequest.put("qrCode", request.getQrCode());
            eventServiceRequest.put("eventId", request.getEventId());
            eventServiceRequest.put("quantity", 1); // Por defecto canjear 1

            RegisterConsumptionResponse consumptionResponse = eventServiceWebClient.post()
                    .uri("/event-service/qr-validation/validate-consumption")
                    .bodyValue(eventServiceRequest)
                    .retrieve()
                    .bodyToMono(RegisterConsumptionResponse.class)
                    .block();

            if (consumptionResponse == null || !consumptionResponse.getSuccess()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", consumptionResponse != null ? consumptionResponse.getMessage() : "Error al registrar el consumo"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", consumptionResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error registering consumption", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        try {
            Long employeeId = extractEmployeeId(authentication);

            // TODO: Implementar estadísticas reales consultando tickets y consumos del día
            // Por ahora, mock response
            EmployeeStatsResponse stats = new EmployeeStatsResponse();
            stats.setTicketsScannedToday(15L);
            stats.setConsumptionsToday(23L);
            stats.setTotalScannedToday(38L);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    private Long extractEmployeeId(Authentication authentication) {
        // Extraer username del JWT token y buscar el empleado
        Map<String, Object> claims = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) claims.get("username");
        
        // Buscar el empleado por username para obtener su ID real en este servicio
        // Esto maneja el caso donde el ID del token (auth-service) no coincide con el ID local
        return employeeService.getEmployeeByUsername(username).getId();
    }
}
