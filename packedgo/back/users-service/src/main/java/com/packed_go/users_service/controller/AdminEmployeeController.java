package com.packed_go.users_service.controller;

import com.packed_go.users_service.dto.EmployeeDTO.*;
import com.packed_go.users_service.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/employees")
@RequiredArgsConstructor
@Slf4j
public class AdminEmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createEmployee(
            @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {
        
        try {
            Long adminId = extractAdminId(authentication);
            EmployeeResponse employee = employeeService.createEmployee(request, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee created successfully");
            response.put("data", employee);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Error creating employee: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error creating employee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getEmployees(Authentication authentication) {
        try {
            Long adminId = extractAdminId(authentication);
            List<EmployeeResponse> employees = employeeService.getEmployeesByAdmin(adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", employees);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching employees", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getEmployeeById(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            Long adminId = extractAdminId(authentication);
            EmployeeResponse employee = employeeService.getEmployeeById(id, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", employee);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error fetching employee: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error fetching employee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateEmployee(
            @PathVariable Long id,
            @RequestBody UpdateEmployeeRequest request,
            Authentication authentication) {
        
        try {
            Long adminId = extractAdminId(authentication);
            EmployeeResponse employee = employeeService.updateEmployee(id, request, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee updated successfully");
            response.put("data", employee);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error updating employee: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error updating employee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<Map<String, Object>> toggleEmployeeStatus(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            Long adminId = extractAdminId(authentication);
            employeeService.toggleEmployeeStatus(id, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee status toggled successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error toggling employee status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error toggling employee status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteEmployee(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            Long adminId = extractAdminId(authentication);
            employeeService.deleteEmployee(id, adminId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee deleted successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error deleting employee: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error deleting employee", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Internal server error"
            ));
        }
    }

    private Long extractAdminId(Authentication authentication) {
        // Extraer adminId del JWT token
        // El JWT debe contener el userId del admin
        Map<String, Object> claims = (Map<String, Object>) authentication.getPrincipal();
        return Long.valueOf(claims.get("userId").toString());
    }
}
