# AUTH-SERVICE

## Descripción General

El AUTH-SERVICE es el microservicio de autenticación y autorización de PackedGo que maneja la gestión completa de usuarios y sesiones. Implementa autenticación diferenciada para administradores (email) y clientes (DNI), junto con funcionalidades de seguridad como JWT, recuperación de contraseñas y verificación de email.

## Puerto de Servicio
**8081**

## Base de Datos
- **Nombre:** auth_db
- **Puerto:** 5433 (PostgreSQL)
- **Tablas principales:**
  - `auth_users` - Usuarios del sistema
  - `user_sessions` - Sesiones activas
  - `email_verification_tokens` - Tokens de verificación de email
  - `password_recovery_tokens` - Tokens de recuperación de contraseña
  - `role_permissions` - Permisos por rol
  - `login_attempts` - Auditoría de intentos de login

## Funcionalidades Principales

### 1. Autenticación Diferenciada
- **Administradores:** Login con email + contraseña
- **Clientes:** Login con DNI + contraseña
- Validación automática de roles y permisos

### 2. Gestión de Usuarios
- Registro de clientes con validación completa
- Registro de administradores con código de autorización
- Verificación de disponibilidad de username, email y documento
- Verificación de email obligatoria

### 3. Seguridad Avanzada
- JWT tokens con tiempo de expiración configurable
- Refresh tokens para renovación automática
- Sistema de bloqueo de cuenta por intentos fallidos (5 intentos, 30 min bloqueo)
- Auditoría completa de intentos de login
- Validación de tokens para otros microservicios

### 4. Recuperación de Contraseñas
- Sistema seguro de reset con validación de email + DNI
- Tokens únicos con expiración de 1 hora
- Invalidación automática de sesiones tras cambio de contraseña

## Endpoints Principales

### AuthController (`/auth`)

#### Autenticación
- `POST /admin/login` - Login de administradores
- `POST /customer/login` - Login de clientes
- `POST /employee/login` - Login de empleados
- `POST /logout` - Cerrar sesión
- `POST /refresh` - Renovar token

#### Registro
- `POST /admin/register` - Registro de administradores
- `POST /customer/register` - Registro de clientes completo

#### Verificación y Recuperación
- `GET /verify-email?token=` - Verificar email
- `POST /resend-verification` - Reenviar email de verificación
- `POST /forgot-password` - Solicitar reset de contraseña
- `POST /reset-password` - Cambiar contraseña con token
- `POST /change-password/{userId}` - Cambiar contraseña (usuario logueado)

#### Gestión de Perfil
- `GET /user/{userId}` - Obtener perfil de usuario
- `PUT /user/{userId}` - Actualizar perfil de usuario

#### Validación de Tokens
- `POST /validate` - Validar token para otros microservicios

### UserController (`/users`)

#### Validación de Disponibilidad
- `GET /exists/username/{username}` - Verificar disponibilidad de username
- `GET /exists/email/{email}` - Verificar disponibilidad de email
- `GET /exists/document/{document}` - Verificar disponibilidad de documento

## Entities Principales

### AuthUser
```java
@Entity
@Table(name = "auth_users")
public class AuthUser {
    private Long id;
    private Long userProfileId;     // Referencia al users-service
    private String username;        // Único, requerido
    private String email;           // Único, opcional para clientes
    private Long document;          // Único, requerido para clientes
    private String passwordHash;    // Hash bcrypt
    private String role;           // ADMIN, SUPER_ADMIN, CUSTOMER
    private String loginType;      // EMAIL, DOCUMENT
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Boolean isDocumentVerified;
    private LocalDateTime lastLogin;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedUntil;
    // ... campos de auditoría
}
```

### UserSession
Gestiona sesiones activas con tokens JWT y refresh tokens, incluyendo información del dispositivo e IP.

### EmailVerificationToken / PasswordRecoveryToken
Tokens temporales para verificación de email y recuperación de contraseñas con expiración automática.

## DTOs Principales

### Requests
- `CustomerRegistrationRequest` - Registro completo de cliente con datos personales
- `AdminRegistrationRequest` - Registro de admin con código de autorización
- `CustomerLoginRequest` - Login con documento + contraseña
- `AdminLoginRequest` - Login con email + contraseña
- `PasswordResetRequest` - Solicitud de reset con email + documento
- `ChangePasswordRequest` - Cambio de contraseña con token

### Responses
- `LoginResponse` - Token, refresh token, info de usuario y permisos
- `TokenValidationResponse` - Validación de token con permisos
- `ApiResponse<T>` - Wrapper estándar para todas las respuestas

## Servicios

### AuthService / AuthServiceImpl
- **loginAdmin()** - Autenticación de administradores con validación de intentos
- **loginCustomer()** - Autenticación de clientes con validación de cuenta
- **registerCustomer()** - Registro completo + creación de perfil en users-service
- **registerAdmin()** - Registro de admin con código de autorización
- **validateToken()** - Validación de JWT con permisos
- **verifyEmail()** - Verificación de email con token
- **requestPasswordReset()** - Generación de token de recuperación
- **resetPassword()** - Cambio de contraseña con token

### EmailService / EmailServiceImpl
- Envío de emails de verificación con templates HTML
- Envío de emails de recuperación de contraseña
- Configuración SMTP con Gmail/SendGrid

### UsersServiceClient
- Cliente HTTP para comunicación con users-service
- Creación automática de perfil tras registro de cliente

## Configuración de Seguridad

### JWT Configuration
- Secret key configurable
- Tiempo de expiración: 1 hora (configurable)
- Refresh token: 30 días (configurable)
- Algoritmo HS256

### Password Security
- Bcrypt encoding con salt automático
- Validación de fortaleza mínima (6 caracteres)

### Account Security
- Máximo 5 intentos de login fallidos
- Bloqueo de cuenta por 30 minutos
- Auditoría completa de intentos

## Variables de Entorno

```bash
# Server Configuration
SERVER_PORT=8081

# Database Configuration
DATABASE_URL=jdbc:postgresql://auth-db:5432/auth_db
DATABASE_USER=auth_user
DATABASE_PASSWORD=secure_password

# JWT Configuration  
JWT_SECRET=your_jwt_secret_minimum_32_characters_here
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=2592000000

# Email Configuration
EMAIL_USERNAME=your_gmail@gmail.com
EMAIL_PASSWORD=your_gmail_app_password_here
EMAIL_FROM=noreply@packedgo.com

# External Services
USERS_SERVICE_URL=http://users-service:8082
FRONTEND_BASE_URL=http://localhost:8080
```

## Dependencias con Otros Servicios

### Users-Service
- **Outbound:** Creación automática de perfil tras registro de cliente
- **Endpoint:** `POST /api/user-profiles/from-auth`

### Todos los Microservicios
- **Inbound:** Validación de tokens JWT
- **Endpoint:** `POST /auth/validate`

## Seguridad y Validaciones

### Validaciones de Registro
- Username único, 3-50 caracteres
- Email válido y único para admins
- Documento único para clientes
- Contraseña mínimo 6 caracteres
- Campos personales obligatorios (nombre, apellido, fecha nacimiento, teléfono, género)

### Funcionalidades de Seguridad
- Rate limiting por IP en intentos de login
- Tokens únicos no reutilizables para verificación/reset
- Invalidación de sesiones en cascada
- Logging completo de eventos de seguridad
- Validación de autorización por código para admins

## Patrones Implementados
- Repository Pattern para acceso a datos
- Service Layer para lógica de negocio
- DTO Pattern para transferencia de datos
- Builder Pattern para construcción de objetos
- Global Exception Handler para manejo de errores

## Características Especiales
- Autenticación diferenciada por tipo de usuario
- Sistema de permisos granular por rol
- Integración automática con users-service
- Templates HTML para emails responsivos
- Auditoría completa de eventos de autenticación
- Sistema robusto de recuperación de cuentas