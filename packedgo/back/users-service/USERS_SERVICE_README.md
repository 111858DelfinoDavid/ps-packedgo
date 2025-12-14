# 👥 USERS-SERVICE

## 📋 Descripción General

El USERS-SERVICE es el microservicio encargado de **gestionar los perfiles de usuario** de PackedGo. Maneja toda la información personal y demográfica de los usuarios, complementando la autenticación del AUTH-SERVICE con datos completos del perfil. También gestiona el sistema de empleados y sus operaciones.

### Características Principales:
- 👤 Gestión completa de perfiles de usuario
- 🔄 Integración automática con AUTH-SERVICE
- 👷 Sistema de gestión de empleados para admins
- 📱 Validación de tickets y registro de consumos (proxy a event-service)
- 🗑️ Soft delete para preservación de datos
- 🔍 Consultas optimizadas por estado activo

## 🚀 Puerto de Servicio
**8082** (HTTP)
**5006** (Debug JDWP)

## 📦 Base de Datos
- **Nombre:** users_db
- **Puerto:** 5434 (PostgreSQL 15)
- **Usuario:** users_user
- **Imagen:** postgres:15-alpine

### Tablas principales:
  - `user_profiles` - Perfiles completos de usuarios
  - `employees` - Empleados asignados a eventos (TBD en el esquema mostrado)

## 🚀 Tecnologías

- **Java 17** - Lenguaje de programación
- **Spring Boot 3.5.6** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **Spring Security** - Seguridad
- **Spring Validation** - Validación de datos
- **Spring WebFlux** - Cliente HTTP reactivo
- **Spring Actuator** - Monitoreo y métricas
- **ModelMapper 3.1.1** - Mapeo entre DTOs y entidades
- **PostgreSQL 15** - Base de datos
- **Lombok** - Reducción de boilerplate
- **H2** - Base de datos en memoria para tests
- **Docker** - Contenedorización

## Funcionalidades Principales

### 1. Gestión de Perfiles de Usuario
- CRUD completo de perfiles de usuario
- Creación automática desde AUTH-SERVICE
- Gestión de datos personales y demográficos
- Soft delete (eliminación lógica)

### 2. Integración con AUTH-SERVICE
- Recepción de datos de registro desde auth-service
- Creación automática de perfil tras registro exitoso
- Sincronización de datos entre servicios

### 3. Consultas Especializadas
- Búsqueda por documento (único)
- Búsqueda por ID de usuario de auth
- Filtrado por usuarios activos
- Validación de existencia

## Endpoints Principales

### UserProfileController (`/api/user-profiles`)

#### Gestión Básica
- `POST /` - Crear nuevo perfil de usuario
- `GET /{id}` - Obtener perfil por ID
- `GET /` - Obtener todos los perfiles
- `PUT /{id}` - Actualizar perfil existente
- `DELETE /{id}` - Eliminar perfil físicamente
- `DELETE /logical/{id}` - Eliminar perfil lógicamente

#### Integración con AUTH-SERVICE
- `POST /from-auth` - Crear perfil desde auth-service (endpoint especializado)

#### Consultas por Estado Activo
- `GET /active` - Obtener todos los perfiles activos
- `GET /active/{id}` - Obtener perfil activo por ID
- `GET /active/document/{document}` - Obtener perfil activo por documento

### AdminEmployeeController (`/admin/employees`)

#### Gestión de Empleados (Admin)
- `POST /` - Crear nuevo empleado
- `GET /` - Listar empleados del admin
- `GET /{id}` - Obtener empleado por ID
- `PUT /{id}` - Actualizar empleado
- `DELETE /{id}` - Eliminar empleado
- `PATCH /{id}/toggle-status` - Activar/Desactivar empleado

### EmployeeController (`/employee`)

#### Operaciones de Empleado
- `GET /assigned-events` - Obtener eventos asignados
- `POST /validate-ticket` - Validar ticket de entrada (Proxy a event-service)
- `POST /register-consumption` - Registrar consumo (Proxy a event-service)
- `GET /stats` - Obtener estadísticas del empleado

### InternalEmployeeController (`/internal/employees`)

#### Uso Interno (Auth Service)
- `POST /validate` - Validar credenciales de empleado (usado por auth-service durante login)

## Entities Principales

### UserProfileEntity
```java
@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {
    private Long id;
    private Long authUserId;        // Referencia al AUTH-SERVICE
    private String name;            // Nombre, requerido
    private String lastName;        // Apellido, requerido
    private Gender gender;          // MALE, FEMALE, OTHER
    private Long document;          // DNI único
    private LocalDate bornDate;     // Fecha de nacimiento
    private Long telephone;         // Teléfono único
    private String profileImageUrl; // URL de imagen de perfil (opcional)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;       // Para soft delete
}
```

## Modelo de Dominio

### UserProfile
Clase de dominio que representa un perfil de usuario con todas sus propiedades y métodos de negocio.

### Gender (Enum)
```java
public enum Gender {
    MALE("Masculino"),
    FEMALE("Femenino"),
    OTHER("Otro");
}
```

## DTOs Principales

### UserProfileDTO
DTO principal para transferencia de datos de perfil con validaciones.

### CreateProfileFromAuthRequest
```java
public class CreateProfileFromAuthRequest {
    private Long authUserId;        // ID del usuario en auth-service
    private Long document;          // DNI del usuario
    private String name;            // Nombre completo
    private String lastName;        // Apellido
    private LocalDate bornDate;     // Fecha de nacimiento
    private Long telephone;         // Teléfono
    private Gender gender;          // Género
}
```

## Servicios

### UserProfileService / UserProfileServiceImpl

#### Métodos Principales
- **create()** - Crear nuevo perfil con validación de duplicados
- **createFromAuthService()** - Crear perfil desde datos del auth-service
- **getById()** - Obtener perfil por ID con manejo de errores
- **getByDocument()** - Buscar por documento único
- **getByAuthUserId()** - Buscar por ID de usuario de auth
- **existsByAuthUserId()** - Verificar existencia por auth user ID
- **update()** - Actualizar perfil existente
- **delete()** - Eliminación física
- **deleteLogical()** - Eliminación lógica (isActive = false)

#### Métodos de Consulta Activa
- **getAllActive()** - Todos los perfiles activos
- **getByIdActive()** - Perfil activo por ID
- **getByDocumentActive()** - Perfil activo por documento
- **getByAuthUserIdActive()** - Perfil activo por auth user ID

## Repository

### UserProfileRepository
```java
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
    Optional<UserProfileEntity> findByDocument(Long document);
    Optional<UserProfileEntity> findByAuthUserId(Long authUserId);
    boolean existsByAuthUserId(Long authUserId);
    
    // Consultas por estado activo
    List<UserProfileEntity> findByIsActiveTrue();
    Optional<UserProfileEntity> findByIdAndIsActiveTrue(Long id);
    Optional<UserProfileEntity> findByDocumentAndIsActiveTrue(Long document);
    Optional<UserProfileEntity> findByAuthUserIdAndIsActiveTrue(Long authUserId);
}
```

## Configuración

### ModelMapper
Configuración automática de ModelMapper para mapeo entre DTOs y entidades con personalización para campos específicos.

```java
@Configuration
public class MappersConfig {
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
```

## Variables de Entorno

```bash
# Server Configuration
SERVER_PORT=8082

# Database Configuration
DATABASE_URL=jdbc:postgresql://users-db:5432/users_db
DATABASE_USER=users_db_user
DATABASE_PASSWORD=secure_users_password

# External Services
AUTH_SERVICE_URL=http://auth-service:8081

# Logging Configuration
LOGGING_LEVEL_USERS=DEBUG
```

## Validaciones y Reglas de Negocio

### Validaciones de Creación
- **Documento único:** No puede existir otro perfil con el mismo documento
- **AuthUserId único:** Un usuario de auth solo puede tener un perfil
- **Campos requeridos:** name, lastName, gender, document, bornDate, telephone
- **Formato de teléfono:** Debe ser un número válido y único

### Reglas de Negocio
- **Soft Delete:** Los perfiles se marcan como inactivos en lugar de eliminarse
- **Auditoría automática:** Timestamps de creación y actualización
- **Integridad referencial:** Validación de existencia de authUserId

## Dependencias con Otros Servicios

### AUTH-SERVICE
- **Inbound:** Recibe solicitudes de creación de perfil tras registro
- **Endpoint usado:** `POST /api/user-profiles/from-auth`
- **Flujo:** AUTH-SERVICE → registro exitoso → llamada automática → creación de perfil

## Manejo de Errores

### Excepciones Personalizadas
- **RuntimeException:** Para casos de negocio (duplicados, no encontrados)
- **Validaciones automáticas:** Campos requeridos y formatos
- **Logging detallado:** Registro de errores y operaciones exitosas

### Respuestas HTTP
- **201 Created:** Perfil creado exitosamente
- **200 OK:** Operaciones exitosas de consulta/actualización
- **400 Bad Request:** Datos inválidos o reglas de negocio violadas
- **404 Not Found:** Perfil no encontrado
- **409 Conflict:** Conflicto de unicidad (documento duplicado)

## Características Especiales

### Integración Automática
- Creación transparente de perfiles desde auth-service
- Manejo de fallos sin afectar el registro principal
- Logging detallado de operaciones inter-servicios

### Flexibilidad de Consultas
- Métodos específicos para usuarios activos
- Búsqueda por múltiples campos únicos
- Soporte para eliminación física y lógica

### Mapeo Automático
- Conversión automática entre DTOs y entidades
- Configuración centralizada de mapeo
- Optimización de consultas con proyecciones

## Patrones Implementados
- **Repository Pattern:** Para acceso a datos
- **Service Layer:** Para lógica de negocio
- **DTO Pattern:** Para transferencia de datos
- **Active Record Pattern:** Gestión de estado de entidades
- **Soft Delete Pattern:** Para preservación de datos históricos

## Consideraciones de Performance
- Índices únicos en campos de búsqueda frecuente
- Consultas optimizadas por estado activo
- Mapeo eficiente con ModelMapper
- Validaciones tempranas para reducir carga en BD

## 🐳 Ejecución con Docker

### Desde el directorio raíz del backend:
```bash
docker-compose up -d users-service
```

### Logs del servicio:
```bash
docker-compose logs -f users-service
```

### Reconstruir imagen:
```bash
docker-compose up -d --build users-service
```

## 🔧 Desarrollo Local

### Requisitos:
- Java 17+
- Maven 3.8+
- PostgreSQL 15+ (o usar Docker)

### Ejecutar localmente:
```bash
./mvnw spring-boot:run
```

### Compilar:
```bash
./mvnw clean package
```

### Tests:
```bash
./mvnw test
```

## 🔗 Integración con Otros Servicios

### AUTH-SERVICE (Inbound)
- **Endpoint:** `POST /api/user-profiles/from-auth`
- **Propósito:** Creación automática de perfil tras registro exitoso
- **Flujo:** AUTH-SERVICE → registro → users-service → crear perfil

### EVENT-SERVICE (Outbound)
- **Endpoints:** `/api/qr-validation/*`
- **Propósito:** Validación de tickets y registro de consumos
- **Flujo:** Employee → users-service (proxy) → event-service
- **URL Configurada:** `EVENT_SERVICE_URL=http://event-service:8086/api`

## 🔐 Seguridad

- **Autenticación:** Spring Security configurado
- **Endpoints Internos:** `/internal/*` solo para comunicación entre microservicios
- **Validación:** Campos requeridos validados con `@Valid`
- **Integridad:** Validación de unicidad en documento, teléfono y authUserId

## 📊 Health Check

```bash
curl http://localhost:8082/actuator/health
```

Respuesta esperada:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

## 📝 Notas de Desarrollo

- Los perfiles se crean automáticamente desde auth-service tras registro
- El campo `isActive` permite soft delete sin perder datos históricos
- Los empleados pueden validar tickets y registrar consumos en tiempo real
- ModelMapper se configura globalmente para mapeos automáticos
- Todas las timestamps son gestionadas automáticamente por JPA