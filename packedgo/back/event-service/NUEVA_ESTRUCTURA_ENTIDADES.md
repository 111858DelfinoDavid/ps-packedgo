# Nueva Estructura de Entidades - Event Service

## 📋 Resumen de Cambios

Se ha implementado una nueva estructura de entidades que incluye **Event**, **Pass**, y **Ticket** para manejar la venta y canje de tickets de eventos.

## 🏗️ Estructura de Entidades

### 1. **Event** (Actualizada)
- **Tabla**: `events`
- **Funcionalidad**: Evento que determina la cantidad fija de Pass disponibles
- **Nuevos campos**:
  - `totalPasses`: Cantidad total de passes creados
  - `availablePasses`: Cantidad de passes disponibles para venta
  - `soldPasses`: Cantidad de passes vendidos
  - `version`: Control de concurrencia optimista
- **Relación**: One-to-Many con `Pass`

### 2. **Pass** (Nueva)
- **Tabla**: `passes`
- **Funcionalidad**: Código único para cada entrada al evento
- **Campos**:
  - `id`: Identificador único
  - `code`: Código único del pass (único en la BD)
  - `event`: Relación Many-to-One con Event
  - `active`: Estado activo/inactivo
  - `available`: Disponible para venta
  - `sold`: Vendido
  - `soldToUserId`: ID del usuario que lo compró
  - `soldAt`: Fecha de venta
  - `version`: Control de concurrencia optimista

### 3. **Ticket** (Nueva)
- **Tabla**: `tickets`
- **Funcionalidad**: Conecta Pass, User y TicketConsumption
- **Campos**:
  - `id`: Identificador único
  - `userId`: ID del usuario comprador (referencia externa)
  - `pass`: Relación One-to-One con Pass
  - `ticketConsumption`: Relación One-to-One con TicketConsumption
  - `active`: Estado activo/inactivo
  - `redeemed`: Canjeado
  - `redeemedAt`: Fecha de canje
  - `version`: Control de concurrencia optimista

### 4. **TicketConsumption** (Renombrada)
- **Tabla**: `consumption_tickets` (antes `tickets`)
- **Funcionalidad**: Maneja los consumos asociados al ticket
- **Sin cambios en campos**, solo cambio de nombre de tabla

### 5. **TicketConsumptionDetail** (Actualizada)
- **Tabla**: `ticket_consumption_details`
- **Cambio**: Columna `ticket_id` → `consumption_ticket_id`
- **Funcionalidad**: Detalles individuales de consumo que se pueden canjear

## 🔗 Relaciones

```
Event (1) ←→ (N) Pass (1) ←→ (1) Ticket (1) ←→ (1) TicketConsumption (1) ←→ (N) TicketConsumptionDetail
```

### Flujo de Datos:
1. **Event** define cantidad fija de **Pass**
2. **Pass** tiene código único y puede venderse
3. **Ticket** conecta **Pass** vendido + **User** + **TicketConsumption**
4. **TicketConsumption** contiene los consumos comprados
5. **TicketConsumptionDetail** permite canjear consumos individuales

## 🚀 APIs Implementadas

### PassController (`/api/event-service/passes`)
- `POST /` - Crear pass
- `POST /event/{eventId}` - Crear pass para evento
- `PUT /{passId}/sell` - Vender pass
- `PUT /code/{passCode}/sell` - Vender pass por código
- `GET /{passId}` - Obtener pass por ID
- `GET /code/{passCode}` - Obtener pass por código
- `GET /event/{eventId}` - Obtener passes por evento
- `GET /event/{eventId}/available` - Obtener passes disponibles
- `GET /event/{eventId}/sold` - Obtener passes vendidos
- `GET /user/{userId}` - Obtener passes por usuario
- `GET /event/{eventId}/available/count` - Contar passes disponibles
- `GET /event/{eventId}/sold/count` - Contar passes vendidos
- `GET /event/{eventId}/has-available` - Verificar disponibilidad

### TicketController (`/api/event-service/tickets`)
- `POST /` - Crear ticket
- `POST /purchase` - Comprar ticket
- `PUT /{ticketId}/redeem` - Canjear ticket
- `GET /{ticketId}` - Obtener ticket por ID
- `GET /pass-code/{passCode}` - Obtener ticket por código de pass
- `GET /user/{userId}` - Obtener tickets por usuario
- `GET /user/{userId}/active` - Obtener tickets activos
- `GET /user/{userId}/redeemed` - Obtener tickets canjeados
- `GET /user/{userId}/not-redeemed` - Obtener tickets no canjeados
- `GET /event/{eventId}` - Obtener tickets por evento
- `GET /event/{eventId}/count` - Contar tickets por evento
- `GET /event/{eventId}/redeemed/count` - Contar tickets canjeados
- `GET /{ticketId}/is-redeemed` - Verificar si está canjeado

## 🔒 Manejo de Concurrencia

Todas las operaciones críticas implementan:
- **Bloqueo pesimista** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)
- **Control de versión optimista** (`@Version`)
- **Reintentos automáticos** (`@Retryable`)
- **Recuperación graceful** (`@Recover`)

## 📊 DTOs Creados

### Pass
- `PassDTO` - Información completa del pass
- `CreatePassDTO` - Para crear nuevos passes

### Ticket
- `TicketDTO` - Información completa del ticket
- `CreateTicketDTO` - Para crear nuevos tickets

### Event (Actualizado)
- `EventDTO` - Incluye información de passes (total, disponibles, vendidos)

## 🎯 Casos de Uso Principales

1. **Crear Evento con Passes**: El evento define cuántos passes tendrá
2. **Vender Pass**: Un pass se marca como vendido y se asocia a un usuario
3. **Crear Ticket**: Se crea un ticket que conecta pass + usuario + consumos
4. **Canjear Consumos**: Los detalles de consumo se pueden canjear individualmente
5. **Canjear Ticket**: Cuando todos los consumos están canjeados, el ticket se marca como canjeado

## ✅ Beneficios

- **Control de Inventario**: Cada evento tiene cantidad fija de passes
- **Trazabilidad**: Cada pass tiene código único y seguimiento completo
- **Flexibilidad**: Los consumos se pueden canjear individualmente
- **Concurrencia**: Sistema robusto ante múltiples operaciones simultáneas
- **Escalabilidad**: Estructura preparada para microservicios
