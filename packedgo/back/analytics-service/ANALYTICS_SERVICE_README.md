# ANALYTICS-SERVICE

Microservicio encargado de la generación de reportes y dashboards para el sistema PackedGo.

## Descripción

Este servicio consolida información de otros microservicios (Orders, Payments, Events) para proporcionar métricas clave a los administradores y organizadores.

## Tecnologías

- **Java 17**
- **Spring Boot 3.5.6**
- **Spring Data JPA**
- **Spring Security (JWT)**
- **PostgreSQL**
- **Lombok**

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

## Ejecución con Docker

El servicio se ejecuta en el puerto **8087** dentro de la red de Docker `packedgo-network`.
