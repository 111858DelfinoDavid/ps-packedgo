package com.packed_go.auth_service.services.impl;

import com.packed_go.auth_service.dto.request.AdminLoginRequest;
import com.packed_go.auth_service.dto.request.AdminRegistrationRequest;
import com.packed_go.auth_service.dto.request.CreateProfileFromAuthRequest;
import com.packed_go.auth_service.dto.request.CustomerLoginRequest;
import com.packed_go.auth_service.dto.request.CustomerRegistrationRequest;
import com.packed_go.auth_service.dto.response.LoginResponse;
import com.packed_go.auth_service.dto.response.TokenValidationResponse;
import com.packed_go.auth_service.entities.*;
import com.packed_go.auth_service.exceptions.BadRequestException;
import com.packed_go.auth_service.exceptions.ResourceNotFoundException;
import com.packed_go.auth_service.exceptions.UnauthorizedException;
import com.packed_go.auth_service.repositories.*;
import com.packed_go.auth_service.security.JwtTokenProvider;
import com.packed_go.auth_service.services.AuthService;
import com.packed_go.auth_service.services.UsersServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthUserRepository authUserRepository;
    private final UserSessionRepository userSessionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    // TODO: Agregar cuando se implemente recuperaci�n de contrase�as y verificaci�n de email
    // private final PasswordRecoveryTokenRepository passwordRecoveryTokenRepository;
    // private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UsersServiceClient usersServiceClient;
    // TODO: Agregar cuando sea necesario mapear DTOs complejos
    // private final ModelMapper modelMapper;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_MINUTES = 30;

    @Override
    public LoginResponse loginAdmin(AdminLoginRequest request, String ipAddress, String userAgent) {
        //log.debug("Admin login attempt for email: {}", request.getEmail());
        
        // Buscar usuario por email y tipo de login ADMIN
        AuthUser user = authUserRepository.findByEmailAndLoginType(request.getEmail(), "EMAIL")
            .orElseThrow(() -> {
                recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "User not found");
                return new UnauthorizedException("Invalid credentials");
            });

        // Verificar si es admin
        if (!"ADMIN".equals(user.getRole()) && !"SUPER_ADMIN".equals(user.getRole())) {
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Not an admin");
            throw new UnauthorizedException("Access denied");
        }

        // Verificar si la cuenta est� bloqueada
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            recordFailedLogin(request.getEmail(), "EMAIL", ipAddress, userAgent, "Account locked");
            throw new UnauthorizedException("Account is locked until " + user.getLockedUntil());
        }

        // Verificar contrase�a
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, request.getEmail(), "EMAIL", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Login exitoso
        return processSuccessfulLogin(user, ipAddress, userAgent);
    }

    @Override
    public LoginResponse loginCustomer(CustomerLoginRequest request, String ipAddress, String userAgent) {
        log.debug("Customer login attempt for document: {}", request.getDocument());
        
        // Buscar usuario por documento y tipo de login CUSTOMER
        AuthUser user = authUserRepository.findByDocumentAndLoginType(request.getDocument(), "DOCUMENT")
            .orElseThrow(() -> {
                recordFailedLogin(String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent, "User not found");
                return new UnauthorizedException("Invalid credentials");
            });

        // Verificar si es cliente
        if (!"CUSTOMER".equals(user.getRole())) {
            recordFailedLogin(String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent, "Not a customer");
            throw new UnauthorizedException("Access denied");
        }

        // Verificar si la cuenta est� bloqueada
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            recordFailedLogin(String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent, "Account locked");
            throw new UnauthorizedException("Account is locked until " + user.getLockedUntil());
        }

        // Verificar contrase�a
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user, String.valueOf(request.getDocument()), "DOCUMENT", ipAddress, userAgent);
            throw new UnauthorizedException("Invalid credentials");
        }

        // Login exitoso
        return processSuccessfulLogin(user, ipAddress, userAgent);
    }

    @Override
    public AuthUser registerCustomer(CustomerRegistrationRequest request) {
        log.debug("Registering new customer with username: {}", request.getUsername());
        
        // Validar que no exista el username
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        // Validar que no exista el email
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Validar que no exista el documento
        if (authUserRepository.existsByDocument(request.getDocument())) {
            throw new BadRequestException("Document already registered");
        }

        // Crear nuevo usuario
        AuthUser newUser = AuthUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .document(request.getDocument())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("CUSTOMER")
                .loginType("DOCUMENT")
                .isActive(true)
                .isEmailVerified(false)
                .isDocumentVerified(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userProfileId(0L) // Se actualizar� cuando se cree el perfil en USER-SERVICE
                .build();

        AuthUser savedUser = authUserRepository.save(newUser);
        log.info("Customer registered successfully with ID: {}", savedUser.getId());
        
        // Llamar al users-service para crear el perfil de usuario
        try {
            CreateProfileFromAuthRequest profileRequest = CreateProfileFromAuthRequest.builder()
                    .authUserId(savedUser.getId())
                    .document(savedUser.getDocument())
                    .name(request.getName())
                    .lastName(request.getLastName())
                    .bornDate(request.getBornDate())
                    .telephone(request.getTelephone())
                    .gender(request.getGender())
                    .build();
            
            usersServiceClient.createUserProfile(profileRequest);
            log.info("User profile creation request sent for authUserId: {}", savedUser.getId());
            
        } catch (Exception e) {
            log.error("Failed to create user profile for authUserId: {}, continuing with registration", 
                     savedUser.getId(), e);
            // No lanzamos la excepción para que el registro en auth-service continúe
        }
        
        // TODO: Enviar email de verificación
        
        return savedUser;
    }

    public AuthUser registerAdmin(AdminRegistrationRequest request) {
        log.debug("Registering new admin with username: {}", request.getUsername());
        
        // Validar que no exista el username
        if (authUserRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        // Validar que no exista el email
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Crear nuevo admin
        AuthUser newAdmin = AuthUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("ADMIN")
                .loginType("EMAIL")
                .isActive(true)
                .isEmailVerified(false)
                .isDocumentVerified(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .userProfileId(0L)
                .build();

        AuthUser savedAdmin = authUserRepository.save(newAdmin);
        log.info("Admin registered successfully with ID: {}", savedAdmin.getId());
        
        // TODO: Enviar email de verificaci�n
        
        return savedAdmin;
    }

    @Override
    public TokenValidationResponse validateToken(String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                List<String> permissions = rolePermissionRepository.findPermissionsByRole(role);
                
                return TokenValidationResponse.builder()
                        .valid(true)
                        .userId(userId)
                        .role(role)
                        .permissions(permissions)
                        .build();
            }
        } catch (Exception e) {
            log.error("Token validation error", e);
        }
        
        return new TokenValidationResponse(false, "Invalid token");
    }

    @Override
    public void logout(String token) {
        if (token != null) {
            userSessionRepository.findBySessionToken(token).ifPresent(session -> {
                session.setIsActive(false);
                userSessionRepository.save(session);
            });
            log.info("User logged out successfully");
        }
    }

    @Override
    public void logoutAllSessions(Long userId) {
        userSessionRepository.deactivateAllUserSessions(userId);
        log.info("All sessions for user {} have been deactivated", userId);
    }

    @Override
    public String refreshToken(String refreshToken) {
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!session.getIsActive() || session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        AuthUser user = authUserRepository.findById(session.getAuthUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<String> permissions = rolePermissionRepository.findPermissionsByRole(user.getRole());
        
        String newToken = jwtTokenProvider.generateTokenFromUserId(
                user.getId(), user.getUsername(), user.getRole(), permissions);
        
        session.setSessionToken(newToken);
        session.setLastActivity(LocalDateTime.now());
        userSessionRepository.save(session);
        
        return newToken;
    }

    @Override
    public AuthUser findByUsername(String username) {
        return authUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public AuthUser findByEmail(String email) {
        return authUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public AuthUser findByDocument(Long document) {
        return authUserRepository.findByDocument(document)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public boolean existsByUsername(String username) {
        return authUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return authUserRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByDocument(Long document) {
        return authUserRepository.existsByDocument(document);
    }

    // M�todos privados de ayuda
    
    private LoginResponse processSuccessfulLogin(AuthUser user, String ipAddress, String userAgent) {
        // Resetear intentos fallidos
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        authUserRepository.save(user);

        // Registrar login exitoso
        recordSuccessfulLogin(user.getUsername(), user.getLoginType(), ipAddress, userAgent);

        // Obtener permisos del usuario
        List<String> permissions = rolePermissionRepository.findPermissionsByRole(user.getRole());

        // Generar tokens
        String token = jwtTokenProvider.generateTokenFromUserId(
                user.getId(), user.getUsername(), user.getRole(), permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getUsername());

        // Crear sesi�n
        UserSession session = UserSession.builder()
                .authUserId(user.getId())
                .sessionToken(token)
                .refreshToken(refreshToken)
                .deviceInfo(extractDeviceInfo(userAgent))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpirationTime() / 1000))
                .lastActivity(LocalDateTime.now())
                .isActive(true)
                .build();

        userSessionRepository.save(session);

        // Construir respuesta
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .document(user.getDocument())
                .role(user.getRole())
                .loginType(user.getLoginType())
                .lastLogin(user.getLastLogin())
                .build();

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userInfo)
                .permissions(permissions)
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .build();
    }

    private void handleFailedLogin(AuthUser user, String identifier, String loginType, 
                                 String ipAddress, String userAgent) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
            log.warn("Account locked for user: {} after {} failed attempts", identifier, attempts);
        }

        authUserRepository.save(user);
        recordFailedLogin(identifier, loginType, ipAddress, userAgent, "Invalid password");
    }

    private void recordFailedLogin(String identifier, String loginType, String ipAddress, 
                                 String userAgent, String reason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .loginIdentifier(identifier)
                .loginType(loginType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(false)
                .failureReason(reason)
                .attemptedAt(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);
        log.warn("Failed login attempt for {}: {}", identifier, reason);
    }

    private void recordSuccessfulLogin(String identifier, String loginType, String ipAddress, String userAgent) {
        LoginAttempt attempt = LoginAttempt.builder()
                .loginIdentifier(identifier)
                .loginType(loginType)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(true)
                .attemptedAt(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);
        log.info("Successful login for {}", identifier);
    }

    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        // Extraer informaci�n b�sica del dispositivo del User-Agent
        if (userAgent.contains("Mobile")) return "Mobile Device";
        if (userAgent.contains("Tablet")) return "Tablet";
        if (userAgent.contains("Windows")) return "Windows Desktop";
        if (userAgent.contains("Mac")) return "Mac Desktop";
        if (userAgent.contains("Linux")) return "Linux Desktop";
        
        return "Unknown Device";
    }
}