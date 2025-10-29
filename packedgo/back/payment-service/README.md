# Payment Service - MercadoPago Integration

Microservicio de gestión de pagos integrado con MercadoPago para el sistema PackedGo.

## 📋 Descripción

Este microservicio proporciona una pasarela de pagos segura que permite a los administradores procesar pagos a través de MercadoPago. Cada administrador tiene sus propias credenciales almacenadas de forma segura en la base de datos.

## 🏗️ Arquitectura

### Estructura del Proyecto

```
payment-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/packedgo/payment_service/
│   │   │       ├── config/                 # Configuraciones
│   │   │       │   ├── MercadoPagoConfig.java
│   │   │       │   └── SecurityConfig.java
│   │   │       ├── controller/             # Controladores REST
│   │   │       │   └── PaymentController.java
│   │   │       ├── dto/                    # Data Transfer Objects
│   │   │       │   ├── PaymentRequest.java
│   │   │       │   ├── PaymentResponse.java
│   │   │       │   └── WebhookNotification.java
│   │   │       ├── exception/              # Manejo de excepciones
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── ErrorResponse.java
│   │   │       │   ├── PaymentException.java
│   │   │       │   ├── CredentialException.java
│   │   │       │   └── ResourceNotFoundException.java
│   │   │       ├── model/                  # Entidades JPA
│   │   │       │   ├── Payment.java
│   │   │       │   └── AdminCredential.java
│   │   │       ├── repository/             # Repositorios JPA
│   │   │       │   ├── PaymentRepository.java
│   │   │       │   └── AdminCredentialRepository.java
│   │   │       ├── service/                # Lógica de negocio
│   │   │       │   ├── PaymentService.java
│   │   │       │   └── AdminCredentialService.java
│   │   │       └── PaymentServiceApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## 🚀 Características Principales

- ✅ Integración completa con MercadoPago SDK
- ✅ Multi-tenant: Cada admin tiene sus propias credenciales
- ✅ Webhooks para notificaciones de MercadoPago
- ✅ Gestión de estados de pago
- ✅ Seguridad con Spring Security
- ✅ Validación de datos con Bean Validation
- ✅ Manejo centralizado de excepciones
- ✅ Health checks con Actuator
- ✅ Logging estructurado
- ✅ Persistencia con PostgreSQL

## 📦 Tecnologías

- **Java 17**
- **Spring Boot 3.5.7**
- **Spring Data JPA**
- **Spring Security**
- **PostgreSQL**
- **MercadoPago SDK 2.2.0**
- **Lombok**
- **Maven**

## 🔧 Configuración

### Requisitos Previos

- Java 17+
- Maven 3.6+
- PostgreSQL 12+

### Variables de Entorno

```properties
# Base de datos
DB_URL=jdbc:postgresql://localhost:5432/payment_service_db
DB_USERNAME=postgres
DB_PASSWORD=tu_password

# Webhook URL (para producción)
WEBHOOK_URL=https://tu-dominio.com/api/payments/webhook

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:4200
```

### Base de Datos

Crear la base de datos en PostgreSQL:

```sql
CREATE DATABASE payment_service_db;
```

Las tablas se crearán automáticamente al iniciar la aplicación (Hibernate DDL auto).

### Instalación

1. Clonar el repositorio:
```bash
git clone <repository-url>
cd payment-service
```

2. Configurar las variables de entorno o editar `application.properties`

3. Compilar el proyecto:
```bash
mvn clean install
```

4. Ejecutar la aplicación:
```bash
mvn spring-boot:run
```

La aplicación estará disponible en `http://localhost:8082`

## 📚 API Endpoints

### 1. Crear Preferencia de Pago

Crea una preferencia de pago en MercadoPago.

**Endpoint:** `POST /api/payments/create`

**Request Body:**
```json
{
  "adminId": 1,
  "orderId": "ORDER-12345",
  "amount": 1500.00,
  "description": "Paquete Premium",
  "payerEmail": "cliente@email.com",
  "payerName": "Juan Pérez",
  "externalReference": "REF-12345",
  "successUrl": "https://tu-app.com/success",
  "failureUrl": "https://tu-app.com/failure",
  "pendingUrl": "https://tu-app.com/pending"
}
```

**Response:**
```json
{
  "paymentId": 123,
  "orderId": "ORDER-12345",
  "status": "PENDING",
  "amount": 1500.00,
  "currency": "ARS",
  "preferenceId": "123456789-abc123-def456",
  "initPoint": "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=...",
  "sandboxInitPoint": "https://sandbox.mercadopago.com.ar/checkout/v1/redirect?pref_id=...",
  "message": "Preferencia de pago creada exitosamente"
}
```

### 2. Webhook de MercadoPago

Recibe notificaciones de cambios de estado de pagos.

**Endpoint:** `POST /api/payments/webhook?adminId={adminId}`

**Request Body:**
```json
{
  "action": "payment.updated",
  "api_version": "v1",
  "data": {
    "id": "123456789"
  },
  "date_created": "2024-01-20T10:00:00Z",
  "id": 123456789,
  "live_mode": true,
  "type": "payment",
  "user_id": 987654321
}
```

**Response:**
```json
{
  "status": "processed"
}
```

### 3. Consultar Pago por OrderId

Consulta el estado de un pago por su orderId.

**Endpoint:** `GET /api/payments/order/{orderId}`

**Response:**
```json
{
  "message": "Endpoint de consulta"
}
```

### 4. Health Check

Verifica el estado del servicio.

**Endpoint:** `GET /api/payments/health`

**Response:**
```json
{
  "status": "UP"
}
```

### 5. Actuator Health

**Endpoint:** `GET /actuator/health`

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

## 🔐 Seguridad

### Autenticación

El servicio está configurado con Spring Security. Los siguientes endpoints son públicos:

- `/api/payments/webhook` - Para recibir notificaciones de MercadoPago
- `/api/payments/health` - Health check
- `/actuator/health` - Actuator health check

Todos los demás endpoints requieren autenticación (JWT - implementación pendiente).

### Credenciales de MercadoPago

Las credenciales de MercadoPago se almacenan de forma segura en la base de datos:

- Cada administrador tiene sus propias credenciales
- Las credenciales nunca se envían desde el cliente
- El servicio las recupera de la BD usando el `adminId`

### Tabla: admin_credentials

```sql
CREATE TABLE admin_credentials (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL UNIQUE,
    access_token VARCHAR(500) NOT NULL,
    public_key VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_sandbox BOOLEAN NOT NULL DEFAULT false,
    merchant_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

Para agregar credenciales de un admin:

```sql
INSERT INTO admin_credentials (admin_id, access_token, public_key, is_active, is_sandbox, created_at)
VALUES (1, 'APP_USR-123456789-...', 'APP_USR-123456789-...', true, true, NOW());
```

## 💾 Modelo de Datos

### Payment

Representa un pago en el sistema.

```java
public class Payment {
    private Long id;
    private Long adminId;                    // ID del administrador
    private String orderId;                  // ID único de la orden
    private Long mpPaymentId;                // ID del pago en MercadoPago
    private BigDecimal amount;               // Monto
    private String currency;                 // Moneda (ARS)
    private PaymentStatus status;            // Estado del pago
    private String paymentMethod;            // Método de pago
    private String payerEmail;               // Email del pagador
    private String payerName;                // Nombre del pagador
    private String description;              // Descripción
    private String externalReference;        // Referencia externa
    private String preferenceId;             // ID de preferencia MP
    private Long merchantOrderId;            // ID de orden del merchant
    private BigDecimal transactionAmount;    // Monto de la transacción
    private String statusDetail;             // Detalle del estado
    private String paymentTypeId;            // Tipo de pago
    private Integer installments;            // Cuotas
    private LocalDateTime createdAt;         // Fecha de creación
    private LocalDateTime updatedAt;         // Fecha de actualización
    private LocalDateTime approvedAt;        // Fecha de aprobación
}
```

### PaymentStatus (Enum)

```java
public enum PaymentStatus {
    PENDING,        // Pendiente
    APPROVED,       // Aprobado
    REJECTED,       // Rechazado
    CANCELLED,      // Cancelado
    REFUNDED,       // Reembolsado
    IN_PROCESS,     // En proceso
    IN_MEDIATION,   // En mediación
    CHARGED_BACK    // Contracargo
}
```

## 🔄 Flujo de Pago

1. **Cliente solicita pago**: El frontend envía la información del pago al endpoint `/api/payments/create`
2. **Validación**: El servicio valida los datos y obtiene las credenciales del admin desde la BD
3. **Creación en MercadoPago**: Se crea una preferencia de pago en MercadoPago
4. **Respuesta al cliente**: Se retorna el `initPoint` donde el usuario completará el pago
5. **Usuario paga**: El usuario completa el pago en la plataforma de MercadoPago
6. **Webhook**: MercadoPago notifica al servicio sobre el cambio de estado
7. **Actualización**: El servicio actualiza el estado del pago en la BD
8. **Notificación**: (Opcional) Notificar al servicio de órdenes sobre el pago

## 🧪 Testing

```bash
# Ejecutar tests
mvn test

# Ejecutar tests con cobertura
mvn test jacoco:report
```

## 📝 Logging

El servicio genera logs estructurados con diferentes niveles:

- **DEBUG**: Información detallada para debugging
- **INFO**: Eventos importantes del sistema
- **ERROR**: Errores y excepciones

Ejemplo:
```
2024-01-20 10:00:00 - POST /api/payments/create - AdminId: 1, OrderId: ORDER-12345
2024-01-20 10:00:01 - Validando credenciales para admin: 1
2024-01-20 10:00:02 - Preferencia creada exitosamente: 123456789-abc123-def456 para orden: ORDER-12345
```

## 🐛 Manejo de Errores

El servicio tiene un manejador global de excepciones que retorna respuestas consistentes:

```json
{
  "timestamp": "2024-01-20T10:00:00",
  "status": 400,
  "error": "Payment Error",
  "message": "Error al crear el pago",
  "details": {
    "field": "amount",
    "error": "El monto debe ser mayor a 0"
  }
}
```

## 📊 Monitoreo

### Actuator Endpoints

- `/actuator/health` - Estado del servicio
- `/actuator/info` - Información del servicio
- `/actuator/metrics` - Métricas de rendimiento

## 🚀 Despliegue

### Docker (Próximamente)

```dockerfile
FROM openjdk:17-jdk-slim
COPY target/payment-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Variables de Entorno para Producción

```bash
export DB_URL=jdbc:postgresql://prod-db:5432/payment_service_db
export DB_USERNAME=payment_user
export DB_PASSWORD=secure_password
export WEBHOOK_URL=https://api.packedgo.com/api/payments/webhook
export CORS_ORIGINS=https://app.packedgo.com
```

## 🔮 Próximas Mejoras

- [ ] Implementar autenticación JWT completa
- [ ] Agregar endpoint para consultar historial de pagos
- [ ] Implementar reembolsos
- [ ] Agregar soporte para pagos recurrentes
- [ ] Dockerizar el servicio
- [ ] Implementar circuit breaker con Resilience4j
- [ ] Agregar cache con Redis
- [ ] Implementar métricas con Micrometer/Prometheus
- [ ] Agregar documentación con Swagger/OpenAPI

## 📄 Licencia

Este proyecto es parte del sistema PackedGo.

## 👥 Contacto

Para más información sobre el proyecto, contactar al equipo de desarrollo.

---

**Versión:** 0.0.1-SNAPSHOT  
**Última actualización:** Octubre 2025
