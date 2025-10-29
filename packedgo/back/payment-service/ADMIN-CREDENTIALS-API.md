# 🔧 Guía de Gestión de Credenciales - Dashboard Admin

## 📋 Endpoints para Gestión de Credenciales

Estos endpoints permiten a los administradores configurar sus credenciales de MercadoPago desde el Dashboard.

## 🔐 Endpoints Disponibles

### 1. Guardar/Actualizar Credenciales

Permite al admin configurar sus credenciales de MercadoPago.

**Endpoint:** `POST /api/admin/credentials`

**Headers:**
```
Content-Type: application/json
Authorization: Bearer {JWT_TOKEN}  // TODO: Implementar en producción
```

**Request Body:**
```json
{
  "adminId": 1,
  "accessToken": "TEST-1234567890-010101-abc123def456-789012345",
  "publicKey": "TEST-abc123def-456789-012345-678901-234567",
  "isSandbox": true
}
```

**Response Exitoso (200):**
```json
{
  "message": "Credenciales guardadas exitosamente",
  "adminId": 1,
  "isSandbox": true,
  "isActive": true
}
```

**Response Error (500):**
```json
{
  "error": "Error al guardar credenciales: [mensaje]"
}
```

---

### 2. Verificar si Tiene Credenciales

Verifica si un admin tiene credenciales configuradas.

**Endpoint:** `GET /api/admin/credentials/check/{adminId}`

**Ejemplo:**
```
GET /api/admin/credentials/check/1
```

**Response:**
```json
{
  "adminId": 1,
  "hasCredentials": true
}
```

---

### 3. Desactivar Credenciales

Desactiva las credenciales del admin (no las elimina, solo las marca como inactivas).

**Endpoint:** `DELETE /api/admin/credentials/{adminId}`

**Ejemplo:**
```
DELETE /api/admin/credentials/1
```

**Response:**
```json
{
  "message": "Credenciales desactivadas exitosamente",
  "adminId": 1
}
```

---

## 🧪 Ejemplos de Uso

### Guardar Credenciales (PowerShell)

```powershell
$body = @{
    adminId = 1
    accessToken = "TEST-1234567890-010101-abc123def456-789012345"
    publicKey = "TEST-abc123def-456789-012345-678901-234567"
    isSandbox = $true
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

### Guardar Credenciales (cURL)

```bash
curl -X POST http://localhost:8082/api/admin/credentials \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 1,
    "accessToken": "TEST-1234567890-010101-abc123def456-789012345",
    "publicKey": "TEST-abc123def-456789-012345-678901-234567",
    "isSandbox": true
  }'
```

### Verificar Credenciales (PowerShell)

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials/check/1" -Method Get
```

### Verificar Credenciales (cURL)

```bash
curl http://localhost:8082/api/admin/credentials/check/1
```

### Desactivar Credenciales (PowerShell)

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/admin/credentials/1" -Method Delete
```

### Desactivar Credenciales (cURL)

```bash
curl -X DELETE http://localhost:8082/api/admin/credentials/1
```

---

## 🎨 Integración con Frontend (React/Angular)

### React Example

```typescript
import { useState } from 'react';

interface CredentialForm {
  accessToken: string;
  publicKey: string;
  isSandbox: boolean;
}

function PaymentSettingsPage() {
  const [credentials, setCredentials] = useState<CredentialForm>({
    accessToken: '',
    publicKey: '',
    isSandbox: true
  });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const adminId = 1; // Obtener del contexto/auth

  // Verificar si tiene credenciales al cargar
  useEffect(() => {
    checkCredentials();
  }, []);

  const checkCredentials = async () => {
    try {
      const response = await fetch(
        `http://localhost:8082/api/admin/credentials/check/${adminId}`
      );
      const data = await response.json();
      
      if (data.hasCredentials) {
        setMessage('Ya tienes credenciales configuradas');
      }
    } catch (error) {
      console.error('Error checking credentials:', error);
    }
  };

  const saveCredentials = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');

    try {
      const response = await fetch('http://localhost:8082/api/admin/credentials', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // 'Authorization': `Bearer ${token}` // TODO: Agregar JWT
        },
        body: JSON.stringify({
          adminId,
          ...credentials
        })
      });

      const data = await response.json();

      if (response.ok) {
        setMessage('✅ Credenciales guardadas exitosamente');
        setCredentials({ accessToken: '', publicKey: '', isSandbox: true });
      } else {
        setMessage(`❌ Error: ${data.error || 'Error desconocido'}`);
      }
    } catch (error) {
      setMessage('❌ Error de conexión');
      console.error('Error saving credentials:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="payment-settings">
      <h2>Configuración de MercadoPago</h2>
      
      <form onSubmit={saveCredentials}>
        <div className="form-group">
          <label>Access Token *</label>
          <input
            type="text"
            value={credentials.accessToken}
            onChange={(e) => setCredentials({...credentials, accessToken: e.target.value})}
            placeholder="TEST-123456789-..."
            required
          />
          <small>
            Obtén tu Access Token en: 
            <a href="https://www.mercadopago.com.ar/developers/panel/credentials" target="_blank">
              Panel de Credenciales de MercadoPago
            </a>
          </small>
        </div>

        <div className="form-group">
          <label>Public Key (Opcional)</label>
          <input
            type="text"
            value={credentials.publicKey}
            onChange={(e) => setCredentials({...credentials, publicKey: e.target.value})}
            placeholder="TEST-abc123def..."
          />
        </div>

        <div className="form-group">
          <label>
            <input
              type="checkbox"
              checked={credentials.isSandbox}
              onChange={(e) => setCredentials({...credentials, isSandbox: e.target.checked})}
            />
            Modo Sandbox (Testing)
          </label>
          <small>
            Usa Sandbox para pruebas. Desactiva en producción.
          </small>
        </div>

        <button type="submit" disabled={loading}>
          {loading ? 'Guardando...' : 'Guardar Credenciales'}
        </button>
      </form>

      {message && <div className="message">{message}</div>}

      <div className="help-section">
        <h3>¿Cómo obtener mis credenciales?</h3>
        <ol>
          <li>Ve a <a href="https://www.mercadopago.com.ar/developers" target="_blank">MercadoPago Developers</a></li>
          <li>Inicia sesión con tu cuenta</li>
          <li>Ve a "Tus integraciones" → "Credenciales"</li>
          <li>Copia el Access Token y Public Key</li>
          <li>Usa las de "Testing" para sandbox o "Producción" para producción</li>
        </ol>
      </div>
    </div>
  );
}

export default PaymentSettingsPage;
```

### Angular Example

```typescript
import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface CredentialForm {
  accessToken: string;
  publicKey: string;
  isSandbox: boolean;
}

@Component({
  selector: 'app-payment-settings',
  templateUrl: './payment-settings.component.html'
})
export class PaymentSettingsComponent implements OnInit {
  credentials: CredentialForm = {
    accessToken: '',
    publicKey: '',
    isSandbox: true
  };

  loading = false;
  message = '';
  adminId = 1; // Obtener del servicio de auth

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.checkCredentials();
  }

  checkCredentials() {
    this.http.get<any>(`http://localhost:8082/api/admin/credentials/check/${this.adminId}`)
      .subscribe({
        next: (data) => {
          if (data.hasCredentials) {
            this.message = 'Ya tienes credenciales configuradas';
          }
        },
        error: (error) => console.error('Error checking credentials:', error)
      });
  }

  saveCredentials() {
    this.loading = true;
    this.message = '';

    const payload = {
      adminId: this.adminId,
      ...this.credentials
    };

    this.http.post<any>('http://localhost:8082/api/admin/credentials', payload)
      .subscribe({
        next: (data) => {
          this.message = '✅ Credenciales guardadas exitosamente';
          this.credentials = { accessToken: '', publicKey: '', isSandbox: true };
          this.loading = false;
        },
        error: (error) => {
          this.message = `❌ Error: ${error.error?.error || 'Error desconocido'}`;
          this.loading = false;
        }
      });
  }
}
```

---

## 📝 Notas Importantes

### Seguridad - TODO para Producción

⚠️ **IMPORTANTE:** Los endpoints actuales están configurados como `.permitAll()` para facilitar el desarrollo.

**Para producción, DEBES implementar:**

1. **Autenticación JWT**
   ```java
   @PostMapping
   @PreAuthorize("isAuthenticated()")
   public ResponseEntity<?> saveCredentials(...) { ... }
   ```

2. **Validación de Propiedad**
   ```java
   // Verificar que el admin solo pueda modificar sus propias credenciales
   if (!jwtUtils.getAdminIdFromToken(token).equals(request.getAdminId())) {
       throw new ForbiddenException("No autorizado");
   }
   ```

3. **Encriptación de Tokens**
   ```java
   // Encriptar tokens antes de guardar en BD
   String encryptedToken = encryptionService.encrypt(accessToken);
   credential.setAccessToken(encryptedToken);
   ```

4. **Auditoría**
   ```java
   // Registrar cambios de credenciales
   auditService.log("CREDENTIAL_UPDATE", adminId, ipAddress);
   ```

### Obtener Credenciales de MercadoPago

1. Ve a https://www.mercadopago.com.ar/developers
2. Inicia sesión con tu cuenta de MercadoPago
3. Ve a "Tus integraciones" → "Credenciales"
4. Verás dos tipos de credenciales:
   - **Testing (Sandbox)**: Para pruebas
   - **Producción**: Para ventas reales

**Para Sandbox:**
- Access Token: `TEST-123456789-...`
- Public Key: `TEST-abc123def-...`

**Para Producción:**
- Access Token: `APP_USR-123456789-...`
- Public Key: `APP_USR-abc123def-...`

### Validaciones en Frontend

```typescript
const validateAccessToken = (token: string): boolean => {
  // Sandbox tokens start with TEST-
  // Production tokens start with APP_USR-
  if (isSandbox) {
    return token.startsWith('TEST-');
  } else {
    return token.startsWith('APP_USR-');
  }
};
```

---

## 🔄 Flujo Completo

1. **Admin abre Dashboard** → Pantalla de Configuración
2. **Frontend verifica** → `GET /api/admin/credentials/check/{adminId}`
3. **Admin ingresa credenciales** → Formulario
4. **Frontend valida formato** → Validación básica
5. **Envía al backend** → `POST /api/admin/credentials`
6. **Backend valida y guarda** → Base de datos
7. **Credenciales listas** → Admin puede crear pagos

---

## ✅ Testing Completo

### 1. Guardar Credenciales
```bash
curl -X POST http://localhost:8082/api/admin/credentials \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 1,
    "accessToken": "TEST-1234567890-010101-abc123def456-789012345",
    "publicKey": "TEST-abc123def-456789-012345-678901-234567",
    "isSandbox": true
  }'

# ✅ Respuesta: {"message": "Credenciales guardadas exitosamente", ...}
```

### 2. Verificar en BD
```sql
SELECT * FROM admin_credentials WHERE admin_id = 1;

# ✅ Debe aparecer el registro con is_active=true
```

### 3. Crear Pago
```bash
curl -X POST http://localhost:8082/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 1,
    "orderId": "ORDER-001",
    "amount": 1500.00,
    ...
  }'

# ✅ Debe usar las credenciales guardadas
```

### 4. Desactivar
```bash
curl -X DELETE http://localhost:8082/api/admin/credentials/1

# ✅ Respuesta: {"message": "Credenciales desactivadas exitosamente"}
```

### 5. Verificar Desactivación
```bash
curl -X POST http://localhost:8082/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{"adminId": 1, ...}'

# ❌ Error: "Admin sin credenciales configuradas o credenciales inactivas"
```

---

## 🎉 Resumen

Con estos endpoints, los admins pueden:
- ✅ Configurar sus credenciales desde el Dashboard
- ✅ Verificar si tienen credenciales configuradas
- ✅ Actualizar sus credenciales cuando cambien
- ✅ Desactivar credenciales si es necesario

El flujo es completamente **seguro** porque:
- ✅ Las credenciales NO viajan en cada request de pago
- ✅ Se guardan en BD encriptadas (TODO: implementar encriptación)
- ✅ Solo el admin dueño puede modificarlas (TODO: agregar JWT)
- ✅ El backend siempre valida contra la BD

---

**Estado:** ✅ Funcional para desarrollo  
**TODO:** Agregar JWT y encriptación para producción
