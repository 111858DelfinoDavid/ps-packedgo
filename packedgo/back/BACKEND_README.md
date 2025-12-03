# PackedGo Backend

Este repositorio contiene la arquitectura de microservicios para el sistema PackedGo. El backend está construido principalmente con **Java 17** y **Spring Boot**, utilizando **Docker** para la orquestación de contenedores.

## 🏗️ Arquitectura de Microservicios

El sistema está dividido en los siguientes servicios:

| Servicio | Puerto | Descripción | Base de Datos |
|----------|--------|-------------|---------------|
| **Auth Service** | 8081 | Gestión de autenticación (JWT), roles y sesiones. | PostgreSQL (5433) |
| **Users Service** | 8082 | Gestión de perfiles de usuarios y empleados. | PostgreSQL (5434) |
| **Order Service** | 8084 | Gestión de órdenes de compra y carritos. | PostgreSQL (5436) |
| **Payment Service** | 8085 | Pasarela de pagos (Stripe) y webhooks. | PostgreSQL (5432) |
| **Event Service** | 8086 | Gestión de eventos, tickets, consumiciones y validación QR. | PostgreSQL (5435) |
| **Analytics Service** | 8087 | Dashboard y reportes para organizadores. | PostgreSQL |
| **API Gateway** | - | (En desarrollo) Punto de entrada unificado. | - |

## 🚀 Requisitos Previos

- **Docker** y **Docker Compose** instalados.
- **Java 17** (para desarrollo local sin Docker).
- **Maven** (para compilación local).

## 🛠️ Cómo Iniciar el Sistema

La forma recomendada de ejecutar el backend completo es utilizando Docker Compose desde esta carpeta raíz (`back/`).

### 1. Configuración de Variables de Entorno
Asegúrate de que cada microservicio tenga su archivo `.env` configurado correctamente (basado en `.env.example` si existe).

### 2. Ejecutar con Docker Compose
Para levantar todos los servicios y bases de datos:

```bash
docker-compose up -d --build
```

Esto iniciará:
- Todos los contenedores de bases de datos PostgreSQL.
- Todos los microservicios Spring Boot.
- Red interna `packedgo-network`.

### 3. Verificar Estado
Puedes ver el estado de los contenedores con:

```bash
docker-compose ps
```

O ver los logs de un servicio específico:

```bash
docker-compose logs -f auth-service
```

## 📚 Documentación Específica

Cada microservicio cuenta con su propio archivo de documentación detallando sus endpoints, configuración y lógica de negocio:

- [Auth Service](./auth-service/AUTH_SERVICE_README.md)
- [Users Service](./users-service/USERS_SERVICE_README.md)
- [Event Service](./event-service/EVENT_SERVICE_README.md)
- [Order Service](./order-service/ORDER_SERVICE_README.md)
- [Payment Service](./payment-service/PAYMENT_SERVICE_README.md)
- [Analytics Service](./analytics-service/ANALYTICS_SERVICE_README.md)

## 🔄 Flujos Principales

### Autenticación
El `auth-service` emite tokens JWT que deben ser incluidos en el header `Authorization: Bearer <token>` para las peticiones a los demás servicios protegidos.

### Compra de Tickets
1. Usuario selecciona tickets (`event-service`).
2. Se crea una orden (`order-service`).
3. Se procesa el pago (`payment-service` -> Stripe).
4. Al confirmar el pago, se generan los tickets (`event-service`).

### Validación QR (Sistema de Empleados)
Los empleados utilizan el dashboard para escanear QRs.
1. El frontend envía el código QR a `users-service`.
2. `users-service` delega la validación a `event-service`.
3. `event-service` verifica la validez y actualiza el estado (entrada o consumición).

## 📝 Notas de Desarrollo

- **Red Docker**: Todos los servicios se comunican a través de la red `packedgo-network`. Utiliza los nombres de servicio (ej. `http://auth-service:8081`) para la comunicación entre contenedores.
- **Base de Datos**: Cada servicio tiene su propia base de datos aislada para mantener el desacoplamiento.
