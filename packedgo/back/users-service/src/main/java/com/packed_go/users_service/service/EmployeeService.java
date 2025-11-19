package com.packed_go.users_service.service;

import com.packed_go.users_service.dto.EmployeeDTO.*;
import com.packed_go.users_service.entity.Employee;
import com.packed_go.users_service.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Qualifier("eventServiceWebClient")
    private final WebClient eventServiceWebClient;

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request, Long adminId) {
        log.info("Creating employee with email: {} for admin: {}", request.getEmail(), adminId);

        // Validaciones
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (employeeRepository.existsByDocument(request.getDocument())) {
            throw new IllegalArgumentException("Document already exists");
        }

        if (request.getAssignedEventIds() == null || request.getAssignedEventIds().isEmpty()) {
            throw new IllegalArgumentException("At least one event must be assigned");
        }

        // Crear empleado
        Employee employee = new Employee();
        employee.setEmail(request.getEmail());
        employee.setUsername(request.getUsername());
        employee.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        employee.setDocument(request.getDocument());
        employee.setAdminId(adminId);
        employee.setIsActive(true);
        employee.setAssignedEventIds(request.getAssignedEventIds());

        employee = employeeRepository.save(employee);
        log.info("Employee created successfully with ID: {}", employee.getId());

        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long employeeId, UpdateEmployeeRequest request, Long adminId) {
        log.info("Updating employee ID: {} by admin: {}", employeeId, adminId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Verificar que el empleado pertenece al admin
        if (!employee.getAdminId().equals(adminId)) {
            throw new IllegalArgumentException("Unauthorized: Employee does not belong to this admin");
        }

        // Validar email único (si cambió)
        if (!employee.getEmail().equals(request.getEmail()) && 
            employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Validar documento único (si cambió)
        if (!employee.getDocument().equals(request.getDocument()) && 
            employeeRepository.existsByDocument(request.getDocument())) {
            throw new IllegalArgumentException("Document already exists");
        }

        if (request.getAssignedEventIds() == null || request.getAssignedEventIds().isEmpty()) {
            throw new IllegalArgumentException("At least one event must be assigned");
        }

        // Actualizar campos
        employee.setEmail(request.getEmail());
        employee.setUsername(request.getUsername());
        employee.setDocument(request.getDocument());
        employee.setAssignedEventIds(request.getAssignedEventIds());

        employee = employeeRepository.save(employee);
        log.info("Employee updated successfully");

        return mapToResponse(employee);
    }

    @Transactional
    public void toggleEmployeeStatus(Long employeeId, Long adminId) {
        log.info("Toggling status for employee ID: {} by admin: {}", employeeId, adminId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getAdminId().equals(adminId)) {
            throw new IllegalArgumentException("Unauthorized: Employee does not belong to this admin");
        }

        employee.setIsActive(!employee.getIsActive());
        employeeRepository.save(employee);

        log.info("Employee status changed to: {}", employee.getIsActive());
    }

    @Transactional
    public void deleteEmployee(Long employeeId, Long adminId) {
        log.info("Deleting employee ID: {} by admin: {}", employeeId, adminId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getAdminId().equals(adminId)) {
            throw new IllegalArgumentException("Unauthorized: Employee does not belong to this admin");
        }

        employeeRepository.delete(employee);
        log.info("Employee deleted successfully");
    }

    public List<EmployeeResponse> getEmployeesByAdmin(Long adminId) {
        log.info("Fetching employees for admin: {}", adminId);

        List<Employee> employees = employeeRepository.findByAdminId(adminId);
        
        return employees.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EmployeeResponse getEmployeeById(Long employeeId, Long adminId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getAdminId().equals(adminId)) {
            throw new IllegalArgumentException("Unauthorized: Employee does not belong to this admin");
        }

        return mapToResponse(employee);
    }

    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
    }

    public Employee getEmployeeByUsername(String username) {
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found with username: " + username));
    }

    public boolean validatePassword(Employee employee, String rawPassword) {
        return passwordEncoder.matches(rawPassword, employee.getPasswordHash());
    }

    public List<AssignedEventInfo> getAssignedEvents(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getIsActive()) {
            throw new IllegalArgumentException("Employee is not active");
        }

        Set<Long> eventIds = employee.getAssignedEventIds();
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Llamar al event-service para obtener información completa de los eventos
            log.info("Fetching events from event-service for employee {}: {}", employeeId, eventIds);

            List<EventDTO> events = eventServiceWebClient.post()
                    .uri("/event-service/event/by-ids")
                    .bodyValue(new ArrayList<>(eventIds))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<EventDTO>>() {})
                    .block();

            if (events == null) {
                log.warn("No events returned from event-service for employee {}", employeeId);
                return new ArrayList<>();
            }

            // Mapear EventDTO a AssignedEventInfo
            return events.stream()
                    .map(event -> {
                        AssignedEventInfo info = new AssignedEventInfo();
                        info.setId(event.getId());
                        info.setName(event.getName());
                        info.setLocation(event.getLocation());
                        info.setEventDate(event.getStartDate());
                        info.setStatus(event.isActive() ? "ACTIVE" : "INACTIVE");
                        return info;
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error fetching events from event-service for employee {}", employeeId, e);
            // En caso de error, retornar lista vacía en lugar de fallar completamente
            return new ArrayList<>();
        }
    }

    // DTO interno para mapear respuestas de event-service
    private static class EventDTO {
        private Long id;
        private String name;
        private String location;
        private LocalDateTime startDate;
        private boolean active;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public boolean hasAccessToEvent(Long employeeId, Long eventId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getIsActive()) {
            return false;
        }

        return employee.hasAccessToEvent(eventId);
    }

    /**
     * Método helper para obtener eventos asignados desde un Set de IDs (usado por mapToResponse)
     */
    private List<AssignedEventInfo> getAssignedEventsForMapping(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<EventDTO> events = eventServiceWebClient.post()
                .uri("/event-service/event/by-ids")
                .bodyValue(new ArrayList<>(eventIds))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<EventDTO>>() {})
                .block();

        if (events == null) {
            return new ArrayList<>();
        }

        return events.stream()
                .map(event -> {
                    AssignedEventInfo info = new AssignedEventInfo();
                    info.setId(event.getId());
                    info.setName(event.getName());
                    info.setLocation(event.getLocation());
                    info.setEventDate(event.getStartDate());
                    info.setStatus(event.isActive() ? "ACTIVE" : "INACTIVE");
                    return info;
                })
                .collect(Collectors.toList());
    }

    private EmployeeResponse mapToResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(employee.getId());
        response.setEmail(employee.getEmail());
        response.setUsername(employee.getUsername());
        response.setDocument(employee.getDocument());
        response.setAdminId(employee.getAdminId());
        response.setIsActive(employee.getIsActive());
        response.setCreatedAt(employee.getCreatedAt());
        response.setAssignedEventIds(employee.getAssignedEventIds());

        // Obtener información completa de eventos desde event-service
        try {
            List<AssignedEventInfo> eventInfos = getAssignedEventsForMapping(employee.getAssignedEventIds());
            response.setAssignedEvents(eventInfos);
        } catch (Exception e) {
            log.error("Error fetching event details for employee {}", employee.getId(), e);
            // En caso de error, usar solo los IDs
            List<AssignedEventInfo> fallbackEventInfos = employee.getAssignedEventIds().stream()
                    .map(eventId -> {
                        AssignedEventInfo info = new AssignedEventInfo();
                        info.setId(eventId);
                        info.setName("Event " + eventId);
                        return info;
                    })
                    .collect(Collectors.toList());
            response.setAssignedEvents(fallbackEventInfos);
        }

        return response;
    }
}
