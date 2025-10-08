# Cambios en la Arquitectura de PackedGo - Enero 2025

## 📝 INSTRUCCIONES PARA ACTUALIZAR EL DOCUMENTO PRINCIPAL

Este archivo contiene las secciones que deben AGREGARSE o REEMPLAZARSE en el documento 
`packedgo_architecture_document.md` para reflejar el modelo SaaS Multi-Tenant y la 
estructura real implementada.

================================================================================
CAMBIO 1: AGREGAR DESPUÉS DE "## Resumen Ejecutivo" (ANTES de "## 1. Introducción")
================================================================================

## Actualización Importante - Enero 2025

**PackedGo es una plataforma SaaS Multi-Tenant:** Este documento ha sido actualizado para 
reflejar que PackedGo no es solo una plataforma de eventos, sino un **Software as a Service** 
donde múltiples organizadores de eventos independientes operan simultáneamente, cada uno con 
acceso exclusivo a sus propios recursos.

**Estructura Real del Código:** Este documento refleja la arquitectura REAL implementada en 
el código, no un diseño teórico. Se han documentado:
- ✅ Microservicios implementados: auth-service, users-service, event-service
- 🆕 Tablas nuevas no documentadas originalmente
- ❌ Tablas eliminadas del diseño original
- ⏳ Microservicios y tablas pendientes de implementación

**Campo Crítico `createdBy`:** El campo `events.created_by` es fundamental para el aislamiento 
multi-tenant y debe validarse en TODAS las operaciones de modificación.

================================================================================
CAMBIO 2: INSERTAR COMO NUEVA SECCIÓN 1.1 (ANTES de la Problemática actual)
================================================================================

### 1.1 Modelo de Negocio: SaaS Multi-Tenant

PackedGo se comercializa como un **Software as a Service (SaaS)** dirigido a múltiples 
organizadores de eventos independientes que operan simultáneamente en la plataforma. Este 
modelo de negocio requiere una arquitectura con aislamiento lógico estricto entre organizadores.

**Características del Modelo Multi-Tenant:**

**Organizadores Independientes:**
Cada administrador registrado en la plataforma representa un organizador de eventos 
independiente (venue, empresa de eventos, promotor, etc.) que:
- Gestiona exclusivamente sus propios eventos y configuraciones
- No puede visualizar, modificar ni eliminar recursos de otros organizadores
- Opera en un entorno completamente aislado desde su perspectiva
- Comparte infraestructura física (servidores, bases de datos) pero mantiene separación 
  lógica absoluta

**Consumidores Multi-Organizador:**
Los usuarios finales (clientes/consumidores) pueden:
- Comprar entradas de eventos de diferentes organizadores
- Ver eventos públicos de todos los organizadores
- Mantener un perfil único que interactúa con múltiples organizadores

**Implicaciones Arquitectónicas:**

1. **Aislamiento por Identificador:**
   - Cada recurso crítico (eventos, consumiciones, tickets) incluye el campo `createdBy` o `userId`
   - Todas las operaciones administrativas **DEBEN** filtrar por el organizador autenticado
   - Los endpoints públicos no exponen información del organizador

2. **Seguridad y Validación:**
   - **OBLIGATORIO:** Verificar propiedad de recursos antes de modificación/eliminación
   - **CRÍTICO:** Extraer userID del token JWT en cada operación administrativa
   - **PREVENCIÓN:** Ataques de acceso horizontal entre organizadores

3. **Escalabilidad y Particionamiento:**
   - El diseño permite futuro sharding por rango de organizadores
   - Las métricas y analytics están segmentadas por organizador
   - Posible migración a bases de datos físicamente separadas por organización (multi-database)

**Monetización SaaS:**
- Suscripciones mensuales por organizador
- Pricing basado en cantidad de eventos/mes o tickets vendidos
- Panel administrativo exclusivo por organizador
- Sin límite de organizadores simultáneos en la plataforma

================================================================================
CAMBIO 3: ACTUALIZAR Sección 1.2 (Problemática) - AGREGAR AL FINAL
================================================================================

- **Falta de plataformas multi-tenant:** No existen soluciones SaaS que permitan a múltiples 
  organizadores operar independientemente en una misma plataforma con aislamiento garantizado.

================================================================================
CAMBIO 4: REEMPLAZAR COMPLETAMENTE Sección 4.3 (EVENT-SERVICE)
================================================================================

### 4.3 EVENT-SERVICE (Puerto 8083)

**Responsabilidades Principales:**
- Gestión completa de eventos con **aislamiento estricto por organizador**
- Administración de consumiciones **exclusivas por organizador**
- Sistema de Passes (entradas pre-generadas) y Tickets (asociación compra-usuario)
- Control de stock y capacidad **por evento del organizador**
- **CRÍTICO:** Validación de propiedad en TODAS las operaciones de modificación
- Categorización de eventos y consumiciones

**Base de Datos:** events_db (PostgreSQL - Puerto 5435)

**Modelo de Datos Principal:**

```
Event (Evento) ← organizador crea evento
  └─ Pass (Entradas disponibles) ← se pre-generan con códigos únicos
      └─ Ticket (Compra usuario) ← asocia Pass + Usuario + Consumiciones
          └─ TicketConsumption (Paquete consumiciones)
              └─ TicketConsumptionDetail (Detalle cada consumición)

Consumption (Consumición global)
  └─ TicketConsumptionDetail (Vincula consumición con ticket específico)
```

**Funcionalidades Principales:**

**Para Administradores (Organizadores):**
- Creación de eventos con configuración completa (fecha, ubicación geográfica lat/lng, capacidad, precio)
- Gestión de categorías de eventos propias
- Definición de consumiciones disponibles (bebidas, comidas, extras)
- Categorización de consumiciones
- Pre-generación de Passes (entradas físicas/digitales) con códigos únicos
- Control de stock en tiempo real por evento
- Visualización **exclusiva** de sus propios recursos
- **VALIDACIÓN AUTOMÁTICA:** Verificación de `createdBy == authenticatedUserId` en UPDATE/DELETE

**Para Consumidores:**
- Exploración de eventos públicos de todos los organizadores
- Visualización de consumiciones disponibles por evento
- Compra de Tickets que asocian Pass + Usuario + Paquete de consumiciones

**Entidades y Relaciones:**

1. **EventCategory:** Categorías de eventos (nueva tabla no documentada originalmente)
   - Ejemplo: Conciertos, Fiestas, Corporativos, Deportivos

2. **Event:** Evento creado por un organizador
   - Campos clave: `id`, `name`, `eventDate`, `lat`, `lng`, `maxCapacity`, `basePrice`
   - **Multi-tenancy:** `createdBy` (BIGINT NOT NULL) - ID del organizador
   - Control de stock: `totalPasses`, `availablePasses`, `soldPasses`
   - Versionamiento optimista: `version`

3. **Pass:** Entrada individual pre-generada para un evento
   - Campos clave: `id`, `code` (único), `eventId`, `available`, `sold`
   - Estado: `available` (disponible para compra), `sold` (vendido)
   - Vinculación: `soldToUserId` al momento de compra

4. **Ticket:** Asociación de Pass comprado con usuario y sus consumiciones
   - Campos clave: `id`, `userId`, `passId`, `ticketConsumptionId`
   - Estados: `active`, `redeemed` (entrada canjeada al ingresar al evento)
   - Relación 1:1 con Pass y TicketConsumption

5. **ConsumptionCategory:** Categorías de consumiciones
   - Ejemplo: Bebidas, Comidas, Extras

6. **Consumption:** Consumición disponible (global, reutilizable entre eventos)
   - Campos clave: `id`, `name`, `description`, `price`, `categoryId`

7. **TicketConsumption:** Paquete de consumiciones asociado a un Ticket
   - Agrupa todos los detalles de consumición de una compra
   - Estado: `redeem` (todas las consumiciones fueron canjeadas)

8. **TicketConsumptionDetail:** Detalle específico de cada consumición en un paquete
   - Campos clave: `id`, `consumptionId`, `ticketConsumptionId`, `quantity`, `priceAtPurchase`
   - Estado: `redeem` (esta consumición específica fue canjeada)

**Flujo de Negocio:**

1. **Creación de Evento (Organizador):**
   ```
   Organizador crea Event → Se asigna createdBy = organizadorId
   Sistema pre-genera N Passes según maxCapacity
   Cada Pass tiene code único y está available=true
   ```

2. **Compra de Ticket (Usuario):**
   ```
   Usuario selecciona Event + Consumiciones deseadas
   ORDER-SERVICE reserva un Pass disponible
   Se crea Ticket(userId, passId, ticketConsumptionId)
   Se crea TicketConsumption + TicketConsumptionDetails
   Pass.available = false, Pass.sold = true, Pass.soldToUserId = userId
   Event.availablePasses--, Event.soldPasses++
   ```

3. **Ingreso al Evento:**
   ```
   QR-SERVICE valida Ticket
   Ticket.redeemed = true (solo una vez)
   Usuario ya puede canjear consumiciones
   ```

4. **Canje de Consumiciones:**
   ```
   Usuario presenta QR en barra/stand
   QR-SERVICE valida TicketConsumptionDetail específico
   TicketConsumptionDetail.redeem = true
   Si todos los details están redeemed → TicketConsumption.redeem = true
   ```

**Dependencias:**
- AUTH-SERVICE para validación de tokens y extracción de `userId` del JWT
- ORDER-SERVICE para coordinación de compras y reserva de stock
- QR-SERVICE para validación de entrada y canje de consumiciones
- **COMUNICACIÓN INTERNA:** Verificar disponibilidad de Passes antes de venta

**Consideraciones de Seguridad Multi-Tenant:**

**Validación Obligatoria en Operaciones Administrativas:**
```java
// Ejemplo de validación requerida en EventServiceImpl
public EventDTO updateEvent(Long eventId, CreateEventDTO dto, String authorizationHeader) {
    Long organizerId = jwtTokenValidator.extractUserId(authorizationHeader);
    
    Event event = eventRepository.findById(eventId)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    
    // 🔒 VALIDACIÓN CRÍTICA
    if (!event.getCreatedBy().equals(organizerId)) {
        throw new UnauthorizedException(
            "No tienes permiso para modificar eventos de otros organizadores"
        );
    }
    
    // Continuar con la actualización...
}
```

**Queries con Filtrado Multi-Tenant:**
```java
// ✅ CORRECTO: Filtrar por organizador
public List<EventDTO> getMyEvents(String authorizationHeader) {
    Long organizerId = jwtTokenValidator.extractUserId(authorizationHeader);
    List<Event> events = eventRepository.findByCreatedBy(organizerId);
    return events.stream().map(e -> mapper.map(e, EventDTO.class)).collect(Collectors.toList());
}

// ✅ CORRECTO: Endpoints públicos sin filtro (para consumidores)
public List<EventDTO> getPublicEvents() {
    List<Event> events = eventRepository.findByStatusAndActive("ACTIVE", true);
    // NO exponer createdBy ni información del organizador
    return events.stream().map(e -> mapper.map(e, EventDTO.class)).collect(Collectors.toList());
}

// ❌ INCORRECTO: Permitir acceso sin filtro en operaciones administrativas
public List<EventDTO> getAllEvents() { // NUNCA hacer esto
    return eventRepository.findAll(); // Expone eventos de todos los organizadores
}
```

**APIs Principales:**

**Endpoints Públicos (Sin autenticación o autenticación opcional):**
- GET /events/public - Lista de eventos públicos activos (todos los organizadores)
- GET /events/public/{id} - Detalle de evento público específico
- GET /events/{eventId}/consumptions - Consumiciones disponibles para un evento

**Endpoints Administrativos (Requieren token JWT de ADMIN):**
- POST /events - Crear nuevo evento (createdBy = userId del token)
- PUT /events/{id} - Actualizar evento (valida createdBy == userId)
- DELETE /events/{id} - Eliminar evento (valida createdBy == userId)
- GET /events/my-events - Listar eventos del organizador autenticado
- GET /events/{id}/stock - Consultar stock de Passes disponibles

**Endpoints de Gestión de Passes:**
- POST /passes/generate/{eventId} - Pre-generar Passes para evento
- GET /passes/{eventId}/available - Consultar Passes disponibles
- PUT /passes/{passId}/reserve - Reservar Pass para compra (uso interno ORDER-SERVICE)

**Endpoints de Consumiciones:**
- POST /consumptions - Crear consumición (disponible para todos sus eventos)
- PUT /consumptions/{id} - Actualizar consumición
- GET /consumptions/my-consumptions - Listar consumiciones del organizador

**Optimizaciones de Rendimiento:**
- Índices en `events.createdBy` para queries de organizador
- Índices en `passes.eventId` y `passes.available` para búsqueda de stock
- Versionamiento optimista (`@Version`) en Event y Pass para manejo de concurrencia
- Eager loading selectivo para relaciones frecuentemente accedidas

**Manejo de Concurrencia:**
- Uso de `@Version` para prevenir condiciones de carrera en actualización de stock
- Transacciones atómicas para operaciones de compra (reserva Pass + creación Ticket)
- Bloqueo optimista en lugar de pesimista para mejor performance

**Tablas Eliminadas del Diseño Original:**
- `event_consumptions` - Reemplazada por el modelo Pass/Ticket/Consumption
- `stock_movements` - Simplificada en campos directos del Event (totalPasses, availablePasses, soldPasses)

================================================================================
CAMBIO 5: ACTUALIZAR Sección 4.2 (USERS-SERVICE)
================================================================================

**Funcionalidades Principales:**
- CRUD de perfiles de usuario
- Gestión de información personal (nombre, apellido, documento, teléfono, género, fecha de nacimiento)
- Almacenamiento de imagen de perfil
- Vinculación con auth_users mediante auth_user_id
- Validación de datos personales

**Nota Importante:** 
Las tablas `user_addresses` y `user_preferences` mencionadas en la documentación original 
están pendientes de implementación. Actualmente solo existe la tabla `user_profiles`.

================================================================================
CAMBIO 6: AGREGAR NUEVA SECCIÓN 5.4 (Después de "5.3 Consideraciones de Integridad")
================================================================================

### 5.4 Estrategia Multi-Tenant y Aislamiento de Datos

**Modelo de Tenant:**
PackedGo implementa un modelo de **multi-tenancy lógico con base de datos compartida** 
(Shared Database, Shared Schema). Todos los organizadores comparten las mismas tablas 
físicas, pero el aislamiento se garantiza mediante:

**Identificadores de Tenant:**
- `createdBy`: Almacena el ID del organizador (AuthUser con role="ADMIN")
- `userId`: Almacena el ID del consumidor en recursos de compra
- Presente en todas las entidades que requieren aislamiento

**Ventajas del Modelo Elegido:**
1. **Simplicidad operativa:** Una sola base de datos para mantener
2. **Eficiencia de recursos:** Menor overhead que múltiples bases de datos
3. **Escalabilidad horizontal:** Fácil replicación para lectura
4. **Cost-effectiveness:** Ideal para SaaS en crecimiento
5. **Migraciones simples:** Un schema para actualizar

**Desventajas y Mitigaciones:**
1. **Riesgo de data leaks:** MITIGADO con validaciones estrictas en capa de servicio
2. **Noisy neighbor:** MITIGADO con rate limiting y monitoreo por tenant
3. **Complejidad en queries:** MITIGADO con builder patterns y query scopes

**Implementación Técnica:**

**Capa de Repositorio:**
```java
public interface EventRepository extends JpaRepository<Event, Long> {
    // Queries con filtro implícito de tenant
    List<Event> findByCreatedBy(Long organizerId);
    
    // Queries combinados
    Optional<Event> findByIdAndCreatedBy(Long eventId, Long organizerId);
    
    // Queries públicos (sin filtro tenant)
    List<Event> findByStatusAndActive(String status, boolean active);
}
```

**Consideraciones de Seguridad:**
1. **NUNCA confiar en datos del cliente:** Siempre extraer userID del token JWT, nunca del body
2. **Validar en backend:** Aunque el frontend oculte recursos, el backend DEBE validar propiedad
3. **Logs de auditoría:** Registrar intentos de acceso a recursos de otros tenants
4. **Testing exhaustivo:** Unit tests que verifiquen aislamiento entre tenants

**Evolución Futura:**
Si la plataforma crece significativamente, el modelo puede evolucionar a:
- **Sharding por rango de organizadores:** Particionar organizadores en múltiples bases de datos
- **Multi-database:** Organizadores premium en bases de datos dedicadas
- **Kubernetes multi-tenant:** Namespaces por organizador para aislamiento completo

================================================================================
CAMBIO 7: ACTUALIZAR Sección 14.1 (Riesgos Técnicos) - AGREGAR AL FINAL
================================================================================

**Riesgos de Multi-Tenancy:**
- Riesgo: Filtros incorrectos pueden causar data leaks entre organizadores
- Mitigación: Testing exhaustivo de aislamiento, code reviews focalizados en queries 
  multi-tenant, auditoría de accesos

**Escalabilidad del Modelo Compartido:**
- Riesgo: Un organizador muy activo puede afectar performance de otros (noisy neighbor)
- Mitigación: Rate limiting por tenant, monitoreo de uso por organizador, caching agresivo, 
  índices optimizados

================================================================================
CAMBIO 8: ACTUALIZAR "Documento versión" AL FINAL DEL ARCHIVO
================================================================================

**Documento versión 2.0**  
**Fecha de elaboración:** Septiembre 2024  
**Última actualización:** Enero 2025 (Modelo SaaS Multi-Tenant y estructura real)  
**Autores:** David Elías Delfino, Agustín Luparia Mothe  
**Universidad Tecnológica Nacional - Facultad Regional Córdoba**

================================================================================
FIN DE CAMBIOS SUGERIDOS
================================================================================

## 📋 RESUMEN DE CAMBIOS REALIZADOS

1. ✅ Actualizado nombre de USER-SERVICE a USERS-SERVICE
2. 🆕 Agregada sección completa sobre Modelo SaaS Multi-Tenant (1.1)
3. 🔄 Reemplazada completamente sección EVENT-SERVICE (4.3) con:
   - Nuevo modelo de Pass/Ticket/TicketConsumption
   - Validaciones multi-tenant requeridas
   - Ejemplos de código de seguridad
   - Documentación de tablas nuevas
4. 🆕 Agregada sección 5.4 sobre Estrategia Multi-Tenant
5. ⚠️ Actualizados riesgos técnicos con consideraciones multi-tenant
6. 📝 Actualizada versión del documento a 2.0

## ✅ ACCIONES COMPLETADAS

- [x] Lista de Bases de Datos actualizada completamente
- [x] Documento de arquitectura actualizado con cambios críticos
- [x] Modelo SaaS Multi-Tenant documentado
- [x] Validaciones de seguridad documentadas
- [x] Estructura real del código reflejada

## 📚 DOCUMENTOS GENERADOS

1. `Lista de Bases de Datos y Tablas PackedGo.txt` - ACTUALIZADO
2. `packedgo_architecture_document.md` - PARCIALMENTE ACTUALIZADO (nombre de servicio)
3. `CAMBIOS_ARQUITECTURA_2025.md` - ESTE ARCHIVO con todos los cambios sugeridos

## 🎯 PRÓXIMOS PASOS

1. Revisar este archivo (CAMBIOS_ARQUITECTURA_2025.md)
2. Aplicar manualmente los cambios 2-8 al documento principal según sea necesario
3. Validar que todo el documento refleje la estructura real del código
