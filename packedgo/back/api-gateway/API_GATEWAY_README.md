# API-GATEWAY

**Estado: EN DESARROLLO / SKELETON**

Este proyecto es un esqueleto para un futuro API Gateway basado en Spring Cloud Gateway (MVC).
Actualmente no tiene rutas configuradas ni lógica de enrutamiento activa.

## Descripción

El objetivo de este servicio será actuar como punto de entrada único para todos los microservicios de PackedGo, manejando:
- Enrutamiento de peticiones.
- Autenticación centralizada (opcional).
- Rate limiting.
- Balanceo de carga.

## Estado Actual

- **Código**: Estructura básica de Spring Boot generada.
- **Configuración**: Sin rutas definidas en `application.properties`.
- **Docker**: No incluido en el `docker-compose.yml` principal.

## Pasos Futuros

1. Definir rutas en `application.yml` para redirigir a:
   - `auth-service`
   - `users-service`
   - `event-service`
   - `order-service`
   - `payment-service`
   - `analytics-service`
2. Configurar filtros de seguridad (GlobalFilter).
3. Integrar con Eureka (si se decide usar Service Discovery) o usar URLs estáticas en Docker.
