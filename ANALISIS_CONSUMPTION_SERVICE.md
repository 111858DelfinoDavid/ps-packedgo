# 🔍 ANÁLISIS EXHAUSTIVO - CONSUMPTION-SERVICE

---

## 📊 INFORMACIÓN GENERAL

**Servicio:** consumption-service
**Puerto:** 8088 (asumido, no especificado en configuración)
**Context Path:** `/api`
**Base URL Endpoints:** `/api/consumption/tickets`
**Estado:** ❌ NO DESPLEGADO (requiere configuración en docker-compose)

---

## 🎯 PROPÓSITO DEL SERVICIO

El **consumption-service** es un microservicio especializado en la **gestión del ciclo de vida de tickets post-compra**, específicamente:

1. **Generación de Tickets:** Convierte órdenes pagadas en tickets utilizables con QR codes
2. **Validación de Entrada:** Valida tickets QR en la entrada de eventos
3. **Validación de Consumo:** Valida QR codes para canje de consumiciones dentro del evento

**Diferencia clave:** Este servicio NO maneja la compra/pago (ORDER-SERVICE), sino la **utilización** de los tickets después de la compra.

---

## 📡 ENDPOINTS IDENTIFICADOS

### 1. POST `/api/consumption/tickets/generate`
**Descripción:** Genera tickets con QR codes a partir de una orden pagada
**Parámetros:**
- `orderId` (query param): ID de la orden pagada

**Request:**
```
POST /api/consumption/tickets/generate?orderId=1
```

**Lógica:**
1. Obtiene la orden desde ORDER-SERVICE
2. Valida que el estado sea "PAID"
3. Por cada item de la orden, genera un ticket en EVENT-SERVICE
4. Genera QR codes de entrada y consumo
5. Retorna los tickets generados con sus QR codes

**Response (Success):**
```json
{
  "success": true,
  "message": "Tickets generated successfully",
  "tickets": [
    {
      "ticketId": 1,
      "userId": 1,
      "eventId": 2,
      "eventName": "Evento para Carrito",
      "passCode": "PKG-2-XXX",
      "entryQR": "base64_encoded_qr_image",
      "consumptionQRs": [
        {
          "consumptionId": 1,
          "consumptionName": "Camiseta Oficial",
          "qrCode": "base64_encoded_qr_image"
        }
      ]
    }
  ]
}
```

**Response (Error):**
```json
{
  "success": false,
  "message": "Order must be in PAID status. Current: PENDING_PAYMENT",
  "tickets": []
}
```

**Integración:**
- **ORDER-SERVICE:** GET /orders/{orderId}
- **EVENT-SERVICE:** POST /tickets/create-with-consumptions
- **QRCodeService:** Genera QR codes con payload encriptado

---

### 2. POST `/api/consumption/tickets/validate-entry`
**Descripción:** Valida un QR code de entrada al evento
**Requiere:** Body con el QR code escaneado

**Request:**
```json
{
  "qrCode": "encrypted_qr_payload",
  "eventId": 2
}
```

**Lógica:**
1. Decodifica el QR code encriptado
2. Valida que el tipo sea "ENTRY"
3. Obtiene el ticket completo desde EVENT-SERVICE
4. Valida ownership (userId), evento correcto
5. Valida que no esté usado, cancelado o expirado
6. Marca el ticket como "REDEEMED" (usado)
7. Retorna información del ticket validado

**Response (Success):**
```json
{
  "valid": true,
  "message": "Entry validated successfully",
  "ticketId": 1,
  "userId": 1,
  "eventId": 2,
  "eventName": "Evento para Carrito",
  "userName": "Agustin Luparia",
  "validatedAt": "2025-11-02T01:30:00"
}
```

**Response (Error):**
```json
{
  "valid": false,
  "message": "Ticket already used at: 2025-11-02T01:00:00",
  "ticketId": 1,
  "userId": 1,
  "eventId": 2,
  "eventName": "Evento para Carrito",
  "userName": "Agustin Luparia",
  "validatedAt": "2025-11-02T01:30:00"
}
```

**Integración:**
- **QRCodeService:** Decodifica y valida QR
- **EVENT-SERVICE:** GET /tickets/{ticketId}/full
- **EVENT-SERVICE:** PUT /tickets/{ticketId}/redeem

---

### 3. POST `/api/consumption/tickets/validate-consumption`
**Descripción:** Valida un QR code para canjear una consumición dentro del evento
**Requiere:** Body con el QR code de consumo escaneado

**Request:**
```json
{
  "qrCode": "encrypted_qr_payload",
  "eventId": 2,
  "consumptionId": 1
}
```

**Lógica:**
1. Decodifica el QR code encriptado
2. Valida que el tipo sea "CONSUMPTION"
3. Obtiene información del ticket-consumption desde EVENT-SERVICE
4. Valida que corresponda al evento y consumo solicitado
5. Valida que no esté ya usado
6. Marca el ticket-consumption como "USED"
7. Retorna información de la consumición canjeada

**Response (Success):**
```json
{
  "valid": true,
  "message": "Consumption validated successfully",
  "ticketId": 1,
  "consumptionId": 1,
  "consumptionName": "Camiseta Oficial",
  "quantity": 1,
  "userName": "Agustin Luparia",
  "validatedAt": "2025-11-02T01:45:00"
}
```

**Response (Error):**
```json
{
  "valid": false,
  "message": "Consumption already redeemed at: 2025-11-02T01:30:00",
  "ticketId": 1,
  "consumptionId": 1,
  "consumptionName": "Camiseta Oficial",
  "quantity": 1,
  "userName": "Agustin Luparia",
  "validatedAt": "2025-11-02T01:45:00"
}
```

**Integración:**
- **QRCodeService:** Decodifica y valida QR
- **EVENT-SERVICE:** GET /ticket-consumption/{id}
- **EVENT-SERVICE:** PUT /ticket-consumption/{id}/use

---

### 4. GET `/api/consumption/tickets/health`
**Descripción:** Health check del servicio
**Parámetros:** Ninguno

**Response:**
```
Consumption Service UP
```

---

## 🔗 INTEGRACIONES CON OTROS SERVICIOS

### 1. ORDER-SERVICE Integration
**Client:** `OrderServiceClient`

**Endpoints usados:**
- `GET /api/orders/{orderId}` - Obtiene información completa de la orden

**Configuración requerida:**
```properties
order.service.url=http://order-service:8084/api
```

---

### 2. EVENT-SERVICE Integration
**Client:** `EventServiceClient`

**Endpoints usados:**
- `POST /tickets/create-with-consumptions` - Crea ticket con consumiciones
- `GET /tickets/{ticketId}/full` - Obtiene ticket completo
- `PUT /tickets/{ticketId}/redeem` - Marca ticket como usado
- `GET /ticket-consumption/{id}` - Obtiene info de consumición
- `PUT /ticket-consumption/{id}/use` - Marca consumición como usada

**Configuración requerida:**
```properties
event.service.url=http://event-service:8086/api
```

**NOTA CRÍTICA:** Estos endpoints de EVENT-SERVICE actualmente NO están accesibles vía `/api/event-service/` path (retornan 404). El consumption-service **NO FUNCIONARÁ** hasta que estos endpoints sean configurados correctamente.

---

## 🔐 SERVICIO QR CODE

**Clase:** `QRCodeService`

**Funcionalidades:**
1. **Generación de QR de Entrada:**
   - Tipo: "ENTRY"
   - Payload: ticketId, userId, eventId, timestamp
   - Encriptación: Sí (seguridad)
   - Formato: Base64 encoded PNG image

2. **Generación de QR de Consumo:**
   - Tipo: "CONSUMPTION"
   - Payload: ticketId, userId, eventId, consumptionId, timestamp
   - Encriptación: Sí
   - Formato: Base64 encoded PNG image

3. **Validación y Decodificación:**
   - Desencripta el payload
   - Valida la estructura
   - Valida timestamp (evita replay attacks)
   - Retorna QRPayload con información extraída

**Tecnología:** Probablemente usa ZXing (Java QR code library)

---

## 📊 FLUJO COMPLETO DE NEGOCIO

### Flujo Normal de Uso:

```
1. Usuario compra tickets
   └─> ORDER-SERVICE: POST /cart/add
   └─> ORDER-SERVICE: POST /checkout/multi
   └─> PAYMENT-SERVICE: POST /payments/create
   └─> Usuario paga → Order status = "PAID"

2. Sistema genera tickets con QR
   └─> CONSUMPTION-SERVICE: POST /tickets/generate?orderId=X
   └─> Por cada item de la orden:
       ├─> EVENT-SERVICE: POST /tickets/create-with-consumptions
       ├─> Genera QR de entrada
       └─> Genera QR(s) de consumo(s)
   └─> Retorna tickets con QR codes al usuario

3. Usuario llega al evento
   └─> Personal del evento escanea QR de entrada
   └─> CONSUMPTION-SERVICE: POST /tickets/validate-entry
   └─> Si válido:
       ├─> EVENT-SERVICE: PUT /tickets/{id}/redeem
       └─> Ticket marcado como usado (entrada concedida)

4. Usuario canjea consumiciones
   └─> Personal del stand escanea QR de consumo
   └─> CONSUMPTION-SERVICE: POST /tickets/validate-consumption
   └─> Si válido:
       ├─> EVENT-SERVICE: PUT /ticket-consumption/{id}/use
       └─> Consumición marcada como usada (producto entregado)
```

---

## 🎯 PROPÓSITO Y VALOR DEL SERVICIO

### ¿Por qué existe este servicio?

1. **Separación de Responsabilidades:**
   - ORDER-SERVICE: Gestión de compras y pagos
   - EVENT-SERVICE: Gestión de eventos, passes, tickets (storage)
   - CONSUMPTION-SERVICE: Gestión de **uso** de tickets (lógica de validación)

2. **Seguridad:**
   - QR codes encriptados
   - Validación de ownership
   - Prevención de replay attacks (timestamp)
   - Validación de estado (no usado, no cancelado, no expirado)

3. **Escalabilidad:**
   - Independiente de ORDER-SERVICE y EVENT-SERVICE
   - Puede escalar horizontalmente para eventos masivos
   - No afecta el flujo de compra/pago

4. **Funcionalidad Específica:**
   - Generación de QR codes
   - Validación en tiempo real (entrada al evento)
   - Gestión de canjes de consumiciones

---

## ❌ PROBLEMAS IDENTIFICADOS

### 1. Servicio NO Desplegado
**Severidad:** Alta
**Descripción:** El servicio no está en docker-compose.yml principal
**Impacto:** No se puede testear ningún endpoint
**Solución requerida:** Agregar al docker-compose con configuración apropiada

### 2. Dependencia de Endpoints No Accesibles
**Severidad:** Crítica
**Descripción:** EVENT-SERVICE endpoints `/tickets/**` y `/ticket-consumption/**` retornan 404
**Impacto:** El servicio NO PUEDE FUNCIONAR sin estos endpoints
**Solución requerida:** Configurar routing en EVENT-SERVICE para exponer estos controllers

### 3. Configuración Incompleta
**Severidad:** Alta
**Descripción:** No hay puerto configurado, no hay variables de entorno para URLs de servicios
**Impacto:** Conexión inter-service fallará
**Solución requerida:** Agregar application.properties completo con:
- `server.port=8088`
- `order.service.url=...`
- `event.service.url=...`
- Configuración de seguridad (secret key para QR encryption)

---

## 📋 CONFIGURACIÓN REQUERIDA

### Archivo: `application.properties`
```properties
# Server
spring.application.name=consumption-service
server.port=8088
server.servlet.context-path=/api

# Service URLs
order.service.url=http://order-service:8084/api
event.service.url=http://event-service:8086/api

# QR Code Security
qr.secret.key=${QR_SECRET_KEY:your-secret-key-for-qr-encryption}
qr.expiration.minutes=60

# Logging
logging.level.com.packed_go.consumption_service=DEBUG
```

### Archivo: `docker-compose.yml` (agregar)
```yaml
consumption-service:
  build: ./consumption-service
  container_name: consumption-service
  ports:
    - "8088:8088"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - ORDER_SERVICE_URL=http://order-service:8084/api
    - EVENT_SERVICE_URL=http://event-service:8086/api
    - QR_SECRET_KEY=${QR_SECRET_KEY}
  depends_on:
    - order-service
    - event-service
  networks:
    - backend-network
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8088/api/consumption/tickets/health"]
    interval: 30s
    timeout: 10s
    retries: 3
```

---

## 🧪 PLAN DE TESTING

### Una vez desplegado, testear en este orden:

1. **Health Check**
   ```bash
   curl http://localhost:8088/api/consumption/tickets/health
   ```

2. **Generar Tickets** (requiere orden PAID)
   ```bash
   # Primero crear y pagar una orden
   # Luego:
   curl -X POST "http://localhost:8088/api/consumption/tickets/generate?orderId=1"
   ```

3. **Validar Entrada** (requiere QR generado)
   ```bash
   curl -X POST "http://localhost:8088/api/consumption/tickets/validate-entry" \
     -H "Content-Type: application/json" \
     -d '{
       "qrCode": "qr_code_from_generation",
       "eventId": 2
     }'
   ```

4. **Validar Consumo** (requiere QR de consumo generado)
   ```bash
   curl -X POST "http://localhost:8088/api/consumption/tickets/validate-consumption" \
     -H "Content-Type: application/json" \
     -d '{
       "qrCode": "consumption_qr_from_generation",
       "eventId": 2,
       "consumptionId": 1
     }'
   ```

---

## 📊 RESUMEN DE ENDPOINTS

| Endpoint | Método | Requiere Auth | Estado Actual | Notas |
|----------|--------|---------------|---------------|-------|
| `/api/consumption/tickets/health` | GET | No | ❌ No desplegado | Health check |
| `/api/consumption/tickets/generate` | POST | No | ❌ No desplegado | Genera tickets desde orden PAID |
| `/api/consumption/tickets/validate-entry` | POST | No | ❌ No desplegado | Valida QR de entrada |
| `/api/consumption/tickets/validate-consumption` | POST | No | ❌ No desplegado | Valida QR de consumo |

**Total endpoints:** 4
**Estado:** ❌ **NINGUNO TESTEABLE** (servicio no desplegado)

---

## 💡 RECOMENDACIONES

### Prioridad Crítica:
1. ✅ Agregar consumption-service a docker-compose.yml
2. ✅ Configurar variables de entorno y puerto
3. ✅ **CRÍTICO:** Resolver routing de EVENT-SERVICE endpoints `/tickets/**` y `/ticket-consumption/**`
4. ✅ Levantar el servicio y verificar logs

### Prioridad Alta:
1. ⏳ Configurar secret key para encriptación de QR codes
2. ⏳ Testear integración con ORDER-SERVICE
3. ⏳ Testear integración con EVENT-SERVICE (una vez endpoints disponibles)
4. ⏳ Testear flujo completo end-to-end

### Prioridad Media:
1. ⏳ Agregar autenticación/autorización a endpoints
2. ⏳ Agregar rate limiting para prevenir abuso
3. ⏳ Agregar logging y monitoreo
4. ⏳ Agregar tests unitarios e integración

---

## 🎯 IMPACTO EN EL SISTEMA

**Sin consumption-service funcionando:**
- ❌ No se pueden generar tickets utilizables después de la compra
- ❌ No se pueden validar entradas al evento
- ❌ No se pueden canjear consumiciones
- ✅ El flujo de compra/pago sigue funcionando (ORDER + PAYMENT services)

**Con consumption-service funcionando:**
- ✅ Flujo completo de compra → generación → uso
- ✅ Validación en tiempo real de tickets
- ✅ Control de consumiciones
- ✅ Seguridad con QR encriptados

---

**📅 Fecha Análisis:** 2 de Noviembre 2025
**👤 Responsable:** Claude Code AI
**🎯 Resultado:** Servicio bien diseñado pero NO DESPLEGADO - Requiere configuración
**⚠️ Bloqueador:** EVENT-SERVICE endpoints `/tickets/**` no accesibles (dependencia crítica)

---

**Estado:** ⚠️ **SERVICIO COMPLETO PERO NO OPERATIVO - REQUIERE DESPLIEGUE Y CONFIGURACIÓN**
