package com.packed_go.users_service.service;

import com.packed_go.users_service.dto.EmployeeDTO.*;
import com.packed_go.users_service.entity.Employee;
import com.packed_go.users_service.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate;

    // URL del event-service (configurar en application.properties)
    private static final String EVENT_SERVICE_URL = "http://localhost:8083/api/events";

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

    public boolean validatePassword(Employee employee, String rawPassword) {
        return passwordEncoder.matches(rawPassword, employee.getPasswordHash());
    }

    public List<AssignedEventInfo> getAssignedEvents(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getIsActive()) {
            throw new IllegalArgumentException("Employee is not active");
        }

        // TODO: Llamar al event-service para obtener información completa de los eventos
        // Por ahora retornamos solo los IDs
        return employee.getAssignedEventIds().stream()
                .map(eventId -> {
                    AssignedEventInfo eventInfo = new AssignedEventInfo();
                    eventInfo.setId(eventId);
                    eventInfo.setName("Event " + eventId); // Placeholder
                    return eventInfo;
                })
                .collect(Collectors.toList());
    }

    public boolean hasAccessToEvent(Long employeeId, Long eventId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        if (!employee.getIsActive()) {
            return false;
        }

        return employee.hasAccessToEvent(eventId);
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

        // TODO: Enriquecer con información completa de eventos desde event-service
        List<AssignedEventInfo> eventInfos = employee.getAssignedEventIds().stream()
                .map(eventId -> {
                    AssignedEventInfo info = new AssignedEventInfo();
                    info.setId(eventId);
                    info.setName("Event " + eventId); // Placeholder
                    return info;
                })
                .collect(Collectors.toList());
        
        response.setAssignedEvents(eventInfos);

        return response;
    }
}
