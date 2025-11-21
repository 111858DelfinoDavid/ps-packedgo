# PackedGo - Sistema de Gestión de Eventos
## Documentación Técnica Actualizada 2025

**Fecha de actualización:** 19 de noviembre de 2025  
**Versión del sistema:** 1.0  
**Branch actual:** feature/employee-dashboard

---

## 📋 Índice

1. [Descripción General](#descripción-general)
2. [Arquitectura del Sistema](#arquitectura-del-sistema)
3. [Microservicios](#microservicios)
4. [Stack Tecnológico](#stack-tecnológico)
5. [Sistema de Autenticación](#sistema-de-autenticación)
6. [Modelo de Datos](#modelo-de-datos)
7. [Sistema de Pagos](#sistema-de-pagos)
8. [Frontend](#frontend)
9. [Configuración y Despliegue](#configuración-y-despliegue)
10. [Guía de Desarrollo](#guía-de-desarrollo)

---

## 🎯 Descripción General

**PackedGo** es una plataforma SaaS Multi-Tenant de gestión de eventos con sistema de consumiciones prepagadas. Permite a múltiples organizadores de eventos independientes operar simultáneamente en la plataforma, mientras los clientes pueden comprar entradas personalizadas con paquetes de consumiciones.

### Características Principales

- **Multi-Tenant:** Múltiples organizadores operan de forma independiente con aislamiento de datos
- **Paquetes Personalizados:** Los clientes construyen paquetes combinando entradas + consumiciones
- **Sistema de Pagos:** Integración completa con Stripe para procesamiento seguro de pagos
- **Validación QR:** Sistema de códigos QR únicos para entrada y consumo en eventos
- **Panel de Analytics:** Métricas en tiempo real, reportes y dashboards para organizadores
- **Sistema de Empleados:** Los organizadores pueden asignar empleados a eventos para validación

### Usuarios del Sistema

| Rol | Login | Permisos |
|-----|-------|----------|
| **ADMIN** (Organizador) | email + contraseña | Gestiona sus propios eventos, consumiciones, empleados y analytics |
| **CUSTOMER** (Cliente) | DNI + contraseña | Explora eventos, compra tickets, visualiza sus compras |
| **EMPLOYEE** (Empleado) | email + contraseña | Valida tickets y consumiciones en eventos asignados |

---

## 🏗️ Arquitectura del Sistema

### Patrón Arquitectónico

**Microservicios con Database per Service**

- Cada microservicio es independiente y auto-contenido
- Comunicación síncrona vía REST APIs
- Base de datos PostgreSQL dedicada por servicio
- Aislamiento lógico multi-tenant mediante campo `createdBy`

### Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                      Frontend Angular 19                         │
│                     (Puerto 4200 / 3000)                         │
└────────────┬────────────────────────────────────────────────────┘
             │
             │ HTTP/REST + JWT
             │
┌────────────▼────────────────────────────────────────────────────┐
│                    API Gateway (Nginx)                           │
│                        Puerto 8080                               │
│                     [PENDIENTE]                                  │
└────────────┬────────────────────────────────────────────────────┘
             │
    ┌────────┼──────────┬──────────┬──────────┬──────────┐
    │        │          │          │          │          │
┌───▼───┐ ┌─▼────┐ ┌──▼────┐ ┌──▼────┐ ┌──▼────┐ ┌───▼────┐
│ Auth  │ │Users │ │ Event │ │ Order │ │Payment│ │Analytics│
│:8081  │ │:8082 │ │ :8086 │ │ :8084 │ │ :8085 │ │  :8087  │
└───┬───┘ └──┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └────┬───┘
    │        │         │         │         │          │
┌───▼────┐┌──▼────┐┌───▼────┐┌───▼────┐┌───▼────┐┌────▼────┐
│auth_db ││users  ││event   ││order   ││payment ││analytics│
│:5433   ││_db    ││_db     ││_db     ││_db     ││_db      │
│        ││:5434  ││:5435   ││:5436   ││:5437   ││:5439    │
└────────┘└───────┘└────────┘└────────┘└────────┘└─────────┘
```

---

## 📦 Microservicios

### 1. Auth Service (Puerto 8081)

**Responsabilidad:** Autenticación y autorización centralizada

**Funcionalidades:**
- Login diferenciado por tipo de usuario (Admin/Customer/Employee)
- Generación y validación de tokens JWT
- Gestión de sesiones de usuario
- Control de intentos fallidos de login
- Recuperación de contraseñas (pendiente activación)
- Verificación de email (pendiente activación)

**Base de Datos:** auth_db (Puerto 5433)

**Entidades Principales:**
- `AuthUser`: Usuarios del sistema
- `UserSession`: Sesiones activas
- `LoginAttempt`: Registro de intentos de login
- `EmailVerificationToken`: Tokens de verificación
- `PasswordRecoveryToken`: Tokens de recuperación

**Endpoints Clave:**
```
POST /auth/admin/register
POST /auth/admin/login
POST /auth/customer/register
POST /auth/customer/login
POST /auth/employee/login
POST /auth/validate-token
POST /auth/logout
```

---

### 2. Users Service (Puerto 8082)

**Responsabilidad:** Gestión de perfiles de usuario y empleados

**Funcionalidades:**
- Perfiles de administradores (organizadores)
- Perfiles de clientes (consumidores)
- Sistema completo de empleados
- Asignación de empleados a eventos
- Validación de credenciales de empleados
- Gestión de datos personales

**Base de Datos:** users_db (Puerto 5434)

**Entidades Principales:**
- `UserProfile`: Perfiles de usuarios
- `Employee`: Empleados de organizadores
- Relación Many-to-Many: Employee ↔ Events

**Endpoints Clave:**
```
GET /api/users/profile
PUT /api/users/profile
POST /api/admin/employees
GET /api/admin/employees
PUT /api/admin/employees/{id}
DELETE /api/admin/employees/{id}
POST /api/admin/employees/{id}/assign-events
GET /api/employee/assigned-events
POST /api/employee/validate-ticket
POST /api/employee/register-consumption
GET /api/employee/stats
POST /api/internal/employees/validate
```

---

### 3. Event Service (Puerto 8086)

**Responsabilidad:** Gestión de eventos, consumiciones y tickets con seguridad multi-tenant

**Funcionalidades:**
- CRUD de eventos con aislamiento por organizador
- Gestión de categorías de eventos
- CRUD de consumiciones (bebidas, comidas, extras)
- Gestión de categorías de consumiciones
- Generación automática de Passes (entradas pre-generadas)
- Sistema de Tickets (asociación Pass + Usuario + Consumiciones)
- Control de stock en tiempo real
- Validación de propiedad de recursos (campo `createdBy`)

**Base de Datos:** event_db (Puerto 5435)

**Modelo de Datos:**
```
EventCategory
    ↓
Event (createdBy: Long) ← Multi-tenant
    ↓
Pass (código único, estado: available/sold)
    ↓
Ticket (userId, passId, consumiciones)
    ↓
TicketConsumption (paquete de consumiciones)
    ↓
TicketConsumptionDetail (detalle individual)
    ↑
Consumption (createdBy: Long) ← Multi-tenant
ConsumptionCategory
```

**Entidades Principales:**
- `Event`: Eventos creados por organizadores
- `EventCategory`: Categorías de eventos
- `Pass`: Entradas pre-generadas con código único
- `Ticket`: Compra de entrada por usuario
- `TicketConsumption`: Paquete de consumiciones del ticket
- `TicketConsumptionDetail`: Detalle de cada consumición
- `Consumption`: Consumiciones globales del organizador
- `ConsumptionCategory`: Categorías de consumiciones

**Endpoints Clave:**
```
# Eventos
GET /api/events
GET /api/events/{id}
POST /api/events (Admin)
PUT /api/events/{id} (Admin + validación createdBy)
DELETE /api/events/{id} (Admin + validación createdBy)

# Consumiciones
GET /api/consumptions
GET /api/consumptions/{id}
POST /api/consumptions (Admin)
PUT /api/consumptions/{id} (Admin + validación createdBy)
DELETE /api/consumptions/{id} (Admin + validación createdBy)

# Passes
POST /api/passes/generate-for-event/{eventId}
GET /api/passes/event/{eventId}

# Tickets
POST /api/tickets
GET /api/tickets/{id}
GET /api/tickets/user/{userId}

# Validación QR
POST /api/qr/validate-entry
POST /api/qr/validate-consumption
```

**Seguridad Multi-Tenant:**
- Campo `createdBy` en Event y Consumption
- Extracción automática de `userId` del JWT
- Validación de propiedad en operaciones UPDATE/DELETE
- Filtrado automático por organizador en queries

---

### 4. Order Service (Puerto 8084)

**Responsabilidad:** Gestión de carritos de compra y órdenes

**Funcionalidades:**
- Carrito de compras por usuario
- Gestión de items del carrito
- Soporte para múltiples eventos en una orden
- Proceso de checkout
- Creación de órdenes pendientes de pago
- Confirmación de órdenes post-pago
- Limpieza automática de carritos abandonados

**Base de Datos:** order_db (Puerto 5436)

**Entidades Principales:**
- `ShoppingCart`: Carrito de usuario
- `CartItem`: Items individuales del carrito (evento + entrada)
- `CartItemConsumption`: Consumiciones por item
- `Order`: Orden de compra
- `OrderItem`: Items de la orden
- `OrderItemConsumption`: Consumiciones de cada item

**Flujo de Compra:**
```
1. Usuario agrega evento al carrito (POST /api/cart/items)
2. Usuario agrega consumiciones al item (POST /api/cart/items/{id}/consumptions)
3. Usuario procede al checkout (POST /api/orders/checkout)
   → Crea Order con status PENDING_PAYMENT
   → Llama a payment-service para crear pago
   → Retorna checkout URL de Stripe
4. Usuario completa pago en Stripe
5. Stripe envía webhook a payment-service
6. Payment-service notifica a order-service (POST /api/orders/{id}/confirm-payment)
   → Actualiza Order a PAID
   → Genera tickets en event-service
7. Limpia el carrito
```

**Endpoints Clave:**
```
# Carrito
GET /api/cart
POST /api/cart/items
PUT /api/cart/items/{id}
DELETE /api/cart/items/{id}
POST /api/cart/items/{id}/consumptions
DELETE /api/cart/clear

# Órdenes
POST /api/orders/checkout
GET /api/orders/user/{userId}
GET /api/orders/{id}
POST /api/orders/{id}/confirm-payment (interno)
```

---

### 5. Payment Service (Puerto 8085)

**Responsabilidad:** Procesamiento de pagos con Stripe

**Funcionalidades:**
- Integración completa con Stripe SDK
- Creación de checkout sessions
- Manejo de webhooks de Stripe
- Verificación de firmas de webhooks
- Gestión de estados de pago
- Notificación a order-service post-pago
- Prevención de pagos duplicados

**Base de Datos:** payment_db (Puerto 5437)

**Entidad Principal:**
- `Payment`: Registro de transacciones
  - `orderId`: Referencia a orden
  - `adminId`: Organizador del evento
  - `amount`: Monto del pago
  - `currency`: Moneda (ARS por defecto)
  - `status`: PENDING, APPROVED, REJECTED, CANCELLED
  - `stripeSessionId`: ID de sesión de Stripe
  - `stripePaymentIntentId`: ID de payment intent
  - `paymentProvider`: "STRIPE"

**Estados de Pago:**
- `PENDING`: Esperando confirmación
- `APPROVED`: Pago confirmado
- `REJECTED`: Pago rechazado
- `CANCELLED`: Pago cancelado

**Endpoints Clave:**
```
POST /api/payments/create
GET /api/payments/{id}
GET /api/payments/order/{orderId}
POST /api/webhooks/stripe (público)
```

**Integración Stripe:**
```java
// Configuración
@Value("${stripe.secret.key}")
private String stripeSecretKey;

@Value("${stripe.webhook.secret}")
private String webhookSecret;

// Creación de checkout session
SessionCreateParams params = SessionCreateParams.builder()
    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
    .setMode(SessionCreateParams.Mode.PAYMENT)
    .setSuccessUrl(successUrl)
    .setCancelUrl(cancelUrl)
    .addLineItem(lineItem)
    .build();

Session session = Session.create(params);

// Verificación de webhook
String payload = request.body();
String sigHeader = request.getHeader("Stripe-Signature");
Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
```

---

### 6. Analytics Service (Puerto 8087)

**Responsabilidad:** Métricas, reportes y dashboards para organizadores

**Funcionalidades:**
- Dashboard con KPIs principales
- Cálculo de ingresos totales (tickets + consumiciones)
- Estadísticas de ventas por evento
- Top eventos más vendidos
- Top consumiciones más populares
- Tendencias diarias de los últimos 7 días
- Cálculo de tasa de ocupación
- Crecimiento mensual
- Filtrado por organizador (multi-tenant)

**Base de Datos:** analytics_db (Puerto 5439)

**Métricas Calculadas:**
- Ingresos totales
- Ingresos por tickets
- Ingresos por consumiciones
- Cantidad de tickets vendidos
- Cantidad de eventos activos
- Tasa de ocupación promedio
- Crecimiento mes a mes

**Endpoints Clave:**
```
GET /api/api/dashboard
GET /api/api/dashboard/organizer/{organizerId}
GET /api/health
```

**Estructura de Respuesta:**
```json
{
  "summary": {
    "totalRevenue": "BigDecimal",
    "ticketsSold": "Integer",
    "activeEvents": "Integer",
    "averageOccupancy": "Double"
  },
  "revenueBreakdown": {
    "ticketRevenue": "BigDecimal",
    "consumptionRevenue": "BigDecimal",
    "percentageTickets": "Double",
    "percentageConsumptions": "Double"
  },
  "monthlyGrowth": {
    "currentMonth": "BigDecimal",
    "previousMonth": "BigDecimal",
    "growthPercentage": "Double"
  },
  "topEvents": [...],
  "topConsumptions": [...],
  "dailyTrends": [...]
}
```

---

### 7. API Gateway (Puerto 8080)

**Estado:** ❌ PENDIENTE DE IMPLEMENTACIÓN

**Objetivo:** Nginx como punto de entrada único para enrutar requests a microservicios

---

## 🛠️ Stack Tecnológico

### Backend

| Tecnología | Versión | Uso |
|------------|---------|-----|
| **Java** | 17 | Lenguaje principal |
| **Spring Boot** | 3.5.6 / 3.5.7 | Framework de microservicios |
| **Spring Security** | 6.x | Seguridad y autenticación |
| **Spring Data JPA** | - | Persistencia de datos |
| **PostgreSQL** | 15-alpine | Base de datos relacional |
| **JWT (jjwt)** | 0.12.6 | Tokens de autenticación |
| **Stripe SDK** | 26.7.0 | Procesamiento de pagos |
| **Lombok** | - | Reducción de boilerplate |
| **BCrypt** | - | Hash de contraseñas |
| **Docker** | - | Containerización |

### Frontend

| Tecnología | Versión | Uso |
|------------|---------|-----|
| **Angular** | 19.2.0 | Framework SPA |
| **TypeScript** | 5.7.2 | Lenguaje tipado |
| **Bootstrap** | 5.3.8 | UI Framework |
| **SweetAlert2** | 11.26.3 | Alertas y modales |
| **ZXing** | 20.0.0 | Scanner QR |
| **RxJS** | 7.8.0 | Programación reactiva |

### DevOps

- **Docker Compose**: Orquestación de servicios
- **Git**: Control de versiones
- **Maven**: Gestión de dependencias Java
- **npm**: Gestión de dependencias Node

---

## 🔐 Sistema de Autenticación

### Arquitectura JWT

```
1. Usuario envía credenciales → Auth Service
2. Auth Service valida → Users Service
3. Auth Service genera JWT con:
   - userId
   - role (ADMIN/CUSTOMER/EMPLOYEE)
   - authorities
   - exp (24h)
4. Frontend almacena JWT en localStorage
5. Frontend incluye JWT en header Authorization
6. Cada microservicio valida JWT con JwtTokenValidator
```

### Estructura del Token JWT

```json
{
  "sub": "user@email.com",
  "userId": 1,
  "role": "ADMIN",
  "authorities": ["ROLE_ADMIN"],
  "iat": 1700000000,
  "exp": 1700086400
}
```

### Flujos de Login

#### Admin (Organizador)
```
POST /auth/admin/login
{
  "email": "organizer@example.com",
  "password": "Password123!"
}

→ Retorna JWT con role: ADMIN
```

#### Customer (Cliente)
```
POST /auth/customer/login
{
  "document": 12345678,
  "password": "Password123!"
}

→ Retorna JWT con role: CUSTOMER
```

#### Employee (Empleado)
```
POST /auth/employee/login
{
  "email": "employee@example.com",
  "password": "Password123!"
}

→ Retorna JWT con role: EMPLOYEE
```

### Validación de Tokens

Cada microservicio incluye `JwtTokenValidator`:

```java
@Component
public class JwtTokenValidator {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        return claims.get("userId", Long.class);
    }
}
```

---

## 📊 Modelo de Datos

### Relaciones Principales

```
AuthUser (auth_db)
    ↓ (userId)
UserProfile (users_db)
    ↓ (adminId/userId)
Event (event_db) ← createdBy
    ↓
Pass
    ↓
Ticket
    ↓
TicketConsumption
    ↓
TicketConsumptionDetail
    ↑
Consumption ← createdBy
    
ShoppingCart (order_db)
    ↓
CartItem → Event
    ↓
CartItemConsumption → Consumption
    
Order (order_db)
    ↓
OrderItem
    ↓
OrderItemConsumption
    
Payment (payment_db)
    ↓ (orderId)
Order
```

### Campos Críticos Multi-Tenant

| Entidad | Campo | Descripción |
|---------|-------|-------------|
| Event | `createdBy` | ID del organizador propietario |
| Consumption | `createdBy` | ID del organizador propietario |
| Employee | `adminId` | ID del admin que lo creó |
| Payment | `adminId` | ID del organizador del evento |
| Order | `adminId` | ID del organizador del evento |

### Versionamiento Optimista

Las entidades críticas usan `@Version` para evitar conflictos:

```java
@Entity
public class Event {
    @Version
    private Long version;
    
    // Evita condiciones de carrera en actualizaciones concurrentes
}
```

---

## 💳 Sistema de Pagos

### Proveedor: Stripe

**Configuración Requerida:**

```properties
# .env del payment-service
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
```

### Flujo Completo de Pago

```
1. Customer → Frontend: Completa carrito

2. Frontend → Order Service: POST /api/orders/checkout
   {
     "userId": 1,
     "items": [...]
   }

3. Order Service:
   - Valida items
   - Calcula total
   - Crea Order (status: PENDING_PAYMENT)
   
4. Order Service → Payment Service: POST /api/payments/create
   {
     "orderId": "ORD-123",
     "amount": 5000.00,
     "adminId": 2
   }

5. Payment Service → Stripe API: Create Checkout Session
   
6. Stripe → Payment Service: Returns session
   {
     "sessionId": "cs_test_...",
     "checkoutUrl": "https://checkout.stripe.com/..."
   }

7. Payment Service → Order Service: Returns checkout URL

8. Order Service → Frontend: Returns checkout URL

9. Frontend: Redirige a Stripe Checkout

10. Customer: Completa pago en Stripe

11. Stripe → Payment Service: POST /api/webhooks/stripe
    Event: checkout.session.completed
    
12. Payment Service:
    - Verifica firma del webhook
    - Actualiza Payment (status: APPROVED)
    
13. Payment Service → Order Service: POST /api/orders/{id}/confirm-payment

14. Order Service:
    - Actualiza Order (status: PAID)
    
15. Order Service → Event Service: POST /api/tickets
    - Genera tickets con QR codes
    
16. Frontend: Redirige a /customer/orders/success
```

### Webhook de Stripe

```java
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleStripeWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String sigHeader) {
    
    try {
        Event event = Webhook.constructEvent(
            payload, sigHeader, webhookSecret
        );
        
        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                .getObject().orElseThrow();
            
            paymentService.handleStripePaymentSuccess(session.getId());
        }
        
        return ResponseEntity.ok("Webhook processed");
    } catch (SignatureVerificationException e) {
        return ResponseEntity.status(400).body("Invalid signature");
    }
}
```

### Testing Local de Webhooks

```bash
# Instalar Stripe CLI
# Windows
scoop install stripe

# Ejecutar listener
stripe listen --forward-to localhost:8085/api/webhooks/stripe

# Obtener webhook secret
stripe listen --print-secret
# Copiar whsec_... al .env
```

---

## 🎨 Frontend

### Estructura de Carpetas

```
src/app/
├── core/
│   ├── guards/
│   │   ├── auth.guard.ts
│   │   ├── admin.guard.ts
│   │   ├── employee.guard.ts
│   │   └── email-verified.guard.ts
│   ├── services/
│   │   ├── auth.service.ts
│   │   ├── event.service.ts
│   │   ├── consumption.service.ts
│   │   ├── cart.service.ts
│   │   ├── order.service.ts
│   │   ├── payment.service.ts
│   │   ├── analytics.service.ts
│   │   └── employee.service.ts
│   └── interceptors/
│       └── auth.interceptor.ts
├── features/
│   ├── admin/
│   │   ├── admin-dashboard/
│   │   ├── events-management/
│   │   ├── consumptions-management/
│   │   ├── employee-management/
│   │   └── admin-analytics/
│   ├── customer/
│   │   ├── customer-dashboard/
│   │   ├── event-detail/
│   │   ├── checkout/
│   │   └── order-success/
│   ├── employee/
│   │   └── employee-dashboard/
│   ├── auth/
│   │   ├── admin-login/
│   │   ├── admin-register/
│   │   ├── customer-login/
│   │   ├── customer-register/
│   │   └── employee-login/
│   ├── landing/
│   └── terms/
└── shared/
    ├── components/
    └── models/
```

### Rutas Principales

| Ruta | Componente | Guard | Descripción |
|------|------------|-------|-------------|
| `/` | LandingComponent | - | Página de inicio |
| `/admin/login` | AdminLoginComponent | - | Login de organizadores |
| `/admin/register` | AdminRegisterComponent | - | Registro de organizadores |
| `/admin/dashboard` | AdminDashboardComponent | admin + emailVerified | Dashboard de admin |
| `/admin/events` | EventsManagementComponent | admin + emailVerified | Gestión de eventos |
| `/admin/consumptions` | ConsumptionsManagementComponent | admin + emailVerified | Gestión de consumiciones |
| `/admin/employees` | EmployeeManagementComponent | admin + emailVerified | Gestión de empleados |
| `/admin/analytics` | AdminAnalyticsComponent | admin + emailVerified | Dashboard de analytics |
| `/customer/login` | CustomerLoginComponent | - | Login de clientes |
| `/customer/register` | CustomerRegisterComponent | - | Registro de clientes |
| `/customer/dashboard` | CustomerDashboardComponent | auth + emailVerified | Dashboard de cliente |
| `/customer/events/:id` | EventDetailComponent | auth + emailVerified | Detalle del evento |
| `/customer/checkout` | CheckoutComponent | auth + emailVerified | Proceso de pago |
| `/customer/orders/success` | OrderSuccessComponent | auth + emailVerified | Confirmación de orden |
| `/employee/login` | EmployeeLoginComponent | - | Login de empleados |
| `/employee/dashboard` | EmployeeDashboardComponent | employee + emailVerified | Dashboard de empleado |

### Guards

```typescript
// auth.guard.ts - Requiere estar autenticado
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  if (authService.isAuthenticated()) {
    return true;
  }
  
  router.navigate(['/admin/login']);
  return false;
};

// admin.guard.ts - Requiere rol ADMIN
export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  if (authService.isAuthenticated() && authService.hasRole('ADMIN')) {
    return true;
  }
  
  router.navigate(['/admin/login']);
  return false;
};

// employee.guard.ts - Requiere rol EMPLOYEE
export const employeeGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  
  if (authService.isAuthenticated() && authService.hasRole('EMPLOYEE')) {
    return true;
  }
  
  router.navigate(['/employee/login']);
  return false;
};
```

### Servicios HTTP

```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = environment.authServiceUrl;
  
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/admin/login`, credentials)
      .pipe(
        tap(response => {
          localStorage.setItem('token', response.token);
          localStorage.setItem('user', JSON.stringify(response.user));
        })
      );
  }
  
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.router.navigate(['/admin/login']);
  }
  
  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }
  
  getToken(): string | null {
    return localStorage.getItem('token');
  }
}
```

### Interceptor de Autenticación

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  
  if (token) {
    const cloned = req.clone({
      headers: req.headers.set('Authorization', `Bearer ${token}`)
    });
    return next(cloned);
  }
  
  return next(req);
};
```

---

## 🚀 Configuración y Despliegue

### Requisitos Previos

- Docker >= 20.10
- Docker Compose >= 2.0
- Java 17 (para desarrollo local)
- Node.js 18+ (para desarrollo frontend)
- Maven 3.8+ (para compilación local)
- PostgreSQL 15 (opcional, si no se usa Docker)

### Despliegue con Docker Compose

#### 1. Clonar el repositorio

```bash
git clone https://github.com/111858DelfinoDavid/ps-packedgo.git
cd ps-packedgo
```

#### 2. Configurar variables de entorno

Cada microservicio necesita un archivo `.env`:

```bash
# Ejemplo: payment-service/.env
cd packedgo/back/payment-service
cp .env.example .env
```

**Variables críticas:**

```properties
# Auth Service
JWT_SECRET=mySecretKey123456789PackedGoAuth2025VerySecureKey
JWT_EXPIRATION=86400000

# Payment Service
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
ORDER_SERVICE_URL=http://order-service:8084

# Users Service
EVENT_SERVICE_URL=http://event-service:8086/api

# Order Service
PAYMENT_SERVICE_URL=http://payment-service:8085
EVENT_SERVICE_URL=http://event-service:8086/api
```

#### 3. Levantar servicios

```bash
cd packedgo/back
docker-compose up --build
```

Esto iniciará:
- 6 microservicios
- 6 bases de datos PostgreSQL
- Red Docker: `packedgo-network`

#### 4. Verificar servicios

```bash
# Auth Service
curl http://localhost:8081/actuator/health

# Users Service
curl http://localhost:8082/actuator/health

# Event Service
curl http://localhost:8086/actuator/health

# Order Service
curl http://localhost:8084/actuator/health

# Payment Service
curl http://localhost:8085/actuator/health

# Analytics Service
curl http://localhost:8087/api/health
```

#### 5. Levantar Frontend

```bash
cd packedgo/front-angular
npm install
npm start
```

Frontend disponible en: http://localhost:4200

### Desarrollo Local (sin Docker)

#### Backend

Cada microservicio puede ejecutarse independientemente:

```bash
cd packedgo/back/auth-service
./mvnw spring-boot:run

# En otra terminal
cd packedgo/back/users-service
./mvnw spring-boot:run

# Repetir para cada servicio
```

**Nota:** Asegurarse de tener PostgreSQL corriendo localmente y crear las bases de datos:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE users_db;
CREATE DATABASE event_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE analytics_db;
```

#### Frontend

```bash
cd packedgo/front-angular
npm install
ng serve
```

### Puertos Utilizados

| Servicio | Puerto Aplicación | Puerto BD | Puerto Debug |
|----------|-------------------|-----------|--------------|
| Auth Service | 8081 | 5433 | 5005 |
| Users Service | 8082 | 5434 | 5006 |
| Event Service | 8086 | 5435 | 5007 |
| Order Service | 8084 | 5436 | 5008 |
| Payment Service | 8085 | 5437 | 5009 |
| Analytics Service | 8087 | 5439 | - |
| Frontend | 4200 | - | - |

---

## 🛠️ Guía de Desarrollo

### Flujo de Trabajo Git

```bash
# Crear feature branch
git checkout -b feature/nueva-funcionalidad

# Hacer cambios
git add .
git commit -m "feat: descripción del cambio"

# Push a GitHub
git push origin feature/nueva-funcionalidad

# Crear Pull Request en GitHub
```

### Estructura de Commits

Seguir Conventional Commits:

```
feat: Nueva funcionalidad
fix: Corrección de bug
docs: Cambios en documentación
style: Formato, punto y coma, etc
refactor: Refactorización de código
test: Agregar tests
chore: Mantenimiento
```

### Testing

#### Backend (JUnit)

```java
@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldCreateEvent() throws Exception {
        String eventJson = """
            {
                "name": "Test Event",
                "description": "Test Description",
                "eventDate": "2025-12-31T20:00:00"
            }
            """;
        
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Event"));
    }
}
```

#### Frontend (Jasmine/Karma)

```typescript
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });
  
  it('should login admin successfully', () => {
    const mockResponse = {
      token: 'jwt-token',
      user: { id: 1, email: 'admin@test.com' }
    };
    
    service.login({ email: 'admin@test.com', password: 'pass' })
      .subscribe(response => {
        expect(response.token).toBe('jwt-token');
      });
    
    const req = httpMock.expectOne(`${service.apiUrl}/auth/admin/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);
  });
});
```

### Debugging

#### Backend con IntelliJ IDEA

1. Run > Edit Configurations
2. Add New Configuration > Remote JVM Debug
3. Port: 5005 (para auth-service), 5006 (users), etc.
4. Start debugging

#### Frontend con VS Code

```json
// .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "chrome",
      "request": "launch",
      "name": "Launch Chrome against localhost",
      "url": "http://localhost:4200",
      "webRoot": "${workspaceFolder}/packedgo/front-angular"
    }
  ]
}
```

### Logs

```bash
# Ver logs de un servicio específico
docker logs -f back-auth-service-1

# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de PostgreSQL
docker logs -f back-auth-db-1
```

---

## 📝 Notas Adicionales

### Seguridad Multi-Tenant

**Validación Obligatoria en Controladores:**

```java
@PutMapping("/{id}")
public ResponseEntity<EventDTO> updateEvent(
    @PathVariable Long id,
    @RequestBody UpdateEventDTO dto,
    @RequestHeader("Authorization") String authHeader) {
    
    String token = authHeader.substring(7);
    Long organizerId = jwtTokenValidator.getUserIdFromToken(token);
    
    // Buscar evento
    Event event = eventRepository.findById(id).orElseThrow();
    
    // VALIDAR PROPIEDAD
    if (!event.getCreatedBy().equals(organizerId)) {
        throw new UnauthorizedException("No puedes modificar eventos de otros organizadores");
    }
    
    // Proceder con actualización
    return ResponseEntity.ok(eventService.update(id, dto));
}
```

### Manejo de Errores

Todos los microservicios implementan `@ControllerAdvice`:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            404,
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(404).body(error);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponse error = new ErrorResponse(
            403,
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(403).body(error);
    }
}
```

### Performance

**Optimizaciones Implementadas:**

1. **Lazy Loading:** Relaciones `@OneToMany` con `FetchType.LAZY`
2. **Optimistic Locking:** `@Version` en entidades críticas
3. **Indexes:** Índices en campos de búsqueda frecuente
4. **Connection Pooling:** HikariCP por defecto en Spring Boot
5. **Caching:** Preparado para Redis (pendiente implementación)

### Próximos Pasos

- [ ] Implementar API Gateway con Nginx
- [ ] Activar sistema de verificación de email
- [ ] Implementar recuperación de contraseñas
- [ ] Agregar tests unitarios e integración
- [ ] Implementar caching con Redis
- [ ] Documentar APIs con Swagger/OpenAPI
- [ ] Migrar a Kubernetes
- [ ] Deploy en cloud (AWS/Azure)
- [ ] Implementar CI/CD con GitHub Actions
- [ ] Monitoreo con Prometheus + Grafana

---

## 👥 Autores

**David Elías Delfino** - Legajo 111858  
**Agustín Luparia Mothe** - Legajo 113973

**Institución:** Universidad Tecnológica Nacional - Facultad Regional Córdoba  
**Carrera:** Tecnicatura Universitaria en Programación  
**Proyecto:** Trabajo Final Integrador 2025

---

## 📄 Licencia

Este proyecto es parte de un trabajo académico de la UTN FRC.

---

**Última actualización:** 19 de noviembre de 2025  
**Versión:** 1.0
