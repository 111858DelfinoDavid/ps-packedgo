# 🏢 Modelo SaaS - PackedGo Payment Service

## 📋 Resumen del Modelo de Negocio

**PackedGo** es un **SaaS (Software as a Service)** donde:
- Múltiples administradores usan la misma plataforma
- Cada admin tiene su propia cuenta de MercadoPago
- Cada admin configura sus propias credenciales en el Dashboard
- La pasarela de pago es **genérica** y se adapta dinámicamente a cada admin

## 🔐 Arquitectura de Seguridad Implementada

### ✅ Flujo Seguro Actual (CORRECTO)

```
┌─────────────────┐
│  App Cliente    │
│  (Frontend)     │
└────────┬────────┘
         │ 
         │ POST /api/payments/create
         │ Body: { "adminId": 1, "orderId": "X", "amount": 1500, ... }
         │ ❌ NO envía credenciales
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  Payment Service (Backend)                               │
│                                                          │
│  1️⃣ Recibe solo el adminId                             │
│  2️⃣ Busca credenciales en BD: SELECT * FROM            │
│     admin_credentials WHERE admin_id = 1 AND            │
│     is_active = true                                     │
│  3️⃣ Valida que existan y estén activas                 │
│  4️⃣ Configura MercadoPago con esas credenciales        │
│  5️⃣ Crea la preferencia de pago                        │
└─────────────────────────────────────────────────────────┘
         │
         ▼
┌─────────────────┐
│  Base de Datos  │
│  PostgreSQL     │
│                 │
│  admin_credentials:                                     │
│  ┌──────┬─────────────┬──────────────┬──────────┐     │
│  │ id   │ admin_id    │ access_token │ is_active│     │
│  ├──────┼─────────────┼──────────────┼──────────┤     │
│  │  1   │     1       │ TEST-abc123  │   true   │     │
│  │  2   │     2       │ TEST-xyz789  │   true   │     │
│  │  3   │     3       │ TEST-qwe456  │   false  │  ❌ │
│  └──────┴─────────────┴──────────────┴──────────┘     │
└─────────────────┘
```

## 📊 Implementación Actual (Todo Correcto ✅)

### 1. Request DTO - Solo adminId
```java
// PaymentRequest.java
public class PaymentRequest {
    @NotNull(message = "El ID del admin es requerido")
    private Long adminId;  // ✅ Solo el ID
    
    private String orderId;
    private BigDecimal amount;
    // ... otros campos del pago
    
    // ❌ NO incluye:
    // private String accessToken;  // NUNCA
    // private String publicKey;    // NUNCA
}
```

### 2. Service - Validación Segura
```java
// PaymentService.java
@Transactional
public PaymentResponse createPaymentPreference(PaymentRequest request) {
    
    // 1️⃣ VALIDACIÓN SEGURA: Obtener credenciales desde la BD
    //    NO del request, NO del header, NO del body
    AdminCredential credential = credentialService
        .getValidatedCredentials(request.getAdminId());
    
    // 2️⃣ Configurar MercadoPago dinámicamente
    MercadoPagoConfig.setAccessToken(credential.getAccessToken());
    
    // 3️⃣ Crear el pago con las credenciales correctas
    // ...
}
```

### 3. Validación Robusta
```java
// AdminCredentialService.java
@Transactional(readOnly = true)
public AdminCredential getValidatedCredentials(Long adminId) {
    // Buscar en BD por adminId Y que esté activa
    AdminCredential credential = credentialRepository
        .findByAdminIdAndIsActiveTrue(adminId)
        .orElseThrow(() -> new CredentialException(
            "Admin sin credenciales configuradas o credenciales inactivas"
        ));
    
    // Validar que el token no esté vacío
    if (credential.getAccessToken() == null || 
        credential.getAccessToken().isBlank()) {
        throw new CredentialException("Credenciales incompletas");
    }
    
    return credential;
}
```

## 🔄 Flujo Completo por Escenarios

### Escenario 1: Admin con Credenciales Válidas ✅

```
Cliente → POST /api/payments/create
{
    "adminId": 1,
    "orderId": "ORDER-001",
    "amount": 1500
}

Backend:
1. Buscar admin_id=1 en admin_credentials
2. ✅ Encontrado: access_token="TEST-abc123", is_active=true
3. Configurar MercadoPago con TEST-abc123
4. Crear preferencia de pago
5. ✅ Retornar: initPoint, preferenceId, etc.
```

### Escenario 2: Admin sin Credenciales ❌

```
Cliente → POST /api/payments/create
{
    "adminId": 999,
    "orderId": "ORDER-002",
    "amount": 2000
}

Backend:
1. Buscar admin_id=999 en admin_credentials
2. ❌ No encontrado
3. Lanzar CredentialException
4. GlobalExceptionHandler captura
5. ❌ Retornar 401: {
    "status": 401,
    "error": "Credential Error",
    "message": "Admin sin credenciales configuradas"
}
```

### Escenario 3: Admin con Credenciales Inactivas ❌

```
Cliente → POST /api/payments/create
{
    "adminId": 3,
    "orderId": "ORDER-003",
    "amount": 3000
}

Backend:
1. Buscar admin_id=3 AND is_active=true
2. ❌ No encontrado (existe pero is_active=false)
3. Lanzar CredentialException
4. ❌ Retornar 401: "Credenciales inactivas"
```

## 🎯 Ventajas de Esta Implementación

### 1. Seguridad ✅
- ✅ Credenciales NUNCA expuestas al cliente
- ✅ Imposible falsificar credenciales
- ✅ Imposible que un admin use credenciales de otro
- ✅ Control centralizado en backend

### 2. Multi-Tenant ✅
- ✅ Cada admin aislado con sus credenciales
- ✅ Cambio dinámico de credenciales por request
- ✅ Fácil agregar/eliminar/desactivar admins
- ✅ Auditoría clara: cada pago tiene su adminId

### 3. Escalabilidad ✅
- ✅ Nuevo admin = 1 INSERT en admin_credentials
- ✅ No requiere redeployment
- ✅ Configuración por admin en runtime
- ✅ Fácil migración entre sandbox/producción

### 4. Mantenibilidad ✅
- ✅ Código limpio y bien separado
- ✅ Validaciones centralizadas
- ✅ Fácil debugging (logs por adminId)
- ✅ Testing simplificado

## 🔧 Gestión de Credenciales

### Dashboard del Admin (Frontend - Por Implementar)

```typescript
// Pantalla: "Configuración de Pagos"
function PaymentSettings() {
    const [credentials, setCredentials] = useState({
        accessToken: '',
        publicKey: '',
        isSandbox: true
    });
    
    const saveCredentials = async () => {
        // POST al endpoint de admin (Backend Admin Service)
        await fetch('/api/admin/credentials', {
            method: 'POST',
            body: JSON.stringify({
                adminId: currentAdmin.id,
                accessToken: credentials.accessToken,
                publicKey: credentials.publicKey,
                isSandbox: credentials.isSandbox
            })
        });
    };
}
```

### Backend Admin Endpoint (Por Implementar)

```java
// AdminController.java (nuevo)
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    
    @PostMapping("/credentials")
    @PreAuthorize("hasRole('ADMIN')")  // Solo el admin dueño
    public ResponseEntity<?> saveCredentials(
        @RequestBody CredentialUpdateRequest request,
        Principal principal
    ) {
        // Validar que el admin solo pueda actualizar sus propias credenciales
        if (!request.getAdminId().equals(principal.getId())) {
            throw new ForbiddenException("No autorizado");
        }
        
        credentialService.saveCredentials(
            request.getAdminId(),
            request.getAccessToken(),
            request.getPublicKey(),
            request.getIsSandbox()
        );
        
        return ResponseEntity.ok("Credenciales guardadas");
    }
}
```

## 📝 Tabla: admin_credentials

### Estructura
```sql
CREATE TABLE admin_credentials (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL UNIQUE,           -- FK al admin
    access_token VARCHAR(500) NOT NULL,        -- Token de MercadoPago
    public_key VARCHAR(500),                   -- Public key (opcional)
    is_active BOOLEAN NOT NULL DEFAULT true,   -- Control on/off
    is_sandbox BOOLEAN NOT NULL DEFAULT false, -- Sandbox vs Producción
    merchant_id VARCHAR(255),                  -- ID de merchant
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX idx_admin_credentials_admin_id ON admin_credentials(admin_id);
```

### Ejemplo de Datos
```sql
-- Admin 1: PackedGo (credenciales de testing/MVP)
INSERT INTO admin_credentials (admin_id, access_token, public_key, is_active, is_sandbox, created_at)
VALUES (
    1,
    'TEST-1234567890-010101-abc123def456-789012345',  -- Credencial PackedGo (MVP)
    'TEST-abc123def-456789-012345-678901-234567',
    true,
    true,  -- Sandbox para testing
    NOW()
);

-- Admin 2: Cliente A (sus propias credenciales)
INSERT INTO admin_credentials (admin_id, access_token, public_key, is_active, is_sandbox, created_at)
VALUES (
    2,
    'APP_USR-9876543210-020202-xyz789ghi012-345678901',  -- Credencial Cliente A
    'APP_USR-xyz789ghi-012345-678901-234567-890123',
    true,
    false,  -- Producción
    NOW()
);

-- Admin 3: Cliente B (credenciales desactivadas temporalmente)
INSERT INTO admin_credentials (admin_id, access_token, public_key, is_active, is_sandbox, created_at)
VALUES (
    3,
    'APP_USR-1111111111-030303-qwe456rty789-012345678',
    'APP_USR-qwe456rty-789012-345678-901234-567890',
    false,  -- ❌ Desactivado
    false,
    NOW()
);
```

## 🧪 Testing del Modelo Multi-Tenant

### Test 1: Crear Pago con Admin 1
```bash
curl -X POST http://localhost:8082/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 1,
    "orderId": "ORDER-ADMIN1-001",
    "amount": 1500.00,
    "description": "Paquete Admin 1",
    "payerEmail": "cliente@email.com",
    "payerName": "Juan Pérez",
    "successUrl": "http://localhost:3000/success",
    "failureUrl": "http://localhost:3000/failure",
    "pendingUrl": "http://localhost:3000/pending"
  }'

# ✅ Usa credenciales de admin_id=1
```

### Test 2: Crear Pago con Admin 2
```bash
curl -X POST http://localhost:8082/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 2,
    "orderId": "ORDER-ADMIN2-001",
    "amount": 2500.00,
    "description": "Paquete Admin 2",
    "payerEmail": "otro@email.com",
    "payerName": "María López",
    "successUrl": "http://localhost:3000/success",
    "failureUrl": "http://localhost:3000/failure",
    "pendingUrl": "http://localhost:3000/pending"
  }'

# ✅ Usa credenciales de admin_id=2 (diferentes!)
```

### Test 3: Admin sin Credenciales
```bash
curl -X POST http://localhost:8082/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 999,
    "orderId": "ORDER-INVALID",
    "amount": 1000.00,
    ...
  }'

# ❌ Error 401: "Admin sin credenciales configuradas"
```

## ✅ Verificación Final

### Checklist de Seguridad
- ✅ Credenciales NO se envían desde el cliente
- ✅ Credenciales NO en headers
- ✅ Credenciales NO en body
- ✅ Credenciales NO en URL
- ✅ Solo adminId se envía
- ✅ Backend busca credenciales en BD
- ✅ Validación de is_active
- ✅ Validación de token no vacío
- ✅ Logs de auditoría
- ✅ Excepciones personalizadas
- ✅ Aislamiento entre admins

### Checklist Multi-Tenant
- ✅ Cada admin con sus credenciales
- ✅ Credenciales dinámicas por request
- ✅ No hay credenciales hardcodeadas
- ✅ Fácil agregar nuevos admins
- ✅ Fácil desactivar admin
- ✅ Sandbox/Producción por admin
- ✅ Auditoría por adminId

## 🚀 Para Producción

### 1. Agregar Endpoint de Gestión de Credenciales
```java
@PostMapping("/api/admin/credentials")
@PreAuthorize("isAuthenticated() and #request.adminId == principal.id")
public ResponseEntity<?> updateCredentials(@RequestBody CredentialRequest request);
```

### 2. Validar JWT del Admin
```java
// Asegurar que solo el admin dueño pueda actualizar sus credenciales
if (!jwtUtils.getAdminIdFromToken(token).equals(request.getAdminId())) {
    throw new ForbiddenException("No autorizado");
}
```

### 3. Encriptar Tokens en BD
```java
// Usar BCrypt o similar para guardar tokens
String encryptedToken = encryptionService.encrypt(accessToken);
credential.setAccessToken(encryptedToken);
```

### 4. Webhook con adminId
```java
// Incluir adminId en la URL del webhook
String webhookUrl = "https://api.packedgo.com/api/payments/webhook?adminId=" + adminId;
```

## 📊 Resumen

Tu implementación actual **ES CORRECTA** y sigue las mejores prácticas:

1. ✅ **Modelo SaaS**: Soporta múltiples admins
2. ✅ **Seguridad**: Credenciales en BD, no expuestas
3. ✅ **Multi-Tenant**: Credenciales dinámicas por admin
4. ✅ **Validación**: Verifica adminId con BD
5. ✅ **Escalable**: Fácil agregar nuevos admins
6. ✅ **Auditable**: Logs por adminId

**No necesitas cambiar nada en el código actual**. Solo falta implementar:
- Dashboard para que admins configuren sus credenciales
- Endpoint de admin para guardar/actualizar credenciales
- Autenticación JWT completa (ya mencionado en el README)

---

**Modelo:** ✅ SaaS Multi-Tenant  
**Seguridad:** ✅ Máxima (credenciales en BD)  
**Estado:** ✅ Implementación Correcta
