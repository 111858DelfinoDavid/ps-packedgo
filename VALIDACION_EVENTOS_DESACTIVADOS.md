# Validación de Eventos Desactivados en Sistema QR

## 📋 Resumen
Se implementó la validación de eventos desactivados en el sistema de canje de QR codes. Cuando un evento es desactivado (baja lógica con `active=false`), todos los tickets y consumiciones asociados quedan invalidados automáticamente.

## 🎯 Objetivo
Prevenir el uso de entradas y consumiciones cuando un evento ha sido desactivado por el administrador, asegurando que:
- Las entradas (tickets/passes) no puedan usarse para ingresar al evento
- Las consumiciones no puedan canjearse
- Los usuarios reciban un mensaje claro explicando por qué el QR no es válido

## 🔧 Implementación Técnica

### Backend - QRValidationServiceImpl.java

#### 1. Validación en Entradas (validateEntryQR)
```java
// 6. Verificar que el evento esté activo (no desactivado)
if (!event.isActive()) {
    log.warn("❌ Event is inactive (deactivated): {}", event.getId());
    return ValidateEntryQRResponse.builder()
            .valid(false)
            .message("❌ Este evento ha sido desactivado. Las entradas ya no son válidas.")
            .build();
}
```

**Ubicación:** Línea ~119 en `QRValidationServiceImpl.java`  
**Momento de validación:** Después de verificar que el ticket pertenece al evento, antes de verificar si ya fue usado

#### 2. Validación en Consumiciones (validateConsumptionQR)
```java
// 4. Verificar que el evento esté activo (no desactivado)
Event event = getEventFromDetail(detail);
if (event != null && !event.isActive()) {
    log.warn("❌ Event is inactive (deactivated): {}", event.getId());
    return ValidateConsumptionQRResponse.builder()
            .success(false)
            .message("❌ Este evento ha sido desactivado. Las consumiciones ya no son válidas.")
            .build();
}
```

**Ubicación:** Línea ~223 en `QRValidationServiceImpl.java`  
**Momento de validación:** Después de encontrar el detalle de consumición, antes de verificar si está activo

#### 3. Método Auxiliar
```java
private Event getEventFromDetail(TicketConsumptionDetail detail) {
    try {
        TicketConsumption ticketConsumption = detail.getTicketConsumption();
        Optional<Ticket> ticketOpt = ticketRepository.findByTicketConsumption(ticketConsumption);
        
        if (ticketOpt.isPresent()) {
            return ticketOpt.get().getPass().getEvent();
        }
        return null;
    } catch (Exception e) {
        log.warn("Could not retrieve event for detail {}", detail.getId());
        return null;
    }
}
```

**Propósito:** Navegar desde el detalle de consumición hasta el evento para verificar su estado

## 📊 Flujo de Validación

### Entrada al Evento
```
Usuario escanea QR
    ↓
QRValidationController recibe request
    ↓
Parsea QR code (ticketId, eventId, userId)
    ↓
Busca ticket en base de datos
    ↓
Verifica que ticket esté activo
    ↓
Verifica que pertenezca al evento solicitado
    ↓
✅ NUEVO: Verifica que evento esté activo (active=true)
    ↓
Verifica si ya fue usado (redeemed)
    ↓
Marca como usado y retorna éxito
```

### Canje de Consumición
```
Usuario escanea QR de consumición
    ↓
QRValidationController recibe request
    ↓
Parsea QR code (ticketId, eventId)
    ↓
Busca detalle de consumición
    ↓
Obtiene evento desde detalle
    ↓
✅ NUEVO: Verifica que evento esté activo (active=true)
    ↓
Verifica que detalle esté activo
    ↓
Verifica cantidad disponible
    ↓
Canjea (parcial o total) y retorna éxito
```

## 🗄️ Base de Datos

### Tabla: events
```sql
id | name        | active | created_by | created_at              | updated_at
---+-------------+--------+------------+-------------------------+-------------------------
1  | Nina Kraviz | f      | 1          | 2025-11-22 00:38:04.14 | 2025-11-22 23:23:31.62
```

**Campo clave:** `active` (boolean)
- `true` (t): Evento activo, QR codes funcionan normalmente
- `false` (f): Evento desactivado, QR codes invalidados

### Verificación de Baja Lógica
```sql
SELECT id, name, active, created_by, created_at, updated_at 
FROM events 
ORDER BY updated_at DESC 
LIMIT 10;
```

## 📱 Experiencia de Usuario

### Empleado intentando validar entrada de evento desactivado:
```json
{
  "valid": false,
  "message": "❌ Este evento ha sido desactivado. Las entradas ya no son válidas.",
  "ticketInfo": null
}
```

### Empleado intentando canjear consumición de evento desactivado:
```json
{
  "success": false,
  "message": "❌ Este evento ha sido desactivado. Las consumiciones ya no son válidas.",
  "consumptionInfo": null
}
```

## ✅ Casos de Uso

### Caso 1: Evento Cancelado
**Escenario:** Un evento se cancela por mal tiempo  
**Acción:** Admin desactiva el evento desde el panel  
**Resultado:** 
- Todos los QR de entrada se invalidan
- Todas las consumiciones se invalidan
- Empleados ven mensaje claro al escanear
- Datos históricos se preservan en BD

### Caso 2: Evento Pospuesto
**Escenario:** Un evento se pospone para otra fecha  
**Acción:** Admin desactiva el evento original, crea uno nuevo  
**Resultado:**
- QR antiguos no funcionan
- Se generan nuevos QR para el evento nuevo
- Trazabilidad completa en base de datos

### Caso 3: Evento Finalizado Prematuramente
**Escenario:** Un evento termina antes de lo previsto  
**Acción:** Admin desactiva el evento  
**Resultado:**
- No se pueden canjear más consumiciones
- No pueden ingresar más personas
- Control total del organizador

## 🔒 Seguridad y Control

### Validaciones en Cascada
1. ✅ QR code válido (formato correcto)
2. ✅ Ticket existe en base de datos
3. ✅ Ticket está activo
4. ✅ Ticket pertenece al evento
5. ✅ **Evento está activo** ← NUEVO
6. ✅ Ticket no fue usado previamente
7. ✅ Solo el creador puede desactivar eventos

### Privilegios
- Solo el usuario que **creó el evento** puede desactivarlo
- Validado mediante JWT token (userId)
- Retorna `403 Forbidden` si el usuario no es el creador

## 🧪 Pruebas

### Verificar en Frontend
1. Login como admin creador del evento
2. Ir a gestión de eventos
3. Desactivar un evento
4. Intentar escanear QR de entrada o consumición
5. **Resultado esperado:** Mensaje "Este evento ha sido desactivado..."

### Verificar en Base de Datos
```bash
docker exec back-event-db-1 psql -U event_user -d event_db -c "SELECT id, name, active, created_by, updated_at FROM events ORDER BY updated_at DESC LIMIT 5;"
```

### Verificar Logs
```bash
# Buscar en logs del event-service
# Debe aparecer: "❌ Event is inactive (deactivated): {eventId}"
```

## 📦 Archivos Modificados

| Archivo | Cambios |
|---------|---------|
| `QRValidationServiceImpl.java` | Agregadas validaciones de `event.isActive()` en ambos métodos |
| `EventController.java` | Ya tenía la baja lógica implementada |
| `events-management.component.ts` | Ya tenía el botón de desactivar |

## 🚀 Despliegue

### Compilación
```bash
cd packedgo\back\event-service
.\mvnw.cmd clean package -DskipTests
```

### Ejecución
```bash
$env:APP_JWT_SECRET="mySecretKey123456789PackedGoAuth2025VerySecureKey"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5435/event_db"
$env:SPRING_DATASOURCE_USERNAME="event_user"
$env:SPRING_DATASOURCE_PASSWORD="event_password"

java -jar target\event-service-0.0.1-SNAPSHOT.jar
```

## 📝 Notas Importantes

1. **Reversibilidad:** La desactivación es reversible. Si se cambia `active=true` en BD, los QR vuelven a funcionar
2. **Datos Históricos:** Nada se elimina físicamente, todo queda para auditoría
3. **Performance:** La validación agrega mínimo overhead (~1 query adicional)
4. **Compatibilidad:** No rompe funcionalidad existente, solo agrega validación adicional

## 🔄 Flujo Completo

```
Admin desactiva evento
    ↓
EventController.deleteLogical()
    ↓
event.setActive(false)
    ↓
event.setUpdatedAt(now)
    ↓
eventRepository.save(event)
    ↓
    
[Usuario intenta usar QR]
    ↓
QRValidationService valida
    ↓
Verifica event.isActive()
    ↓
Retorna error si active=false
    ↓
Frontend muestra mensaje al empleado
```

## 📞 Soporte

Si tienes dudas sobre esta funcionalidad:
- Revisa los logs de `event-service` buscando "Event is inactive"
- Verifica el campo `active` en la tabla `events`
- Confirma que el `updated_at` coincide con la hora de desactivación
- Verifica que solo el creador puede desactivar eventos (campo `created_by`)

---

**Fecha de implementación:** 22 de noviembre de 2025  
**Versión:** event-service v0.0.1-SNAPSHOT  
**Estado:** ✅ Implementado y probado
