# 🛒 ORDER-SERVICE

Microservicio para gestión de carritos de compra del sistema PackedGo.

## 📋 Descripción

ORDER-SERVICE maneja:
- ✅ Creación y gestión de carritos de compra
- ✅ Expiración automática de carritos (10 minutos)
- ✅ Integración con EVENT-SERVICE para validar stock
- ✅ Validación JWT para autenticación
- ✅ Limpieza automática de carritos expirados

## 🚀 Tecnologías

- **Java 17** - Lenguaje de programación
- **Spring Boot 3.5.6** - Framework
- **Spring Data JPA** - Persistencia
- **PostgreSQL 15** - Base de datos
- **WebClient** - Cliente HTTP reactivo
- **JWT** - Autenticación
- **ModelMapper** - Conversión DTOs
- **Lombok** - Reducción de boilerplate
- **Docker** - Contenedorización

## 🏗️ Arquitectura

```
order-service/
├── controller/       # Endpoints REST
├── service/          # Lógica de negocio
│   └── impl/
├── repository/       # Acceso a datos
├── entity/           # Entidades JPA
├── dto/              # DTOs (request/response/external)
├── external/         # Clientes HTTP
├── config/           # Configuraciones
├── security/         # JWT validation
└── exception/        # Manejo de errores
```

## 📦 Base de Datos

### Tablas

**shopping_carts**
- Carrito principal del usuario
- Expiración automática a 10 minutos
- Estados: ACTIVE, EXPIRED, CHECKED_OUT

**cart_items**
- Items individuales (eventos) en el carrito
- Cálculo automático de subtotales
- Relación con consumos

**cart_item_consumptions**
- Consumos asociados a cada item
- Cantidad y precios individuales

## 🔌 API Endpoints

### Carrito de Compras

**POST** `/api/cart/add`
```json
Headers: Authorization: Bearer {token}
Body: {
  "eventId": 1,
  "consumptions": [
    { "consumptionId": 1, "quantity": 2 },
    { "consumptionId": 2, "quantity": 1 }
  ]
}
Response: 201 CREATED
```

**GET** `/api/cart`
```json
Headers: Authorization: Bearer {token}
Response: 200 OK
{
  "id": 1,
  "userId": 123,
  "status": "ACTIVE",
  "expiresAt": "2025-10-18T15:30:00",
  "items": [...],
  "totalAmount": 150.00,
  "itemCount": 2,
  "expired": false
}
```

**DELETE** `/api/cart/items/{itemId}`
```
Headers: Authorization: Bearer {token}
Response: 200 OK (carrito actualizado) o 204 NO CONTENT (vacío)
```

**DELETE** `/api/cart`
```
Headers: Authorization: Bearer {token}
Response: 204 NO CONTENT
```

**PUT** `/api/cart/items/{itemId}`
```json
Headers: Authorization: Bearer {token}
Body: { "quantity": 3 }
Response: 200 OK
```

## 🐳 Docker

### Construcción Local

```bash
# Desde el directorio order-service
docker build -t order-service:latest .
```

### Ejecutar con Docker Compose

```bash
# Desde el directorio back/
docker-compose up -d order-service order-db

# Ver logs
docker-compose logs -f order-service

# Detener
docker-compose down
```

### Ejecutar TODO el sistema

```bash
# Desde el directorio back/
docker-compose up -d

# Servicios incluidos:
# - auth-service (8081)
# - users-service (8082)
# - event-service (8086)
# - order-service (8084)
# - payment-service (8087)
# - Todas las bases de datos
```

## 🔧 Variables de Entorno

Archivo `.env`:

```bash
# JWT
JWT_SECRET=your_super_secret_jwt_key_here

# Database
POSTGRES_DB=orders_db
POSTGRES_USER=orders_user
POSTGRES_PASSWORD=orders_password

# External Services
EVENT_SERVICE_URL=http://event-service:8086/api
AUTH_SERVICE_URL=http://auth-service:8081/api

# Cart Config
CART_EXPIRATION_MINUTES=10
CART_CLEANUP_INTERVAL_MINUTES=5
```

## 🏃 Ejecución Local (sin Docker)

### Prerrequisitos

1. Java 17
2. Maven 3.8+
3. PostgreSQL 15 (puerto 5436)
4. EVENT-SERVICE corriendo (puerto 8086)
5. AUTH-SERVICE corriendo (puerto 8081)

### Base de Datos

```sql
CREATE DATABASE orders_db;
CREATE USER orders_user WITH PASSWORD 'orders_password';
GRANT ALL PRIVILEGES ON DATABASE orders_db TO orders_user;
```

### Ejecución

```bash
# Instalar dependencias
./mvnw clean install

# Ejecutar
./mvnw spring-boot:run

# O con JAR
./mvnw package
java -jar target/order-service-0.0.1-SNAPSHOT.jar
```

Servicio disponible en: `http://localhost:8084/api`

## 📊 Health Check

```bash
# Verificar salud del servicio
curl http://localhost:8084/actuator/health

# Response
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

## 🔄 Tareas Programadas

**Limpieza de Carritos Expirados**
- **Frecuencia**: Cada 5 minutos
- **Acción**: Marca carritos activos que expiraron como EXPIRED

**Eliminación de Carritos Antiguos**
- **Frecuencia**: Diaria a las 3:00 AM
- **Acción**: Elimina carritos EXPIRED/CHECKED_OUT de más de 30 días

## ⚠️ Manejo de Errores

| Código | Error | Descripción |
|--------|-------|-------------|
| 200 | OK | Operación exitosa |
| 201 | Created | Carrito creado/actualizado |
| 204 | No Content | Carrito eliminado |
| 400 | Bad Request | Datos de entrada inválidos |
| 404 | Not Found | Carrito/Evento no encontrado |
| 409 | Conflict | Stock no disponible |
| 410 | Gone | Carrito expirado |
| 500 | Internal Error | Error del servidor |
| 503 | Service Unavailable | Error comunicación con EVENT-SERVICE |

## 🔗 Dependencias Externas

- **EVENT-SERVICE** (8086): Validación de eventos y stock
- **AUTH-SERVICE** (8081): Validación de tokens JWT (indirecta)

## 📝 Notas de Desarrollo

- Los carritos expiran automáticamente a los 10 minutos de inactividad
- Se valida stock disponible antes de agregar al carrito
- WebClient con .block() para mantener API sincrónica
- ModelMapper para conversiones Entity ↔ DTO
- JWT extraído del header Authorization

## 🧪 Testing

Tests salteados según decisión del proyecto. Para implementar en el futuro:
- Unit tests con JUnit 5 y Mockito
- Integration tests con @SpringBootTest
- MockWebServer para EventServiceClient

## 👥 Equipo

Proyecto PackedGo - Sistema de Gestión de Eventos

---

**Puerto**: 8084  
**Context Path**: `/api`  
**Base URL**: `http://localhost:8084/api`
