# 🚀 GUÍA DE COMPILACIÓN Y DEPLOYMENT - ANALYTICS SERVICE

## 📋 PRE-REQUISITOS

Antes de compilar e iniciar el Analytics-Service, asegúrate de tener:

- ✅ **Java 17** o superior instalado
- ✅ **Maven 3.6+** (incluido en `mvnw`)
- ✅ **PostgreSQL 15** corriendo en localhost:5432
- ✅ **Otros microservicios** activos (Auth, Users, Event, Order, Payment)

---

## 🔧 PASO 1: CONFIGURACIÓN INICIAL

### **1.1 Verificar archivo `.env`**

El archivo `.env` ya está configurado en:
```
packedgo/back/analytics-service/.env
```

**Contenido:**
```bash
SERVER_PORT=8087
DATABASE_URL=jdbc:postgresql://localhost:5439/analytics_db
DATABASE_USER=analytics_user
DATABASE_PASSWORD=analytics_password
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
EVENT_SERVICE_URL=http://localhost:8086
ORDER_SERVICE_URL=http://localhost:8084
PAYMENT_SERVICE_URL=http://localhost:8085
```

⚠️ **IMPORTANTE**: El `JWT_SECRET` DEBE ser el mismo que en `auth-service/.env`

---

### **1.2 Crear Base de Datos**

```powershell
# Conectar a PostgreSQL
psql -U postgres -h localhost

# Ejecutar en psql:
CREATE DATABASE analytics_db;
CREATE USER analytics_user WITH PASSWORD 'analytics_password';
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO analytics_user;

# Verificar
\l analytics_db
\q
```

**Verificación:**
```powershell
psql -U analytics_user -h localhost -d analytics_db -c "SELECT version();"
```

---

## 🏗️ PASO 2: COMPILACIÓN

### **2.1 Limpiar y Compilar**

```powershell
cd packedgo\back\analytics-service

# Limpiar compilaciones anteriores
.\mvnw clean

# Compilar (sin tests)
.\mvnw clean install -DskipTests

# Compilar (con tests)
.\mvnw clean install
```

**Salida esperada:**
```
[INFO] BUILD SUCCESS
[INFO] Total time:  XX.XXX s
[INFO] Finished at: 2025-11-07T10:30:00-03:00
```

**Archivo generado:**
```
target/analytics-service-0.0.1-SNAPSHOT.jar
```

---

### **2.2 Verificar Dependencias**

```powershell
# Ver árbol de dependencias
.\mvnw dependency:tree

# Verificar que JWT esté incluido
.\mvnw dependency:tree | Select-String "jjwt"
```

**Salida esperada:**
```
[INFO] +- io.jsonwebtoken:jjwt-api:jar:0.12.6:compile
[INFO] +- io.jsonwebtoken:jjwt-impl:jar:0.12.6:runtime
[INFO] +- io.jsonwebtoken:jjwt-jackson:jar:0.12.6:runtime
```

---

## 🚀 PASO 3: EJECUCIÓN

### **Opción A: Maven (Desarrollo)**

```powershell
cd packedgo\back\analytics-service
.\mvnw spring-boot:run
```

**Logs esperados:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::               (v3.5.6)

2025-11-07 10:30:00.000  INFO 12345 --- [main] c.p.a.AnalyticsServiceApplication : Starting AnalyticsServiceApplication
2025-11-07 10:30:05.000  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8087 (http)
2025-11-07 10:30:05.000  INFO 12345 --- [main] c.p.a.AnalyticsServiceApplication : Started AnalyticsServiceApplication in 5.123 seconds
```

---

### **Opción B: JAR Ejecutable (Producción)**

```powershell
cd packedgo\back\analytics-service

# Compilar
.\mvnw clean package -DskipTests

# Ejecutar JAR
java -jar target/analytics-service-0.0.1-SNAPSHOT.jar
```

**Con variables de entorno custom:**
```powershell
$env:SERVER_PORT=8087
$env:DATABASE_URL="jdbc:postgresql://localhost:5439/analytics_db"
java -jar target/analytics-service-0.0.1-SNAPSHOT.jar
```

---

### **Opción C: Docker**

```powershell
cd packedgo\back\analytics-service

# Construir imagen
docker build -t packedgo/analytics-service:latest .

# Ejecutar contenedor
docker run -p 8087:8087 --name analytics-service `
  --env-file .env `
  packedgo/analytics-service:latest
```

---

### **Opción D: Docker Compose (RECOMENDADO)**

```powershell
cd packedgo\back

# Solo Analytics-Service
docker-compose up analytics-service --build

# Todos los servicios
docker-compose up --build
```

---

## 🔍 PASO 4: VERIFICACIÓN

### **4.1 Health Check**

```powershell
# Usando curl
curl http://localhost:8087/api/dashboard/health

# Usando PowerShell
Invoke-RestMethod -Uri "http://localhost:8087/api/dashboard/health"
```

**Respuesta esperada:**
```
Analytics Service is UP
```

---

### **4.2 Test con Token JWT**

**Paso 1: Obtener Token (login)**
```powershell
$loginBody = @{
    email = "admin@example.com"
    password = "admin123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
    -Uri "http://localhost:8081/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body $loginBody

$token = $loginResponse.access_token
Write-Host "Token obtenido: $token"
```

**Paso 2: Obtener Dashboard**
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

$dashboard = Invoke-RestMethod `
    -Uri "http://localhost:8087/api/dashboard" `
    -Method GET `
    -Headers $headers

# Mostrar resultado
$dashboard | ConvertTo-Json -Depth 5
```

**Respuesta esperada:**
```json
{
  "organizerId": 1,
  "organizerName": "Organizador 1",
  "lastUpdated": "2025-11-07T10:30:00",
  "salesMetrics": {
    "totalTicketsSold": 150,
    ...
  },
  "eventMetrics": { ... },
  "revenueMetrics": { ... },
  "topPerformers": { ... },
  "trends": { ... }
}
```

---

## 🐛 TROUBLESHOOTING

### **Error: "Cannot resolve symbol 'Jwts'"**

**Causa:** Dependencias JWT no descargadas

**Solución:**
```powershell
.\mvnw clean install -U
```

---

### **Error: "Connection refused - localhost:5439"**

**Causa:** Base de datos no creada o PostgreSQL no corriendo

**Solución:**
```powershell
# Verificar PostgreSQL
Get-Service postgresql*

# Crear base de datos
psql -U postgres -h localhost
CREATE DATABASE analytics_db;
```

---

### **Error: "JWT signature does not match"**

**Causa:** JWT_SECRET diferente entre auth-service y analytics-service

**Solución:**
1. Verificar `.env` de ambos servicios
2. Copiar `JWT_SECRET` de auth-service a analytics-service
3. Reiniciar ambos servicios

---

### **Error: "403 Forbidden" al acceder a /api/dashboard**

**Causa:** Token JWT no es de usuario ADMIN

**Solución:**
1. Verificar rol del usuario en la base de datos `auth_db`
2. Hacer login con usuario que tenga rol `ADMIN` o `SUPER_ADMIN`
3. Verificar que el token incluya el claim `role`

---

### **Dashboard vacío (todas las métricas en 0)**

**Causa:** No hay datos en las bases de datos

**Solución:**
1. Verificar que otros servicios estén activos
2. Crear eventos en Event-Service
3. Hacer órdenes en Order-Service
4. Procesar pagos en Payment-Service
5. Refrescar dashboard

---

### **Error: "Connection refused" a Event/Order/Payment Service**

**Causa:** Servicios no están activos o URLs incorrectas

**Solución:**
```powershell
# Verificar servicios activos
curl http://localhost:8086/api/health  # Event
curl http://localhost:8084/api/health  # Order
curl http://localhost:8085/api/health  # Payment

# Si no responden, iniciarlos
cd packedgo\back\event-service
.\mvnw spring-boot:run
```

---

## 📊 PASO 5: MONITOREO

### **Logs en tiempo real**

```powershell
# Ver logs de Spring Boot
Get-Content logs/spring.log -Wait

# En Docker
docker logs -f analytics-service
```

### **Métricas de JVM (Actuator)**

Si habilitaste Spring Boot Actuator:

```powershell
# Health endpoint
curl http://localhost:8087/actuator/health

# Metrics endpoint
curl http://localhost:8087/actuator/metrics
```

---

## 🔐 PASO 6: SEGURIDAD (Producción)

### **6.1 Cambiar JWT_SECRET**

⚠️ **NUNCA usar el JWT_SECRET por defecto en producción**

```powershell
# Generar nuevo secret (256 bits base64)
$bytes = New-Object byte[] 32
[Security.Cryptography.RNGCryptoServiceProvider]::Create().GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes)
Write-Host "Nuevo JWT_SECRET: $secret"
```

**Actualizar en `.env`:**
```bash
JWT_SECRET=TU_NUEVO_SECRET_AQUI
```

### **6.2 Cambiar contraseñas de base de datos**

```bash
DATABASE_PASSWORD=NUEVA_CONTRASEÑA_SEGURA
```

### **6.3 Configurar HTTPS**

En producción, usar HTTPS:

```properties
# application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

---

## 📦 PASO 7: DEPLOYMENT EN SERVIDOR

### **7.1 Crear imagen Docker optimizada**

```dockerfile
# Multi-stage build
FROM maven:3.9-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/analytics-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### **7.2 Deploy en servidor Linux**

```bash
# Transferir JAR al servidor
scp target/analytics-service-0.0.1-SNAPSHOT.jar user@server:/opt/packedgo/

# En el servidor
cd /opt/packedgo
java -jar analytics-service-0.0.1-SNAPSHOT.jar &
```

### **7.3 Crear servicio systemd**

```bash
# /etc/systemd/system/analytics-service.service
[Unit]
Description=PackedGo Analytics Service
After=postgresql.service

[Service]
User=packedgo
WorkingDirectory=/opt/packedgo
ExecStart=/usr/bin/java -jar /opt/packedgo/analytics-service-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
TimeoutStopSec=10
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

**Iniciar servicio:**
```bash
sudo systemctl daemon-reload
sudo systemctl start analytics-service
sudo systemctl enable analytics-service
sudo systemctl status analytics-service
```

---

## 🎯 CHECKLIST FINAL

Antes de considerar el deployment completo:

- [ ] ✅ Compilación exitosa (`mvn clean install`)
- [ ] ✅ Base de datos creada y accesible
- [ ] ✅ Variables de entorno configuradas
- [ ] ✅ JWT_SECRET coincide con auth-service
- [ ] ✅ Otros servicios activos (Event, Order, Payment)
- [ ] ✅ Health check responde correctamente
- [ ] ✅ Dashboard retorna datos con token válido
- [ ] ✅ Logs no muestran errores
- [ ] ✅ CORS configurado para el frontend
- [ ] ✅ Contraseñas cambiadas (producción)
- [ ] ✅ HTTPS configurado (producción)

---

## 📚 RECURSOS ADICIONALES

- **Documentación Spring Boot**: https://spring.io/projects/spring-boot
- **JWT en Spring**: https://jwt.io/
- **PostgreSQL Docker**: https://hub.docker.com/_/postgres
- **Maven Wrapper**: https://maven.apache.org/wrapper/

---

## 💡 TIPS

1. **Desarrollo**: Usar `mvn spring-boot:run` para hot-reload
2. **Producción**: Usar JAR compilado o Docker
3. **Logs**: Configurar niveles según ambiente (DEBUG en dev, INFO en prod)
4. **Monitoreo**: Habilitar Spring Boot Actuator para métricas
5. **Backup**: Respaldar base de datos regularmente

---

## 🎉 ¡LISTO!

El Analytics-Service está completamente desplegado y funcional.

**Acceso:**
- **Backend**: http://localhost:8087/api/dashboard
- **Health**: http://localhost:8087/api/dashboard/health
- **Frontend**: http://localhost:4200/admin/analytics (después de implementar)

---

**Autores:**
- David Elías Delfino (Legajo: 111858)
- Agustín Luparia Mothe (Legajo: 113973)

**UTN FRC - Tecnicatura Universitaria en Programación - 2025**
