package com.packed_go.auth_service.controllers;

import com.packed_go.auth_service.dto.request.*;
import com.packed_go.auth_service.dto.response.ApiResponse;
import com.packed_go.auth_service.dto.response.LoginResponse;
import com.packed_go.auth_service.dto.response.TokenValidationResponse;
import com.packed_go.auth_service.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // En producciï¿½n, configurar especï¿½ficamente
public class AuthController {

    private final AuthService authService;

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest) {
        
        //log.info("Admin login attempt for email: {}", request.getEmail());
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.loginAdmin(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Admin login successful"));
    }

    @PostMapping("/customer/login")
    public ResponseEntity<ApiResponse<LoginResponse>> customerLogin(
            @Valid @RequestBody CustomerLoginRequest request,
            HttpServletRequest httpRequest) {
        
        //log.info("Customer login attempt for document: {}", request.getDocument());
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authService.loginCustomer(request, ipAddress, userAgent);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Customer login successful"));
    }

    @PostMapping("/customer/register")
    public ResponseEntity<ApiResponse<String>> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        
        log.info("Customer registration attempt for document: {}", request.getDocument());
        
        authService.registerCustomer(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered successfully. Please verify your email."));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<ApiResponse<String>> registerAdmin(
            @Valid @RequestBody AdminRegistrationRequest request) {
        
        log.info("Admin registration attempt for email: {}", request.getEmail());
        
        authService.registerAdmin(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admin registration request received. Awaiting approval."));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @Valid @RequestBody TokenValidationRequest request) {
        
        log.debug("Token validation request");
        
        TokenValidationResponse response = authService.validateToken(request.getToken());
        
        return ResponseEntity.ok(ApiResponse.success(response, "Token validation completed"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @Valid @RequestBody LogoutRequest request) {
        
        log.info("Logout request");
        
        authService.logout(request.getToken());
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @RequestParam("token") String token) {
        
        log.info("Email verification request for token: {}", token.substring(0, Math.min(token.length(), 8)) + "...");
        
        boolean verified = authService.verifyEmail(token);
        
        if (verified) {
            return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid or expired verification token"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(
            @RequestBody String refreshToken) {
        
        log.info("Token refresh request");
        
        String newToken = authService.refreshToken(refreshToken);
        
        return ResponseEntity.ok(ApiResponse.success(newToken, "Token refreshed successfully"));
    }

    // Mï¿½todos de utilidad
    private String getClientIpAddress(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}