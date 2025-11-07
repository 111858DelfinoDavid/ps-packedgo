# ========================================
# Guía: Solución al Problema de Verificación de Identidad
# MercadoPago - Usuarios de Prueba
# ========================================

## 🔴 Problema

Al intentar completar un pago con MercadoPago en modo sandbox usando usuarios de prueba (test_user),
el sistema solicita un código de verificación de identidad (SMS o Email).

## ✅ Soluciones

### Solución 1: Usar Tarjetas sin Login (RECOMENDADO)

**No uses usuarios de prueba**, en su lugar:

1. Genera tu preferencia de pago normalmente
2. Abre el link de pago (sandboxInitPoint)
3. **Selecciona "Pagar como invitado"**
4. Ingresa directamente una tarjeta de prueba:

#### Tarjetas de Prueba de MercadoPago (Argentina)

| Tarjeta | Número | Resultado | CVV | Vencimiento |
|---------|--------|-----------|-----|-------------|
| Mastercard | `5031 7557 3453 0604` | ✅ Aprobado | 123 | 11/25 |
| Visa | `4509 9535 6623 3704` | ✅ Aprobado | 123 | 11/25 |
| Visa | `4013 5406 8274 6260` | ⏳ Pendiente | 123 | 11/25 |
| Visa | `4074 5957 4392 9691` | ❌ Rechazado | 123 | 11/25 |

**Datos adicionales:**
- Nombre: APRO (para aprobado) o OTHE (para otros)
- DNI: 12345678
- Email: cualquier@email.com

### Solución 2: Código de Verificación Correcto

Si decides usar usuarios de prueba con login, el código de verificación SIEMPRE es:

```
123456
```

Tanto para SMS como para Email de verificación.

### Solución 3: Crear Usuarios Sin Verificación (API)

Puedes crear usuarios de prueba programáticamente usando la API de MercadoPago:

```powershell
# Reemplaza con tu Access Token de PRODUCCIÓN (APP_USR-)
$accessToken = "TU_ACCESS_TOKEN_DE_PRODUCCION"

$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

$body = @{
    site_id = "MLA"  # Argentina (MLA), Chile (MLC), etc.
} | ConvertTo-Json

# Crear usuario vendedor
$seller = Invoke-RestMethod -Uri "https://api.mercadopago.com/users/test_user" `
    -Method Post `
    -Headers $headers `
    -Body $body

Write-Host "VENDEDOR (SELLER):"
Write-Host "Email   : $($seller.email)"
Write-Host "Password: $($seller.password)"

# Crear usuario comprador
$buyer = Invoke-RestMethod -Uri "https://api.mercadopago.com/users/test_user" `
    -Method Post `
    -Headers $headers `
    -Body $body

Write-Host ""
Write-Host "COMPRADOR (BUYER):"
Write-Host "Email   : $($buyer.email)"
Write-Host "Password: $($buyer.password)"
```

**Importante:** Necesitas tu Access Token de PRODUCCIÓN para crear usuarios de prueba.

### Solución 4: Desactivar Verificación en Panel de Desarrolladores

1. Ve a: https://www.mercadopago.com.ar/developers/panel/test-users
2. Revisa tus usuarios de prueba
3. Verifica que tengan "Verificación desactivada"
4. Si tienen verificación activada, elimínalos y crea nuevos

## 📋 Script de Prueba Automático

Usa el script `test-payment-sin-verificacion.ps1` que evita este problema:

```powershell
cd c:\Users\david\Documents\ps-packedgo\packedgo\back\payment-service
.\test-payment-sin-verificacion.ps1
```

Este script:
1. ✅ Verifica que el servicio esté funcionando
2. ✅ Verifica credenciales del admin
3. ✅ Crea una preferencia de pago
4. ✅ Te muestra el link de pago
5. ✅ Te da las tarjetas de prueba que NO requieren verificación
6. ✅ Opcionalmente abre el navegador automáticamente
7. ✅ Verifica el estado del pago después

## 🎯 Flujo Recomendado

```
1. Ejecutar: .\test-payment-sin-verificacion.ps1
2. Copiar link de pago (sandboxInitPoint)
3. Abrir en navegador
4. Seleccionar "Pagar como invitado"
5. Tarjeta: 5031 7557 3453 0604
6. CVV: 123
7. Vencimiento: 11/25
8. Nombre: APRO
9. DNI: 12345678
10. Email: test@test.com
11. ✅ Completar pago
```

## 🔍 Verificar Pago en Base de Datos

```sql
-- Ver último pago creado
SELECT 
    id,
    order_id,
    status,
    amount,
    mercado_pago_id,
    created_at
FROM payments
ORDER BY created_at DESC
LIMIT 1;
```

## 🚨 Errores Comunes

### Error: "Invalid credentials"
- Verifica que hayas configurado las credenciales de sandbox correctamente
- Asegúrate de usar credenciales de TEST (empiezan con "TEST-" o "APP_USR-...")

### Error: "Payment requires authentication"
- Usa tarjetas directamente sin login
- O usa el código de verificación: 123456

### Error: "Payment rejected"
- Verifica que uses tarjetas de prueba válidas
- Prueba con: 5031 7557 3453 0604

## 📚 Referencias

- **Tarjetas de Prueba:** https://www.mercadopago.com.ar/developers/es/docs/checkout-api/testing
- **Usuarios de Prueba:** https://www.mercadopago.com.ar/developers/es/docs/checkout-api/testing/test-users
- **Credenciales:** https://www.mercadopago.com.ar/developers/panel/credentials

## 💡 Consejos

1. **Para desarrollo:** Usa tarjetas directamente (Solución 1)
2. **Para testing automatizado:** Crea usuarios vía API (Solución 3)
3. **Para debugging:** El código de verificación es siempre 123456 (Solución 2)

## ✅ Checklist

- [ ] Servicio corriendo en puerto 8082
- [ ] Base de datos PostgreSQL activa
- [ ] Credenciales de sandbox configuradas
- [ ] Script de prueba ejecutado
- [ ] Link de pago generado
- [ ] Tarjeta de prueba usada: 5031 7557 3453 0604
- [ ] Pago completado sin verificación
- [ ] Estado verificado en base de datos

---

**Última actualización:** 5 de noviembre de 2025
