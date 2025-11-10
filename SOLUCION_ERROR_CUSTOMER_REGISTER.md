# ✅ SOLUCIÓN: Error al registrar Customer

## 🐛 Problema
Al intentar registrar un customer, el frontend mostraba: **"An unexpected error occurred"**

## 🔍 Diagnóstico

### 1. **Auth Service registraba el usuario correctamente**
   - El usuario se creaba en `auth_db` sin problemas
   - isEmailVerified = true (auto-verificado para desarrollo)

### 2. **Fallo silencioso al llamar a Users Service**
   - Auth Service intentaba crear el perfil de usuario en Users Service
   - La llamada HTTP fallaba pero el error se capturaba y no se propagaba
   
### 3. **URL incorrecta del WebClient**
   ```properties
   # ❌ ANTES (sin contexto path)
   USERS_SERVICE_URL=http://users-service:8082
   
   # ✅ DESPUÉS (con contexto path /api)
   USERS_SERVICE_URL=http://users-service:8082/api
   ```

### 4. **El endpoint correcto es:**
   ```
   POST http://users-service:8082/api/user-profiles/from-auth
   ```
   
   Pero el WebClient estaba intentando:
   ```
   POST http://users-service:8082/api/user-profiles/from-auth
   ```
   
   Sin el contexto path `/api` en la URL base, la petición iba a:
   ```
   POST http://users-service:8082/api/user-profiles/from-auth (404 Not Found)
   ```

---

## ✅ Solución Aplicada

### Paso 1: Corregir la URL en `.env`
**Archivo:** `packedgo/back/auth-service/.env`

```properties
# External Services Configuration
USERS_SERVICE_URL=http://users-service:8082/api
```

### Paso 2: Reiniciar Auth Service
```powershell
cd C:\Users\david\Documents\ps-packedgo\packedgo\back
docker compose restart auth-service
```

---

## 🎯 Resultado

✅ **Ahora el flujo completo funciona:**

1. Customer se registra en el frontend
2. Auth Service crea el usuario en `auth_db`
3. Auth Service llama correctamente a Users Service
4. Users Service crea el perfil en `users_db`
5. El registro se completa exitosamente

---

## 🔍 Cómo verificar que funciona

### 1. **Ver logs de Auth Service durante el registro:**
```powershell
docker compose logs auth-service -f
```

Deberías ver:
```
INFO  - Customer registered successfully with ID: X
INFO  - Calling users-service to create profile for authUserId: X
INFO  - Successfully created user profile for authUserId: X
INFO  - ✅ User registered and auto-verified (development mode) - ID: X
```

### 2. **Verificar en la base de datos:**
```powershell
# Auth DB
docker exec back-auth-db-1 psql -U auth_user -d auth_db -c "SELECT id, username, email, role FROM auth_users WHERE role='CUSTOMER';"

# Users DB
docker exec back-users-db-1 psql -U users_user -d users_db -c "SELECT * FROM user_profiles;"
```

---

## 📝 Notas Importantes

### Contexto Path en todos los servicios:
Todos los microservicios tienen `server.servlet.context-path=/api`, por lo tanto:

| Servicio | Puerto | URL Base |
|----------|--------|----------|
| Auth Service | 8081 | `http://auth-service:8081/api` |
| Users Service | 8082 | `http://users-service:8082/api` |
| Event Service | 8086 | `http://event-service:8086/api` |
| Order Service | 8084 | `http://order-service:8084/api` |
| Payment Service | 8085 | `http://payment-service:8085/api` |
| Consumption Service | 8088 | `http://consumption-service:8088/api` |

### WebClient Configuration:
El `baseUrl` del WebClient siempre debe incluir el contexto path `/api`:

```java
@Bean
public WebClient usersServiceWebClient() {
    return WebClient.builder()
            .baseUrl("http://users-service:8082/api")  // ✅ Con /api
            .build();
}
```

---

## 🚨 Problema Similar en Otros Servicios

Si encuentras errores similares en otros microservicios, revisa:

1. ✅ Las URLs en archivos `.env` incluyen el contexto path `/api`
2. ✅ Los WebClient/RestTemplate están configurados con la URL base correcta
3. ✅ Los endpoints llamados existen y son accesibles

---

**✅ Problema resuelto - Ahora puedes registrar customers sin errores**
