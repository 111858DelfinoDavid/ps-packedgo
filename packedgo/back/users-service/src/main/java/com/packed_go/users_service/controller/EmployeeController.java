package com.packed_go.users_service.controller;

import com.packed_go.users_service.dto.EmployeeDTO.*;
import com.packed_go.users_service.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

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

            // TODO: Implementar validación real del ticket con event-service o ticket-service
            // Por ahora, mock response
            ValidateTicketResponse ticketResponse = new ValidateTicketResponse();
            ticketResponse.setValid(true);
            ticketResponse.setMessage("Ticket válido - Entrada autorizada");

            TicketInfo ticketInfo = new TicketInfo();
            ticketInfo.setTicketId(1L);
            ticketInfo.setTicketType("VIP");
            ticketInfo.setCustomerName("Juan Pérez");
            ticketInfo.setEventName("Evento Test");
            ticketInfo.setAlreadyUsed(false);
            
            ticketResponse.setTicketInfo(ticketInfo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", ticketResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating ticket", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
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

            // TODO: Implementar registro real del consumo con consumption-service
            // Por ahora, mock response
            RegisterConsumptionResponse consumptionResponse = new RegisterConsumptionResponse();
            consumptionResponse.setSuccess(true);
            consumptionResponse.setMessage("Consumo registrado correctamente");

            ConsumptionInfo consumptionInfo = new ConsumptionInfo();
            consumptionInfo.setConsumptionId(1L);
            consumptionInfo.setConsumptionType("Bebida");
            consumptionInfo.setCustomerName("María Gómez");
            consumptionInfo.setEventName("Evento Test");
            consumptionInfo.setRegisteredAt(java.time.LocalDateTime.now());
            
            consumptionResponse.setConsumptionInfo(consumptionInfo);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", consumptionResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error registering consumption", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
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
        // Extraer employeeId del JWT token
        Map<String, Object> claims = (Map<String, Object>) authentication.getPrincipal();
        return Long.valueOf(claims.get("userId").toString());
    }
}
