# Payment Service - Security Configuration

## 🔐 Configuración de Credenciales

Este servicio requiere credenciales de MercadoPago para funcionar. **NUNCA** commitees credenciales reales a Git.

### Archivos Protegidos (en .gitignore)

- `.env` - Variables de entorno
- `database-init.sql` - Contiene credenciales en INSERT statements

### Configuración Inicial

1. **Copiar archivos de ejemplo:**
   ```bash
   cp .env.example .env
   cp database-init.sql.example database-init.sql
   ```

2. **Obtener credenciales de MercadoPago:**
   - Ir a: https://www.mercadopago.com.ar/developers/panel
   - Crear una aplicación
   - Copiar `Access Token` y `Public Key`

3. **Configurar database-init.sql:**
   ```sql
   INSERT INTO admin_credentials (
       admin_id, 
       access_token, 
       public_key, 
       is_active, 
       is_sandbox, 
       created_at
   ) VALUES (
       1,
       'TU_ACCESS_TOKEN_AQUI',  -- Pegar tu Access Token
       'TU_PUBLIC_KEY_AQUI',    -- Pegar tu Public Key
       true,
       true,  -- true para sandbox, false para producción
       NOW()
   );
   ```

4. **Levantar los servicios:**
   ```bash
   docker-compose up -d --build
   ```

### Modo Sandbox vs Producción

**Sandbox (Desarrollo):**
- `is_sandbox = true`
- Usar credenciales de TEST
- URLs de webhook pueden ser localhost

**Producción:**
- `is_sandbox = false`
- Usar credenciales de PRODUCCIÓN
- URLs de webhook deben ser HTTPS válidas
- Descomentar `autoReturn` en `PaymentService.java`

### Actualizar Credenciales en BD

Si necesitas actualizar las credenciales sin reiniciar Docker:

```bash
docker exec -it payment-service-db psql -U postgres -d payment_service_db -c "
UPDATE admin_credentials SET 
  access_token = 'NUEVO_ACCESS_TOKEN',
  public_key = 'NUEVO_PUBLIC_KEY',
  updated_at = NOW()
WHERE admin_id = 1;
"
```

## ⚠️ Advertencias de Seguridad

1. **Nunca commitees** archivos `.env` o `database-init.sql` con credenciales reales
2. **Rota las credenciales** si accidentalmente las commiteas
3. **Usa diferentes credenciales** para desarrollo y producción
4. **Configura webhooks HTTPS** en producción
5. **Revisa los logs** regularmente en busca de credenciales expuestas

## 📝 Variables de Entorno

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `MERCADOPAGO_ACCESS_TOKEN` | Token de acceso | `APP_USR-...` |
| `MERCADOPAGO_PUBLIC_KEY` | Clave pública | `APP_USR-...` |
| `WEBHOOK_URL` | URL del webhook | `https://tu-dominio.com/api/payments/webhook` |
| `SERVER_PORT` | Puerto del servicio | `8087` |

## 🔍 Verificar Configuración

```bash
# Ver credenciales configuradas (sin mostrar tokens completos)
docker exec -it payment-service-db psql -U postgres -d payment_service_db -c "
SELECT 
  admin_id, 
  LEFT(access_token, 20) || '...' as access_token,
  LEFT(public_key, 20) || '...' as public_key,
  is_sandbox, 
  is_active 
FROM admin_credentials;
"
```

## 📚 Documentación

- [MercadoPago Developers](https://www.mercadopago.com.ar/developers)
- [Testing Credentials](https://www.mercadopago.com.ar/developers/es/docs/your-integrations/test-credentials)
