# 🚪 API-GATEWAY

**⚠️ Estado: EN DESARROLLO / SKELETON**

Este proyecto es un esqueleto para un futuro API Gateway basado en **Spring Cloud Gateway**.
Actualmente no tiene rutas configuradas ni lógica de enrutamiento activa.

## 📋 Descripción

El objetivo de este servicio será actuar como **punto de entrada único** para todos los microservicios de PackedGo, implementando:

### Funcionalidades Planificadas:
- ✅ **Enrutamiento Centralizado** - Redirección inteligente de peticiones
- ✅ **Autenticación Centralizada** - Validación JWT antes de enrutar
- ✅ **Rate Limiting** - Control de frecuencia de peticiones
- ✅ **Load Balancing** - Distribución de carga entre instancias
- ✅ **CORS Handling** - Gestión centralizada de CORS
- ✅ **Request/Response Logging** - Auditoría de peticiones
- ✅ **Circuit Breaker** - Resiliencia ante fallos de servicios

## 🎯 Estado Actual

| Componente | Estado | Notas |
|------------|--------|-------|
| **Estructura de Proyecto** | ✅ Completa | Generado con Spring Initializr |
| **Dependencias** | ⚠️ Básicas | Spring Boot Starter |
| **Configuración de Rutas** | ❌ Pendiente | Sin `application.yml` configurado |
| **Filtros Globales** | ❌ Pendiente | Sin GlobalFilters implementados |
| **Docker** | ❌ No incluido | No está en docker-compose.yml |
| **Tests** | ❌ Pendiente | Sin tests implementados |

## 🚀 Tecnologías Planificadas

- **Java 17**
- **Spring Boot 3.x**
- **Spring Cloud Gateway** - Enrutamiento reactivo
- **Spring Security** - Seguridad y JWT
- **Resilience4j** - Circuit breaker y rate limiting
- **Spring Cloud LoadBalancer** - Balanceo de carga

## 🏗️ Arquitectura Propuesta

```
┌─────────────┐
│   Cliente   │
│  (Angular)  │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────┐
│      API GATEWAY            │
│  (Puerto único: 8080)       │
│                             │
│  ┌──────────────────────┐  │
│  │ JWT Validation       │  │
│  │ CORS Handler         │  │
│  │ Rate Limiter         │  │
│  │ Circuit Breaker      │  │
│  └──────────────────────┘  │
└──────────┬──────────────────┘
           │
    ┌──────┴──────┬──────────┬─────────┬──────────┬──────────┐
    ▼             ▼          ▼         ▼          ▼          ▼
┌────────┐  ┌─────────┐ ┌────────┐ ┌──────┐ ┌─────────┐ ┌──────────┐
│  Auth  │  │  Users  │ │ Event  │ │Order │ │ Payment │ │Analytics │
│  8081  │  │  8082   │ │  8086  │ │ 8084 │ │  8085   │ │   8087   │
└────────┘  └─────────┘ └────────┘ └──────┘ └─────────┘ └──────────┘
```

## 📝 Rutas Planificadas

```yaml
# Ejemplo de configuración futura en application.yml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-service
          uri: http://auth-service:8081
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=1
        
        - id: users-service
          uri: http://users-service:8082
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1
        
        - id: event-service
          uri: http://event-service:8086
          predicates:
            - Path=/api/events/**
          filters:
            - StripPrefix=1
            - AuthFilter  # Custom JWT validation
```

## 🔧 Pasos Futuros

### Fase 1: Configuración Básica
- [ ] Agregar dependencias de Spring Cloud Gateway
- [ ] Configurar rutas en `application.yml`
- [ ] Definir predicados y filtros básicos
- [ ] Agregar al `docker-compose.yml`

### Fase 2: Seguridad
- [ ] Implementar `JwtAuthenticationFilter`
- [ ] Configurar CORS global
- [ ] Validar tokens antes de enrutar
- [ ] Implementar lista blanca de endpoints públicos

### Fase 3: Resiliencia
- [ ] Configurar Circuit Breaker con Resilience4j
- [ ] Implementar Rate Limiting por IP/Usuario
- [ ] Agregar fallback responses
- [ ] Configurar timeouts y retry policies

### Fase 4: Observabilidad
- [ ] Agregar logging de requests/responses
- [ ] Integrar métricas con Actuator
- [ ] Configurar health checks
- [ ] Implementar distributed tracing (opcional)

## 🐳 Configuración Docker Futura

```yaml
# Agregar al docker-compose.yml
api-gateway:
  build:
    context: ./api-gateway
    dockerfile: Dockerfile
  ports:
    - "8080:8080"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - AUTH_SERVICE_URL=http://auth-service:8081
    - USERS_SERVICE_URL=http://users-service:8082
    - EVENT_SERVICE_URL=http://event-service:8086
    - ORDER_SERVICE_URL=http://order-service:8084
    - PAYMENT_SERVICE_URL=http://payment-service:8085
    - ANALYTICS_SERVICE_URL=http://analytics-service:8087
  depends_on:
    - auth-service
    - users-service
    - event-service
  networks:
    - packedgo-network
```

## ⚙️ Servicios a Enrutar

| Servicio | Puerto | Prefijo de Ruta |
|----------|--------|----------------|
| Auth Service | 8081 | `/api/auth/**` |
| Users Service | 8082 | `/api/users/**` |
| Event Service | 8086 | `/api/events/**` |
| Order Service | 8084 | `/api/orders/**` |
| Payment Service | 8085 | `/api/payments/**` |
| Analytics Service | 8087 | `/api/analytics/**` |

## 📚 Referencias

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [Spring Cloud Gateway Samples](https://github.com/spring-cloud-samples/spring-cloud-gateway-sample)

## 💡 Beneficios Esperados

1. **Punto de Entrada Único** - Frontend solo necesita conocer un endpoint
2. **Seguridad Centralizada** - Validación JWT en un solo lugar
3. **Mejor Observabilidad** - Logs y métricas centralizadas
4. **Resiliencia** - Circuit breakers y fallbacks automáticos
5. **Escalabilidad** - Load balancing integrado
