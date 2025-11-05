# 📋 Contexto Completo: Implementación MercadoPago - PackedGo

> **Documento de Contexto para IA**
> Última actualización: 2025-11-04
> Branch actual: `develop`
> Cambios aplicados: Restauración de seguridad JWT

---

## 🎯 Objetivo de este Documento

Este documento proporciona contexto completo sobre:
1. La implementación actual de MercadoPago en el proyecto
2. Análisis comparativo entre `develop` y `fix/mercadopago`
3. Cambios realizados y pendientes
4. Problemas identificados y sus soluciones
5. Arquitectura del sistema de pagos

---

## 📊 Estado Actual del Sistema

### Branches Analizadas
- **`develop`**: Branch principal de desarrollo (ACTUAL)
- **`fix/mercadopago`**: Branch con mejoras de MercadoPago

### Cambios Aplicados Hoy
✅ **Restauración de seguridad JWT en PaymentController.java**
- Archivo: `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/controller/PaymentController.java`
- Línea: 38-100
- Cambio: Endpoint `/payments/create` ahora REQUIERE token JWT
- Validación: Token debe ser válido antes de crear preferencias de pago

### Cambios NO Aplicados (de fix/mercadopago)
❌ **Frontend: payment.service.ts**
- Bug de URLs de retorno malformadas (desarrollado en fix/mercadopago)
- Estado: SIN APLICAR

❌ **Frontend: checkout.component.ts**
- Sistema de polling automático (cada 2s)
- Botón "Verificar mi pago" mejorado
- Estado: SIN APLICAR

❌ **Backend: PaymentService.java**
- Método `verifyPaymentStatus()` para consultar MercadoPago
- AutoReturn condicional (HTTPS en producción)
- Estado: SIN APLICAR

---

## 🔍 Análisis Comparativo: develop vs fix/mercadopago

### 1. Payment Service (Backend)

#### SecurityConfig.java
**Cambios en fix/mercadopago:**
```java
// Soporta ambas rutas (con y sin /api prefix)
.requestMatchers("/payments/webhook", "/api/payments/webhook").permitAll()
.requestMatchers("/payments/create", "/api/payments/create").permitAll()
.requestMatchers("/payments/verify/**", "/api/payments/verify/**").permitAll()
```

**Estado actual:** develop NO tiene soporte dual de rutas

#### PaymentController.java
**Cambio crítico aplicado HOY:**
```java
// ANTES (fix/mercadopago - INSEGURO):
@PostMapping("/create")
public ResponseEntity<PaymentResponse> createPayment(
    @Valid @RequestBody PaymentRequest request,
    @RequestHeader(value = "Authorization", required = false) String authHeader) {
    // JWT era OPCIONAL
}

// AHORA (develop con fix aplicado - SEGURO):
@PostMapping("/create")
public ResponseEntity<PaymentResponse> createPayment(
    @Valid @RequestBody PaymentRequest request,
    @RequestHeader("Authorization") String authHeader) {  // ✅ REQUIRED

    // Validación JWT
    String token = jwtTokenValidator.extractTokenFromHeader(authHeader);
    if (!jwtTokenValidator.validateToken(token)) {
        return UNAUTHORIZED;
    }
    // ...
}
```

**Ubicación:** `PaymentController.java:38-100`

#### PaymentService.java
**Cambios en fix/mercadopago NO aplicados:**

1. **AutoReturn Condicional:**
```java
// fix/mercadopago:
if (request.getSuccessUrl() != null && request.getSuccessUrl().startsWith("https://")) {
    preferenceBuilder.autoReturn("approved");
    log.info("autoReturn habilitado para URLs HTTPS");
} else {
    log.info("autoReturn deshabilitado - usando polling del frontend");
}

// develop actual:
// .autoReturn("approved") - Comentado siempre
```

2. **Método verifyPaymentStatus():**
```java
// fix/mercadopago (NO existe en develop):
@Transactional
public Payment verifyPaymentStatus(String orderId) {
    Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(...));

    if (payment.getMpPaymentId() != null) {
        PaymentClient client = new PaymentClient();
        com.mercadopago.resources.payment.Payment mpPayment = client.get(payment.getMpPaymentId());
        return updatePaymentFromMercadoPago(payment, mpPayment);
    }
    return payment;
}
```

**Estado:** Estos métodos NO existen en develop actual

---

### 2. Frontend (Angular)

#### payment.service.ts
**BUG CRÍTICO en develop:**
```typescript
// DEVELOP (BUGGY):
const sessionParam = sessionId ? `?sessionId=${sessionId}` : '';
successUrl: `${checkoutUrl}${sessionParam}&status=approved`
// Genera: http://localhost:4200/customer/checkout&status=approved
//                                                   ↑ FALTA el "?"

// FIX/MERCADOPAGO (CORRECTO):
if (sessionId) {
    successUrl = `${checkoutUrl}?sessionId=${sessionId}&status=approved`;
} else {
    successUrl = `${checkoutUrl}?status=approved`;
}
// Genera: http://localhost:4200/customer/checkout?status=approved
//                                                  ✅ CORRECTO
```

**Impacto:** URLs malformadas pueden causar que MercadoPago rechace la preferencia o que el parámetro `status` no se detecte al retornar.

**Archivo:** `packedgo/front-angular/src/app/core/services/payment.service.ts`

#### checkout.component.ts
**Mejoras en fix/mercadopago NO aplicadas:**

1. **Sistema de Polling Automático:**
```typescript
// fix/mercadopago:
private startPaymentPolling(orderNumber: string): void {
    this.paymentPollingSubscription = interval(2000)
        .pipe(switchMap(() => this.paymentService.verifyPaymentStatus(orderNumber)))
        .subscribe({
            next: (response) => {
                if (response.status === 'APPROVED') {
                    this.stopPaymentPolling();
                    this.loadExistingCheckout(this.sessionId);
                }
            }
        });
}
```

2. **Detección Automática al Regresar de MercadoPago:**
```typescript
// fix/mercadopago:
openPaymentCheckout(group: PaymentGroup): void {
    // Guardar en localStorage para verificar al regresar
    localStorage.setItem('pendingPaymentVerification', group.orderNumber);
    localStorage.setItem('pendingPaymentSessionId', this.sessionId);
    window.location.href = group.initPoint;
}

private checkPendingPaymentVerification(): void {
    const pendingOrderNumber = localStorage.getItem('pendingPaymentVerification');
    if (pendingOrderNumber) {
        this.startPaymentPolling(pendingOrderNumber);
    }
}
```

**Estado:** develop NO tiene este sistema de polling automático

---

## 🏗️ Arquitectura del Sistema de Pagos

### 1. SessionId (Checkout Multi-Admin)

#### ¿Qué es?
```
URL: http://localhost:3000/customer/checkout?sessionId=9475b106-033f-4ad7-9d66-86a0cde49a54
```

**Propósito:**
- Agrupa múltiples órdenes de diferentes admins en una sola sesión de checkout
- Permite persistencia: el usuario puede recargar la página o volver más tarde
- Trackea el progreso: muestra cuántos pagos están completados

**Entity:** `MultiOrderSession`
```java
@Entity
@Table(name = "multi_order_sessions")
public class MultiOrderSession {
    @Id
    private String sessionId;  // UUID generado automáticamente

    private Long userId;
    private Long cartId;
    private BigDecimal totalAmount;
    private String sessionStatus; // PENDING, PARTIAL, COMPLETED, EXPIRED
    private LocalDateTime expiresAt; // 30 minutos desde creación

    @OneToMany
    private List<Order> orders; // Una orden por admin
}
```

**Ubicación:** `packedgo/back/order-service/src/main/java/com/packed_go/order_service/entity/MultiOrderSession.java`

#### Flujo Completo con SessionId

```
1. Usuario agrega al carrito:
   - 2 entradas de Admin A ($1000)
   - 3 entradas de Admin B ($1500)

2. Checkout multi-admin:
   POST /api/orders/checkout-multi
   ├─ Crea MultiOrderSession (UUID: 9475b106...)
   ├─ Crea Order 1 (adminId=A, $1000)
   ├─ Crea Order 2 (adminId=B, $1500)
   └─ Retorna: { sessionId: "9475b106...", paymentGroups: [...] }

3. Frontend redirige:
   → /customer/checkout?sessionId=9475b106...

4. Usuario paga Order 1:
   → MercadoPago → Retorna: ?sessionId=9475b106&status=approved

5. Frontend detecta retorno:
   GET /api/orders/session/9475b106...
   → Muestra: "1 de 2 pagos completados"

6. Usuario paga Order 2:
   → MercadoPago → Retorna: ?sessionId=9475b106&status=approved

7. Ambos pagos completados:
   → Session status: COMPLETED
   → Se generan tickets automáticamente
```

**CONCLUSIÓN: SessionId es CORRECTO, NO es un error.**

---

### 2. Webhooks de MercadoPago

#### ¿Qué son?
MercadoPago envía notificaciones POST a tu servidor cuando un pago cambia de estado.

**Endpoint:** `POST /api/payments/webhook`

**Payload enviado por MercadoPago:**
```json
{
  "type": "payment",
  "data": {
    "id": "123456789"
  }
}
```

#### Problema en Localhost

```
MercadoPago (en internet)
    |
    | Intenta llamar:
    v
http://localhost:8085/api/payments/webhook
    ❌ ERROR: No puede acceder a tu computadora local
```

#### Soluciones

**Opción A: Ngrok (Documentado en fix/mercadopago)**
```bash
# 1. Instalar
winget install ngrok

# 2. Exponer payment-service
ngrok http 8085
# Output: https://abc123.ngrok-free.app

# 3. Configurar .env
WEBHOOK_URL=https://abc123.ngrok-free.app/api/payments/webhook

# 4. Reiniciar
docker-compose restart payment-service-app
```

**Pros:** Webhooks funcionan automáticamente
**Cons:** Requiere instalación, URL cambia cada vez (versión free)

**Opción B: Polling (Recomendado para desarrollo)**
```typescript
// Frontend consulta cada 2 segundos
interval(2000).pipe(
    switchMap(() => this.verifyPaymentStatus(orderNumber))
)
```

**Pros:** Funciona siempre, sin configuración adicional
**Cons:** Consume más requests, latencia de hasta 2s

**RECOMENDACIÓN:**
- **Desarrollo (localhost):** Usar polling (Opción B)
- **Staging/Producción:** Configurar webhooks (Opción A) + polling como fallback

---

### 3. Flujo Completo de Pago

```
┌─────────────┐
│   Usuario   │
└─────┬───────┘
      │ 1. Agrega items al carrito
      v
┌─────────────────┐
│ Shopping Cart   │
│ (order-service) │
└─────┬───────────┘
      │ 2. Checkout multi-admin
      v
┌──────────────────────┐
│ MultiOrderSession    │
│ + Orders (por admin) │
└─────┬────────────────┘
      │ 3. Crear preferencias de pago
      v
┌──────────────────┐         ┌─────────────────┐
│ payment-service  │ ------> │   MercadoPago   │
│ /payments/create │         │   (Sandbox)     │
└─────┬────────────┘         └────────┬────────┘
      │                               │
      │ 4. init_point                 │
      v                               │
┌──────────────┐                      │
│   Frontend   │ <--------------------┘
│   Checkout   │      5. Usuario paga
└─────┬────────┘
      │
      │ 6a. Webhook (producción)
      │     MercadoPago → payment-service
      │     payment-service → order-service
      │
      │ 6b. Polling (desarrollo)
      │     Frontend → payment-service → MercadoPago
      │
      v
┌─────────────────┐
│ Order PAID      │
│ → Genera Ticket │
└─────────────────┘
```

---

## 🚨 Problemas Identificados y Soluciones

### Problema 1: URLs de Retorno Malformadas (Frontend)
**Estado:** ❌ NO RESUELTO en develop
**Severidad:** 🔴 CRÍTICA

**Descripción:**
```typescript
// Cuando NO hay sessionId:
successUrl = `${checkoutUrl}${sessionParam}&status=approved`
// Genera: http://localhost:4200/customer/checkout&status=approved
//         ↑ Falta el "?" antes de "status"
```

**Impacto:**
- MercadoPago puede rechazar la preferencia
- El parámetro `status` no se detecta al retornar
- Usuario ve mensaje de error en lugar de confirmación

**Solución (de fix/mercadopago):**
```typescript
if (sessionId) {
    successUrl = `${checkoutUrl}?sessionId=${sessionId}&status=approved`;
} else {
    successUrl = `${checkoutUrl}?status=approved`;
}
```

**Archivo:** `packedgo/front-angular/src/app/core/services/payment.service.ts`

**Acción requerida:** Aplicar el fix de fix/mercadopago

---

### Problema 2: No hay Polling Automático (Frontend)
**Estado:** ❌ NO IMPLEMENTADO en develop
**Severidad:** 🟡 ALTA

**Descripción:**
- Después de pagar en MercadoPago, el usuario debe recargar manualmente
- No hay verificación automática del estado del pago
- UX deficiente: usuario no sabe si el pago fue procesado

**Solución (de fix/mercadopago):**
- Sistema de polling cada 2 segundos
- Detección automática al regresar de MercadoPago
- Botón "Verificar mi pago" con mejor UX

**Acción requerida:** Aplicar cambios de checkout.component.ts de fix/mercadopago

---

### Problema 3: AutoReturn Siempre Deshabilitado (Backend)
**Estado:** ⚠️ PARCIALMENTE RESUELTO
**Severidad:** 🟠 MEDIA

**Descripción:**
```java
// develop:
// .autoReturn("approved") - Comentado siempre
```

**Razón original:** MercadoPago sandbox rechaza autoReturn con error 400 en localhost (HTTP)

**Solución (de fix/mercadopago):**
```java
// Solo habilitar en HTTPS (producción)
if (request.getSuccessUrl().startsWith("https://")) {
    preferenceBuilder.autoReturn("approved");
} else {
    log.info("autoReturn deshabilitado - usando polling");
}
```

**Acción requerida:** Aplicar autoReturn condicional de fix/mercadopago

---

### Problema 4: Seguridad JWT Relajada (Backend)
**Estado:** ✅ RESUELTO HOY
**Severidad:** 🔴 CRÍTICA

**Descripción original (fix/mercadopago):**
```java
@RequestHeader(value = "Authorization", required = false) String authHeader
// JWT era OPCIONAL - cualquiera podía crear preferencias
```

**Solución aplicada:**
```java
@RequestHeader("Authorization") String authHeader  // ✅ REQUIRED
// Validación JWT antes de crear preferencias
```

**Archivo:** `PaymentController.java:38-41`
**Aplicado:** ✅ SÍ

---

### Problema 5: Falta Método verifyPaymentStatus (Backend)
**Estado:** ❌ NO IMPLEMENTADO en develop
**Severidad:** 🟡 ALTA

**Descripción:**
- No hay forma de consultar manualmente MercadoPago si webhook no llegó
- El endpoint `/verify/{orderId}` usa `processWebhookNotification` que es más limitado

**Solución (de fix/mercadopago):**
```java
@Transactional
public Payment verifyPaymentStatus(String orderId) {
    Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(...));

    if (payment.getMpPaymentId() != null) {
        PaymentClient client = new PaymentClient();
        com.mercadopago.resources.payment.Payment mpPayment =
            client.get(payment.getMpPaymentId());
        return updatePaymentFromMercadoPago(payment, mpPayment);
    }
    return payment;
}
```

**Acción requerida:** Implementar método en PaymentService.java

---

## ✅ Checklist de Tareas Pendientes

### Backend (Payment Service)

- [x] ✅ Restaurar JWT required en `/payments/create` (HECHO HOY)
- [ ] 🔲 Implementar autoReturn condicional (HTTPS en producción)
- [ ] 🔲 Agregar método `verifyPaymentStatus()` en PaymentService
- [ ] 🔲 Agregar método `updatePaymentFromMercadoPago()` helper
- [ ] 🔲 Actualizar SecurityConfig para soportar rutas duales
- [ ] 🔲 Configurar webhook URL para staging/producción

### Frontend (Angular)

- [ ] 🔲 **CRÍTICO:** Corregir bug de URLs de retorno en payment.service.ts
- [ ] 🔲 Implementar sistema de polling automático en checkout.component.ts
- [ ] 🔲 Agregar método `startPaymentPolling()`
- [ ] 🔲 Agregar método `checkPendingPaymentVerification()`
- [ ] 🔲 Actualizar botón "Verificar mi pago" con mejor UX
- [ ] 🔲 Agregar estilos del botón verify (.btn-verify con animación pulse)
- [ ] 🔲 Agregar mensaje de ayuda después de pagar

### Order Service

- [ ] 🔲 Verificar que generateTicketsForOrder se llama correctamente
- [ ] 🔲 Verificar que MultiOrderSession expira a los 30 min
- [ ] 🔲 Agregar validación de sesión expirada en frontend

### Testing

- [ ] 🔲 Probar checkout multi-admin en localhost
- [ ] 🔲 Probar pago en sandbox de MercadoPago
- [ ] 🔲 Verificar que polling detecta pagos aprobados
- [ ] 🔲 Verificar que tickets se generan correctamente
- [ ] 🔲 Probar con sessionId y sin sessionId
- [ ] 🔲 Probar expiración de sesión (30 min)
- [ ] 🔲 Probar en staging con webhooks reales (HTTPS)

---

## 📁 Archivos Modificados Hoy

### Backend
```
✅ packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/controller/PaymentController.java
   - Líneas 38-100
   - Cambio: Restaurar JWT required en /payments/create
   - Validación: Token debe ser válido
```

---

## 📁 Archivos Clave del Sistema

### Backend - Payment Service

```
PaymentController.java
├── POST /payments/create (PROTECTED - JWT required) ✅
├── POST /payments/webhook (PUBLIC - MercadoPago)
├── GET  /payments/order/{orderId} (PROTECTED)
└── POST /payments/verify/{orderId} (PROTECTED)

PaymentService.java
├── createPaymentPreference() ✅
├── processWebhookNotification() ✅
└── verifyPaymentStatus() ❌ (falta implementar)

SecurityConfig.java
└── Configuración de endpoints públicos/protegidos

JwtTokenValidator.java
├── validateToken() ✅
├── getUserIdFromToken() ✅
└── extractTokenFromHeader() ✅
```

### Backend - Order Service

```
OrderServiceImpl.java
├── checkoutMulti() - Crea MultiOrderSession ✅
├── getSessionStatus() - Consulta estado de sesión ✅
├── updateOrderFromPaymentCallback() - Procesa webhook ✅
└── generateTicketsForOrder() - Genera tickets post-pago ✅

MultiOrderSession.java
├── sessionId (UUID) ✅
├── expiresAt (30 min) ✅
└── updateSessionStatus() ✅
```

### Frontend - Angular

```
payment.service.ts
├── createPaymentPreference() ⚠️ (bug de URLs)
├── getPaymentStatus() ✅
└── verifyPaymentStatus() ❌ (falta implementar)

checkout.component.ts
├── loadExistingCheckout() ✅
├── openPaymentCheckout() ✅
└── startPaymentPolling() ❌ (falta implementar)
```

---

## 🔧 Configuración Actual

### Environment Variables (payment-service)

```env
# .env (develop actual)
SERVER_PORT=8085
DB_URL=jdbc:postgresql://payment-db:5432/payment_db

# JWT
JWT_SECRET=mySecretKey123456789PackedGoAuth2025VerySecureKey

# MercadoPago
MERCADOPAGO_ACCESS_TOKEN=APP_USR-1160956444149133-...
MERCADOPAGO_PUBLIC_KEY=APP_USR-704e26b4-...
WEBHOOK_URL=  # ← Vacío (webhooks no configurados)

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:4200
```

### Configuración de Ngrok (Opcional)

```bash
# Solo para testing de webhooks en desarrollo
ngrok http 8085

# Luego actualizar .env:
WEBHOOK_URL=https://abc123.ngrok-free.app/api/payments/webhook
```

---

## 🎯 Recomendaciones Finales

### Prioridad ALTA (Hacer primero)

1. **Corregir bug de URLs en payment.service.ts** 🔴
   - Archivo: `packedgo/front-angular/src/app/core/services/payment.service.ts`
   - Impacto: Crítico - puede causar fallos en producción
   - Esfuerzo: Bajo (5 min)

2. **Implementar polling automático en checkout.component.ts** 🟡
   - Archivo: `packedgo/front-angular/src/app/features/customer/checkout/checkout.component.ts`
   - Impacto: Alto - mejora significativa de UX
   - Esfuerzo: Medio (30 min)

3. **Implementar verifyPaymentStatus() en PaymentService.java** 🟡
   - Archivo: `packedgo/back/payment-service/src/main/java/com/packedgo/payment_service/service/PaymentService.java`
   - Impacto: Alto - permite verificación manual confiable
   - Esfuerzo: Medio (45 min)

### Prioridad MEDIA (Hacer después)

4. **AutoReturn condicional en PaymentService.java** 🟠
   - Beneficio: AutoReturn funciona en producción (HTTPS)
   - Esfuerzo: Bajo (10 min)

5. **Actualizar SecurityConfig para rutas duales** 🟠
   - Beneficio: Mayor compatibilidad
   - Esfuerzo: Bajo (5 min)

### Prioridad BAJA (Opcional)

6. **Configurar webhooks en staging/producción** ⚪
   - Solo necesario en ambientes con HTTPS
   - Polling funciona como fallback

---

## 📚 Referencias

### Documentación Interna

```
packedgo/back/payment-service/WEBHOOK-GUIDE.md
packedgo/back/payment-service/QUICKSTART.md
packedgo/back/payment-service/SECURITY.md
packedgo/back/DEBUG-PAYMENT-FLOW.md
```

### MercadoPago Docs

- [Webhooks](https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks)
- [Testing in Sandbox](https://www.mercadopago.com.ar/developers/es/docs/checkout-api/integration-test)
- [Preferences API](https://www.mercadopago.com.ar/developers/es/reference/preferences/_checkout_preferences/post)

---

## 🐛 Troubleshooting Rápido

### Error: "Token JWT inválido"
**Causa:** Token expirado o no enviado
**Solución:**
1. Verificar que AuthInterceptor agrega el header
2. Verificar expiración del token (console.log del payload)
3. Hacer login nuevamente

### Error: "Payment not found"
**Causa:** orderId incorrecto o pago no creado
**Solución:**
1. Verificar en BD: `SELECT * FROM payments WHERE order_id='ORD-...'`
2. Verificar logs de payment-service
3. Verificar que checkout creó la orden correctamente

### Error: URL malformada en retorno
**Causa:** Bug de concatenación en payment.service.ts
**Solución:** Aplicar fix de fix/mercadopago (ver Problema 1)

### Pago aprobado pero no detectado
**Causa:** Webhook no llegó y no hay polling
**Solución:**
1. Presionar botón "Verificar mi pago" manualmente
2. Implementar polling automático (ver Problema 2)
3. Configurar ngrok para webhooks

---

## 📝 Notas Importantes

### SessionId
- ✅ **NO es un error** - es una feature correcta
- ✅ Necesario para checkout multi-admin
- ✅ Permite persistencia y tracking
- ✅ Expira a los 30 minutos (normal)

### Webhooks
- ⚠️ NO funcionan en localhost (normal)
- ✅ Polling es la solución para desarrollo
- ✅ Webhooks solo necesarios en staging/producción

### Seguridad JWT
- ✅ Restaurada HOY en `/payments/create`
- ✅ Protege contra creación no autorizada de preferencias
- ✅ Valida que usuario esté autenticado

### AutoReturn
- ⚠️ Actualmente deshabilitado siempre
- ✅ fix/mercadopago lo habilita solo en HTTPS
- ✅ Polling compensa cuando autoReturn no está disponible

---

## 🎨 Diagrama de Componentes

```
┌─────────────────────────────────────────────────────┐
│                    Frontend (Angular)                │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────┐│
│  │   Checkout   │  │   Payment    │  │    Auth    ││
│  │  Component   │──│   Service    │──│Interceptor ││
│  └──────────────┘  └──────────────┘  └────────────┘│
└────────────┬────────────────────────────────────────┘
             │
             │ HTTP + JWT
             v
┌─────────────────────────────────────────────────────┐
│              API Gateway (Port 8080)                 │
└────────────┬────────────────────────────────────────┘
             │
        ┌────┼────┐
        │    │    │
        v    v    v
┌──────────┐┌──────────┐┌───────────┐
│  Order   ││ Payment  ││   Auth    │
│ Service  ││ Service  ││  Service  │
│ :8084    ││ :8085    ││  :8081    │
└────┬─────┘└────┬─────┘└───────────┘
     │           │
     │           │ SDK
     │           v
     │    ┌──────────────┐
     │    │  MercadoPago │
     │    │   Sandbox    │
     │    └──────────────┘
     │
     v
┌──────────┐
│  Event   │
│ Service  │
│ :8083    │
└──────────┘
```

---

## ✉️ Contacto y Notas Finales

**Documento creado por:** Claude Code (AI Assistant)
**Fecha:** 2025-11-04
**Propósito:** Contexto completo para otra IA trabajando en el proyecto

### Cambios Aplicados Hoy (Resumen)
1. ✅ Restauración de JWT required en PaymentController.java
2. ✅ Documentación completa del sistema en este archivo

### Próximos Pasos Sugeridos
1. Aplicar fix de URLs en payment.service.ts (5 min) 🔴
2. Implementar polling en checkout.component.ts (30 min) 🟡
3. Testear flujo completo de checkout multi-admin 🧪

---

**Fin del Documento de Contexto**
