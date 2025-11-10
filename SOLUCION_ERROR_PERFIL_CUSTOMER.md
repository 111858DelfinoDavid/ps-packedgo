# 🔧 SOLUCIÓN: Error al cargar perfil de usuario

## 📋 PROBLEMA REAL IDENTIFICADO (ACTUALIZADO)

El error "localhost:3000 dice: Error al cargar el perfil" ocurría porque:

### **CAUSA RAÍZ: URL DUPLICADA EN LA CONFIGURACIÓN** ❌

El `auth-service` estaba intentando llamar a:
```
http://users-service:8082/api/api/user-profiles/from-auth
```

¿Notaste el `/api` duplicado? Esto causaba error 404 y el perfil nunca se creaba.

**Configuración incorrecta en `.env` del auth-service:**
```bash
# ❌ INCORRECTO
USERS_SERVICE_URL=http://users-service:8082/api
```

El código luego agregaba otra vez `/api`:
```java
webClient.post()
    .uri("/api/user-profiles/from-auth")  // ← /api adicional
```

Resultando en: `/api/api/user-profiles/from-auth` 🔴

---

## ✅ SOLUCIÓN APLICADA

### **Paso 1: Corregir configuración**

**Archivo:** `packedgo/back/auth-service/.env`

```bash
# ✅ CORRECTO
USERS_SERVICE_URL=http://users-service:8082
```

### **Paso 2: Reiniciar servicio**

```bash
cd packedgo/back
docker-compose restart auth-service
```

### **Paso 3: Crear perfil para usuarios existentes**

Para usuarios ya registrados antes del fix, crear perfil manualmente:

```sql
-- Conectar a la base de datos
docker exec -it back-users-db-1 psql -U users_user -d users_db

-- Insertar perfil (reemplazar valores)
INSERT INTO user_profiles (
    auth_user_id, 
    document, 
    name, 
    last_name, 
    born_date, 
    telephone, 
    gender, 
    profile_image_url, 
    is_active, 
    created_at, 
    updated_at
) VALUES (
    4,              -- auth_user_id del usuario
    33333333,       -- documento
    'Nombre',       -- nombre
    'Apellido',     -- apellido
    '1990-01-01',   -- fecha de nacimiento
    '3515551234',   -- teléfono
    'MALE',         -- género: MALE, FEMALE, OTHER
    '',             -- URL imagen de perfil (vacío por ahora)
    true,           -- activo
    NOW(),          -- fecha creación
    NOW()           -- fecha actualización
);
```

---

## 🎯 VERIFICACIÓN

### 1. Verificar que el perfil existe

```powershell
curl http://localhost:8082/api/user-profiles/active | ConvertFrom-Json | Format-Table
```

Debe mostrar al menos un perfil con tu `authUserId`.

### 2. Probar en el navegador

1. Recarga el dashboard (F5)
2. Ve a "Mi Perfil"
3. Deberías ver tus datos cargados ✅

### 3. Verificar logs (opcional)

```powershell
# Ver si hay errores
docker logs back-auth-service-1 --tail=50 | Select-String "profile"
docker logs back-users-service-1 --tail=50 | Select-String "authUserId"
```

---

## 📝 NOTA IMPORTANTE

**Para nuevos registros:**
- ✅ El problema está corregido
- ✅ Los perfiles se crearán automáticamente
- ✅ No necesitas hacer nada manual

**Para usuarios existentes:**
- ⚠️ Necesitan crear el perfil manualmente (SQL de arriba)
- O pueden registrarse nuevamente con datos diferentes

---

## 🎯 MEJORAS IMPLEMENTADAS EN EL CÓDIGO

He mejorado el frontend para que muestre mensajes más claros:

1. **Mensajes de error detallados** con emojis 🔴
2. **Diagnóstico en consola** con información de debugging
3. **Detección específica de error 404** (perfil no encontrado)
4. **Manejo de errores de conexión** a cada servicio
5. **@CrossOrigin mejorado** en los controladores del backend

---

## 📝 NOTAS IMPORTANTES

- **No elimines tu token JWT** mientras pruebas, lo necesitas para las llamadas a la API
- Si creas un nuevo usuario, usa **datos únicos** (DNI, email, username diferentes)
- Los servicios deben estar corriendo antes de intentar estas soluciones
- El perfil se crea automáticamente **solo durante el registro**, no durante el login

---

## 🆘 SI NADA FUNCIONA

Reinicia completamente el sistema:

```powershell
# Detener todo
cd packedgo\back
docker-compose down

# Limpiar volúmenes (CUIDADO: borra datos)
docker-compose down -v

# Iniciar de nuevo
docker-compose up -d auth-service users-service

# Esperar 20 segundos
Start-Sleep -Seconds 20

# Registrar nuevo usuario
# Ir al navegador → consumer-register.html
```

---

**Fecha:** 7 de noviembre de 2025  
**Archivos modificados:**
- `packedgo/front/consumer-dashboard.html` (mejoras en manejo de errores)
- `packedgo/back/users-service/src/main/java/.../UserProfileController.java` (@CrossOrigin)
- `diagnostico-perfil.ps1` (script de diagnóstico)
