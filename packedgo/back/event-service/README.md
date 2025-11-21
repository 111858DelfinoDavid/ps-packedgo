# EVENT-SERVICE

## Descripción General

El EVENT-SERVICE es el núcleo de la gestión de eventos en PackedGo. Se encarga de la creación y administración de eventos, consumiciones, tickets y la validación de accesos mediante códigos QR. Implementa la lógica de negocio principal para organizadores y el control de stock.

## Puerto de Servicio
**8086**

## Base de Datos
- **Nombre:** event_db
- **Puerto:** 5435 (PostgreSQL)
- **Tablas principales:**
  - `events` - Eventos
  - `event_categories` - Categorías de eventos
  - `consumptions` - Consumiciones (productos)
  - `consumption_categories` - Categorías de consumiciones
  - `passes` - Entradas pre-generadas
  - `tickets` - Tickets comprados por usuarios
  - `ticket_consumptions` - Paquetes de consumiciones de tickets
  - `ticket_consumption_details` - Detalle de consumiciones por ticket

## Funcionalidades Principales

### 1. Gestión de Eventos
- CRUD completo de eventos (Multi-tenant por `createdBy`)
- Gestión de categorías de eventos
- Control de capacidad y fechas

### 2. Gestión de Consumiciones
- CRUD de productos/consumiciones
- Categorización (Bebidas, Comidas, etc.)
- Asociación de precios y stock

### 3. Sistema de Tickets y Passes
- Generación de Passes (entradas disponibles)
- Emisión de Tickets (compra de usuario)
- Vinculación de consumiciones a tickets

### 4. Validación QR
- Validación de entrada al evento (Single Entry)
- Canje progresivo de consumiciones
- Control de stock en tiempo real durante el evento

## Endpoints Principales

### EventController (`/api/events`)
- `GET /` - Listar eventos (público)
- `GET /{id}` - Obtener evento por ID
- `POST /` - Crear evento (Admin)
- `PUT /{id}` - Actualizar evento
- `DELETE /{id}` - Eliminar evento
- `GET /organizer/my-events` - Eventos del organizador logueado

### ConsumptionController (`/api/consumptions`)
- `GET /` - Listar consumiciones
- `POST /` - Crear consumición
- `PUT /{id}` - Actualizar consumición
- `DELETE /{id}` - Eliminar consumición

### TicketController (`/api/tickets`)
- `POST /` - Crear ticket (compra)
- `GET /{id}` - Obtener ticket
- `GET /user/{userId}` - Tickets de un usuario

### QRValidationController (`/api/qr-validation`)
- `POST /validate-entry` - Validar entrada (usado por users-service)
- `POST /validate-consumption` - Validar/Canjear consumición (usado por users-service)

### PassController (`/api/passes`)
- `POST /generate-for-event/{eventId}` - Generar passes para un evento
- `GET /event/{eventId}` - Obtener passes de un evento

## Entities Principales

### Event
```java
@Entity
@Table(name = "events")
public class Event {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime eventDate;
    private Double lat;
    private Double lng;
    private Integer maxCapacity;
    private BigDecimal basePrice;
    private Long createdBy; // ID del organizador (Multi-tenant)
    // ...
}
```

### Ticket
Representa la entrada comprada por un usuario. Contiene el estado `redeemed` para controlar el acceso.

### TicketConsumptionDetail
Controla el saldo de cada consumición individual dentro de un ticket.

## Variables de Entorno

```bash
# Server Configuration
SERVER_PORT=8086

# Database Configuration
DATABASE_URL=jdbc:postgresql://event-db:5432/event_db
DATABASE_USER=event_user
DATABASE_PASSWORD=event_password

# Security
JWT_SECRET=your_jwt_secret
```

## Dependencias con Otros Servicios

### Users-Service
- **Inbound:** Recibe peticiones de validación QR desde el dashboard de empleados.

### Order-Service
- **Inbound:** Recibe peticiones para generar tickets tras una compra exitosa.

## Validaciones y Reglas de Negocio

### Multi-Tenancy
- Los eventos y consumiciones están vinculados al `createdBy` (ID del organizador).
- Un organizador solo puede modificar sus propios recursos.

### Validación QR
- **Entrada:** Solo se permite un ingreso por ticket (`redeemed` flag).
- **Consumo:** Se decrementa la cantidad disponible. No se permite canjear si el saldo es 0.
