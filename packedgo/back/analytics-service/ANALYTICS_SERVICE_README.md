# 📊 ANALYTICS-SERVICE

Microservicio encargado de la generación de reportes y dashboards para el sistema PackedGo.

## 📋 Descripción

Este servicio consolida información de otros microservicios (Orders, Payments, Events) para proporcionar métricas clave a los administradores y organizadores. Implementa un sistema de analítica centralizado que permite a los organizadores visualizar el rendimiento de sus eventos y tomar decisiones basadas en datos.

## 🚀 Tecnologías

- **Java 17** - Lenguaje de programación
- **Spring Boot 3.5.6** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **Spring Security** - Seguridad y autenticación
- **Spring WebFlux** - Cliente HTTP reactivo
- **JWT (0.12.6)** - Autenticación basada en tokens
- **PostgreSQL 15** - Base de datos
- **Lombok** - Reducción de boilerplate
- **Docker** - Contenedorización

## Arquitectura

```
analytics-service/
 controller/
    DashboardController.java     # Endpoints del Dashboard
 service/
    AnalyticsService.java        # Lógica de agregación de datos
 dto/
    DashboardDTO.java            # Objeto de transferencia de datos del dashboard
 security/
    JwtTokenValidator.java       # Validación de tokens JWT
```

## API Endpoints

### Dashboard

**GET** `/dashboard`
Obtiene el dashboard completo para el organizador autenticado.
- **Seguridad**: Requiere rol `ADMIN` o `SUPER_ADMIN`.
```json
Headers: Authorization: Bearer {token}
Response: 200 OK
{
  "totalRevenue": 15000.00,
  "totalOrders": 120,
  "activeEvents": 5,
  "recentSales": [...]
}
```

**GET** `/dashboard/{organizerId}`
Obtiene el dashboard de un organizador específico.
- **Seguridad**: Requiere rol `SUPER_ADMIN` o ser el mismo usuario (`organizerId` coincide con el token).
```json
Headers: Authorization: Bearer {token}
Response: 200 OK
{
  "totalRevenue": 5000.00,
  ...
}
```

### Health Check

**GET** `/dashboard/health`
```text
Analytics Service is UP
```

## Configuración

Variables de entorno requeridas:

```properties
# Server
SERVER_PORT=8087

# Database
DB_URL=jdbc:postgresql://analytics-db:5432/analytics_db
DB_USERNAME=analytics_user
DB_PASSWORD=analytics_password

# JWT
JWT_SECRET=your_jwt_secret_key

# External Services (Optional/If used)
APP_SERVICES_EVENT-SERVICE_BASE-URL=http://event-service:8086
APP_SERVICES_ORDER-SERVICE_BASE-URL=http://order-service:8084
APP_SERVICES_PAYMENT-SERVICE_BASE-URL=http://payment-service:8085
```

## Seguridad

El servicio utiliza JWT para autenticación y autorización.
- **ADMIN**: Acceso a su propio dashboard.
- **SUPER_ADMIN**: Acceso a cualquier dashboard.

## 🐳 Ejecución con Docker

El servicio se ejecuta en el puerto **8087** dentro de la red de Docker `packedgo-network`.

### Desde el directorio raíz del backend:
```bash
docker-compose up -d analytics-service
```

### Construcción individual:
```bash
cd analytics-service
docker build -t analytics-service:latest .
docker run -p 8087:8087 --env-file .env analytics-service:latest
```

### Logs del servicio:
```bash
docker-compose logs -f analytics-service
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

## 🔗 Integración con Otros Servicios

El Analytics Service se comunica con:
- **EVENT-SERVICE** (Puerto 8086) - Para datos de eventos
- **ORDER-SERVICE** (Puerto 8084) - Para datos de órdenes
- **PAYMENT-SERVICE** (Puerto 8085) - Para datos de pagos

## 📦 Base de Datos

**Nombre:** analytics_db (No especificado en docker-compose - servicio en desarrollo)
**Puerto:** TBD
**Usuario:** analytics_user

## 🔐 Autorización

El servicio implementa control de acceso basado en roles:

| Rol | Permisos |
|-----|----------|
| **ADMIN** | Acceso a dashboard propio (`/dashboard`) |
| **SUPER_ADMIN** | Acceso a cualquier dashboard (`/dashboard/{organizerId}`) |
| **CUSTOMER** | Sin acceso a analytics |

## ⚠️ Estado del Proyecto

**Estado Actual:** ⚙️ En Desarrollo

El servicio está parcialmente implementado y requiere:
- Configuración completa de base de datos en docker-compose
- Implementación de lógica de agregación de datos
- Definición de métricas adicionales
- Endpoints de reportes detallados

## 📝 Notas

- Los dashboards son multi-tenant (por `organizerId`)
- Requiere autenticación JWT válida en todos los endpoints
- Utiliza WebClient para comunicación reactiva con otros servicios
