# ========================================
# RESUMEN: STRIPE CLI INSTALADO Y CONFIGURADO
# ========================================

## ✅ LO QUE SE HIZO:

1. **Descargado Stripe CLI v1.21.8**
   - Ubicación: C:\Users\Agustin\stripe-cli\stripe.exe
   - Agregado al PATH del sistema

2. **Autenticado con Stripe**
   - Cuenta: Entorno de prueba de PackedGo
   - Account ID: acct_1STBhICs02rkj5ed
   - Válido por 90 días

3. **Webhook Listener Activo**
   - Terminal ID: d3d61621-2056-481b-ab1c-c75686c0b034
   - Escuchando en: http://localhost:8085/api/webhooks/stripe
   - Webhook Secret: whsec_8c0d91651ba797412266b4297c822f5123bfb978454f16b5328628e5b0abcec8

4. **Configuración actualizada en payment-service**
   - STRIPE_SECRET_KEY: ✅ Configurado
   - STRIPE_WEBHOOK_SECRET: ✅ Configurado
   - FRONTEND_URL: ✅ Configurado
   - Servicio reiniciado: ✅

## 🎯 PRÓXIMOS PASOS PARA PROBAR:

### 1. Verificar que payment-service esté corriendo:
```powershell
curl http://localhost:8085/api/actuator/health
```

### 2. Obtener un JWT token (necesitas estar logueado):
```powershell
# Ejemplo (debes usar tus credenciales reales):
$body = @{
    email = "tu_email@example.com"
    password = "tu_password"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $body -ContentType "application/json"
$token = $response.token
Write-Host "Token: $token"
```

### 3. Crear un checkout de Stripe:
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

$paymentBody = @{
    orderId = "TEST-ORDER-001"
    amount = 1500.00
    description = "Test Stripe Payment"
    adminId = 1
} | ConvertTo-Json

$paymentResponse = Invoke-RestMethod -Uri "http://localhost:8085/api/payments/create-checkout-stripe" -Method POST -Headers $headers -Body $paymentBody
Write-Host "Checkout URL: $($paymentResponse.checkoutUrl)"
```

### 4. Abrir el checkout y pagar:
- Abre la URL del checkout en tu navegador
- Usa tarjeta de prueba: **4242 4242 4242 4242**
- Fecha: cualquier fecha futura (ej: 12/30)
- CVC: 123
- Completa el pago

### 5. Verificar el webhook:
En la terminal donde está corriendo `stripe listen` verás:
```
[200] POST /api/webhooks/stripe [evt_xxx]
checkout.session.completed
```

### 6. Verificar logs del payment-service:
```powershell
docker logs back-payment-service-1 --tail 30 -f
```

Deberías ver:
```
🔷 Webhook recibido de Stripe
✅ Firma de webhook verificada
🔷 Procesando evento checkout.session.completed
✅ Pago Stripe aprobado
```

## 📋 COMANDOS ÚTILES:

### Ver status del Stripe listener:
La terminal con ID d3d61621-2056-481b-ab1c-c75686c0b034 debe seguir corriendo.

### Si necesitas reiniciar el listener:
```powershell
& "$env:USERPROFILE\stripe-cli\stripe.exe" listen --forward-to http://localhost:8085/api/webhooks/stripe
```
(Y actualizar el nuevo `whsec_...` en el .env)

### Ver logs del payment-service en tiempo real:
```powershell
docker logs back-payment-service-1 -f
```

### Reiniciar payment-service:
```powershell
cd C:\Users\Agustin\Documents\GitHub\PS-Agus\ps-packedgo\packedgo\back
docker-compose restart payment-service
```

## 🔑 CLAVES CONFIGURADAS:

**Archivo:** `packedgo/back/payment-service/.env`

```env
STRIPE_SECRET_KEY=sk_test_51STBhICs02rkj5ed...
STRIPE_WEBHOOK_SECRET=whsec_8c0d91651ba797412266b4297c822f5123bfb978454f16b5328628e5b0abcec8
FRONTEND_URL=http://localhost:4200
```

## ⚠️ IMPORTANTE:

1. **El listener de Stripe CLI debe estar corriendo** para que los webhooks funcionen
2. **El webhook secret cambia cada vez que ejecutas `stripe listen`** - debes actualizarlo en .env
3. **Para producción** necesitarás configurar el webhook directamente en el Dashboard de Stripe

## 🎉 ¡LISTO PARA PROBAR!

Tu integración de Stripe está completamente configurada. Ahora puedes:
1. Crear pagos con `/api/payments/create-checkout-stripe`
2. Los usuarios completan el pago en Stripe
3. El webhook notifica automáticamente a tu backend
4. El Payment se marca como APPROVED en tu DB
5. Order Service recibe la notificación

---

**Terminal del Stripe Listener:** d3d61621-2056-481b-ab1c-c75686c0b034 (debe seguir corriendo)
