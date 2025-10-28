# Configuración de Webhook para Desarrollo Local

## Problema
MercadoPago necesita enviar notificaciones a tu servidor, pero tu aplicación local (`localhost:8082`) no es accesible desde Internet.

## Soluciones

### Opción 1: ngrok (Recomendado para desarrollo)

ngrok crea un túnel seguro que expone tu servidor local a Internet.

#### Instalación

**Windows:**
1. Descargar desde https://ngrok.com/download
2. Descomprimir en una carpeta
3. Agregar a PATH o ejecutar desde la carpeta

**Con Chocolatey:**
```powershell
choco install ngrok
```

#### Uso

1. Iniciar tu aplicación local:
```bash
mvn spring-boot:run
```

2. En otra terminal, iniciar ngrok:
```bash
ngrok http 8082
```

3. ngrok te dará una URL pública, por ejemplo:
```
Forwarding: https://abc123.ngrok.io -> http://localhost:8082
```

4. Configura esta URL en MercadoPago:
```
https://abc123.ngrok.io/api/payments/webhook
```

5. Actualiza el `application.properties` con la URL de ngrok:
```properties
mercadopago.webhook.url=https://abc123.ngrok.io/api/payments/webhook
```

#### Ventajas de ngrok
- ✅ Fácil de configurar
- ✅ HTTPS automático
- ✅ Panel web para ver requests
- ✅ Gratuito para desarrollo

### Opción 2: localtunnel

Similar a ngrok pero no requiere cuenta.

#### Instalación
```bash
npm install -g localtunnel
```

#### Uso
```bash
lt --port 8082 --subdomain payment-service
```

URL resultante: `https://payment-service.loca.lt`

### Opción 3: serveo.net

Túnel SSH, no requiere instalación.

```bash
ssh -R 80:localhost:8082 serveo.net
```

### Opción 4: Desplegar en un servidor con IP pública

Para testing más estable, despliega en:
- Railway
- Render
- Heroku
- AWS EC2
- DigitalOcean

## Configurar Webhook en MercadoPago

1. Ir a https://www.mercadopago.com.ar/developers
2. Seleccionar tu aplicación
3. Ir a "Webhooks"
4. Agregar URL:
   - Desarrollo: `https://tu-ngrok-url.ngrok.io/api/payments/webhook`
   - Producción: `https://tu-dominio.com/api/payments/webhook`
5. Seleccionar eventos:
   - ✅ Pagos
   - ✅ Merchant Orders (opcional)

## Probar el Webhook Localmente

### Con ngrok Web Interface

ngrok proporciona una interfaz web en `http://127.0.0.1:4040` donde puedes:
- Ver todos los requests recibidos
- Inspeccionar headers y body
- Repetir requests para debugging

### Simular Webhook Manualmente

```bash
curl -X POST http://localhost:8082/api/payments/webhook?adminId=1 \
  -H "Content-Type: application/json" \
  -d '{
    "action": "payment.updated",
    "api_version": "v1",
    "data": {
      "id": "123456789"
    },
    "date_created": "2024-10-25T10:00:00Z",
    "id": 123456789,
    "live_mode": false,
    "type": "payment",
    "user_id": 987654321
  }'
```

### Con PowerShell

```powershell
$webhookBody = @{
    action = "payment.updated"
    api_version = "v1"
    data = @{
        id = "123456789"
    }
    date_created = "2024-10-25T10:00:00Z"
    id = 123456789
    live_mode = $false
    type = "payment"
    user_id = 987654321
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/payments/webhook?adminId=1" `
    -Method Post `
    -ContentType "application/json" `
    -Body $webhookBody
```

## Verificar que el Webhook Funciona

### 1. Check logs de tu aplicación
Buscar líneas como:
```
POST /api/payments/webhook - Type: payment, Data: ...
Webhook procesado. Pago 123 actualizado a estado: APPROVED
```

### 2. Verificar en la BD
```sql
SELECT * FROM payments ORDER BY updated_at DESC LIMIT 5;
```

### 3. ngrok Inspector
Ir a `http://127.0.0.1:4040/inspect/http` y ver los requests.

## Troubleshooting

### Webhook no llega
- ✅ Verificar que ngrok esté corriendo
- ✅ Verificar que la URL en MercadoPago sea correcta
- ✅ Revisar logs de ngrok
- ✅ Verificar que el endpoint no requiera autenticación

### Error 401 Unauthorized
El webhook de MercadoPago debe estar en la whitelist de Spring Security.
Verificar `SecurityConfig.java`:
```java
.requestMatchers("/api/payments/webhook").permitAll()
```

### Error 500 en webhook
- Revisar logs de la aplicación
- Verificar que el adminId sea correcto
- Verificar que las credenciales estén en la BD

## Mejores Prácticas

1. **Usar ngrok con subdomain consistente**: `ngrok http 8082 --subdomain=payment-service`
2. **Validar firma de MercadoPago** (implementar en producción)
3. **Procesar webhooks de forma asíncrona**
4. **Implementar idempotencia** (no procesar el mismo webhook dos veces)
5. **Logging detallado** para debugging

## Script de Inicio Rápido

Crea un archivo `start-dev.sh` (Git Bash en Windows) o `start-dev.ps1` (PowerShell):

**start-dev.sh:**
```bash
#!/bin/bash
echo "Iniciando Payment Service..."
mvn spring-boot:run &
sleep 10
echo "Iniciando ngrok..."
ngrok http 8082
```

**start-dev.ps1:**
```powershell
Write-Host "Iniciando Payment Service..."
Start-Process -NoNewWindow mvn "spring-boot:run"
Start-Sleep -Seconds 10
Write-Host "Iniciando ngrok..."
ngrok http 8082
```

## Referencias

- [ngrok Documentation](https://ngrok.com/docs)
- [MercadoPago Webhooks](https://www.mercadopago.com.ar/developers/es/docs/your-integrations/notifications/webhooks)
- [Testing Webhooks Locally](https://requestbin.com/)
