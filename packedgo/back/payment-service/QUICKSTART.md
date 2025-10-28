# 🚀 Guía de Inicio Rápido - Payment Service

## Prerequisitos

- ✅ Java 17 o superior
- ✅ Maven 3.6+
- ✅ PostgreSQL 12+
- ✅ Cuenta de MercadoPago (Sandbox)

## Pasos de Instalación

### 1. Configurar Base de Datos

```sql
-- Crear base de datos
CREATE DATABASE payment_service_db;
```

### 2. Configurar Variables de Entorno (Opcional)

**Windows PowerShell:**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/payment_service_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="tu_password"
```

**Linux/Mac:**
```bash
export DB_URL="jdbc:postgresql://localhost:5432/payment_service_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="tu_password"
```

### 3. Compilar el Proyecto

```bash
cd payment-service
mvn clean install
```

### 4. Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

O con Java directamente:
```bash
java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

La aplicación iniciará en: `http://localhost:8082`

### 5. Verificar que Funciona

```bash
curl http://localhost:8082/api/payments/health
```

Debería retornar: `{"status":"UP"}`

### 6. Configurar Credenciales de MercadoPago

Obtén tus credenciales de sandbox en:
https://www.mercadopago.com.ar/developers/panel/credentials

Luego ejecuta en PostgreSQL:

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
    'TU_ACCESS_TOKEN_DE_SANDBOX',
    'TU_PUBLIC_KEY_DE_SANDBOX',
    true,
    true,
    NOW()
);
```

### 7. Probar Crear un Pago

**PowerShell:**
```powershell
$body = @{
    adminId = 1
    orderId = "ORDER-TEST-001"
    amount = 1500.00
    description = "Paquete Premium"
    payerEmail = "test@email.com"
    payerName = "Juan Pérez"
    externalReference = "REF-001"
    successUrl = "http://localhost:3000/payment/success"
    failureUrl = "http://localhost:3000/payment/failure"
    pendingUrl = "http://localhost:3000/payment/pending"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/api/payments/create" `
    -Method Post `
    -ContentType "application/json" `
    -Body $body
```

**cURL:**
```bash
curl -X POST http://localhost:8082/api/payments/create \
  -H "Content-Type: application/json" \
  -d '{
    "adminId": 1,
    "orderId": "ORDER-TEST-001",
    "amount": 1500.00,
    "description": "Paquete Premium",
    "payerEmail": "test@email.com",
    "payerName": "Juan Pérez",
    "externalReference": "REF-001",
    "successUrl": "http://localhost:3000/payment/success",
    "failureUrl": "http://localhost:3000/payment/failure",
    "pendingUrl": "http://localhost:3000/payment/pending"
  }'
```

## 🐳 Inicio Rápido con Docker

### Usando Docker Compose

```bash
# Iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f payment-service

# Detener servicios
docker-compose down
```

### Solo Base de Datos con Docker

```bash
# Iniciar PostgreSQL
docker run --name payment-db \
  -e POSTGRES_DB=payment_service_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgres:15-alpine

# Luego ejecutar la app normalmente
mvn spring-boot:run
```

## 📝 Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/payments/create` | Crear preferencia de pago |
| POST | `/api/payments/webhook` | Webhook de MercadoPago |
| GET | `/api/payments/order/{orderId}` | Consultar pago |
| GET | `/api/payments/health` | Health check |
| GET | `/actuator/health` | Actuator health |

## 🔧 Configuración Adicional

### Cambiar Puerto

Editar `application.properties`:
```properties
server.port=8083
```

O con variable de entorno:
```bash
SERVER_PORT=8083 mvn spring-boot:run
```

### Habilitar Logs de SQL

En `application.properties`:
```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### Configurar CORS

En `application.properties`:
```properties
cors.allowed-origins=http://localhost:3000,http://localhost:4200,http://localhost:8080
```

## 🧪 Ejecutar Tests

```bash
# Todos los tests
mvn test

# Solo un test específico
mvn test -Dtest=AdminCredentialServiceTest

# Con reporte de cobertura
mvn test jacoco:report
```

## 📊 Monitoreo

### Actuator Endpoints

- Health: http://localhost:8082/actuator/health
- Info: http://localhost:8082/actuator/info
- Metrics: http://localhost:8082/actuator/metrics

### Ver Métricas Específicas

```bash
# Memory usage
curl http://localhost:8082/actuator/metrics/jvm.memory.used

# HTTP requests
curl http://localhost:8082/actuator/metrics/http.server.requests
```

## 🐛 Troubleshooting

### Error: "Could not connect to database"

1. Verificar que PostgreSQL esté corriendo:
```bash
# Windows
Get-Service postgresql*

# Linux
sudo systemctl status postgresql
```

2. Verificar credenciales en `application.properties`

### Error: "Port 8082 already in use"

```bash
# Windows - Encontrar proceso
netstat -ano | findstr :8082

# Matar proceso
taskkill /PID <PID> /F

# Linux
lsof -ti:8082 | xargs kill -9
```

### Error: "Credenciales no encontradas"

Verificar que las credenciales estén insertadas:
```sql
SELECT * FROM admin_credentials WHERE admin_id = 1;
```

### La aplicación no inicia

```bash
# Limpiar y recompilar
mvn clean install -U

# Verificar versión de Java
java -version  # Debe ser 17+

# Ver logs detallados
mvn spring-boot:run -X
```

## 📚 Documentación Adicional

- [README.md](./README.md) - Documentación completa
- [API-TESTING-GUIDE.md](./API-TESTING-GUIDE.md) - Guía de pruebas
- [WEBHOOK-SETUP.md](./WEBHOOK-SETUP.md) - Configuración de webhooks

## 🆘 Ayuda

Si tienes problemas:

1. Revisa los logs: `logs/spring.log`
2. Verifica la conexión a la BD
3. Asegúrate de que las credenciales de MercadoPago sean de Sandbox
4. Revisa que todos los endpoints de seguridad estén correctos

## ✅ Checklist de Verificación

- [ ] PostgreSQL corriendo
- [ ] Base de datos creada
- [ ] Aplicación compila sin errores
- [ ] Aplicación inicia correctamente
- [ ] Health check responde OK
- [ ] Credenciales de admin insertadas en BD
- [ ] Se puede crear una preferencia de pago
- [ ] Logs muestran información correcta

## 🎉 ¡Listo!

Tu Payment Service está configurado y listo para usar. Consulta la documentación completa para más detalles sobre la arquitectura y características avanzadas.
