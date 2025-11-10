# 📊 Analytics Service - Quick Start

> **PackedGo Analytics Service** - Dashboard de métricas y estadísticas para organizadores de eventos.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19-red)](https://angular.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)

---

## 🚀 Inicio Rápido (5 minutos)

### **1. Crear Base de Datos**

```powershell
psql -U postgres -h localhost
```

```sql
CREATE DATABASE analytics_db;
CREATE USER analytics_user WITH PASSWORD 'analytics_password';
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO analytics_user;
\q
```

### **2. Compilar y Ejecutar**

```powershell
# Navegar al servicio
cd packedgo\back\analytics-service

# Compilar
.\mvnw clean install -DskipTests

# Ejecutar
.\mvnw spring-boot:run
```

### **3. Verificar**

```powershell
curl http://localhost:8087/api/dashboard/health
```

✅ Respuesta esperada: `Analytics Service is UP`

---

## 📁 Archivos Importantes

| Archivo | Descripción |
|---------|-------------|
| `INDICE_ANALYTICS_IMPLEMENTATION.md` | 📚 Índice completo de toda la implementación |
| `ANALYTICS_SERVICE_GUIDE.md` | 📖 Guía completa del backend |
| `FRONTEND_DASHBOARD_GUIDE.md` | 🎨 Guía de implementación Angular |
| `DEPLOYMENT_ANALYTICS_GUIDE.md` | 🚀 Guía de deployment y configuración |
| `ARQUITECTURA_ANALYTICS_VISUAL.md` | 🏗️ Diagramas de arquitectura |
| `RESUMEN_ANALYTICS_SERVICE.md` | 📋 Resumen ejecutivo |
| `deploy-analytics.ps1` | ⚙️ Script de deployment automatizado |
| `test-analytics.ps1` | 🧪 Script de testing automatizado |

---

## 🎯 ¿Qué hace este servicio?

El Analytics Service proporciona un **dashboard completo** para organizadores de eventos con:

### **Métricas de Ventas**
- Total de tickets vendidos
- Total de órdenes
- Promedio de tickets por orden
- Tasa de conversión

### **Métricas de Eventos**
- Total de eventos creados
- Eventos activos vs pasados
- Tasa de ocupación promedio
- Capacidad total

### **Métricas de Ingresos**
- Revenue total
- Revenue por tickets
- Revenue por consumibles
- Tasa de crecimiento

### **Top Performers**
- Top 5 eventos más vendidos
- Top 5 consumibles más populares
- Top categorías por revenue

### **Tendencias**
- Tendencias diarias (últimos 30 días)
- Tendencias mensuales (últimos 12 meses)

---

## 🔧 Configuración Rápida

### **Variables de Entorno (.env)**

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

⚠️ **IMPORTANTE:** El `JWT_SECRET` debe coincidir con `auth-service/.env`

---

## 📡 Endpoints API

### **Health Check (Público)**

```http
GET /api/dashboard/health
```

### **Dashboard del Usuario Autenticado**

```http
GET /api/dashboard
Authorization: Bearer {JWT_TOKEN}
```

### **Dashboard de Organizador Específico (SUPER_ADMIN)**

```http
GET /api/dashboard/{organizerId}
Authorization: Bearer {JWT_TOKEN}
```

---

## 🐳 Docker Quick Start

### **Opción 1: Docker Compose (Recomendado)**

```powershell
cd packedgo\back
docker-compose up analytics-service --build
```

### **Opción 2: Docker Manual**

```powershell
cd packedgo\back\analytics-service

# Build
docker build -t packedgo/analytics-service:latest .

# Run
docker run -p 8087:8087 --env-file .env packedgo/analytics-service:latest
```

---

## ⚙️ Scripts de Automatización

### **Deployment Automatizado**

```powershell
# Modo desarrollo
.\deploy-analytics.ps1 -Mode dev

# Modo Docker (con creación de DB)
.\deploy-analytics.ps1 -Mode docker -CreateDb

# Modo producción (JAR)
.\deploy-analytics.ps1 -Mode prod -SkipTests
```

### **Testing Automatizado (10 tests)**

```powershell
# Ejecutar todos los tests
.\test-analytics.ps1

# Solo health check
.\test-analytics.ps1 -TestType health

# Solo métricas
.\test-analytics.ps1 -TestType metrics

# Test de performance
.\test-analytics.ps1 -TestType performance
```

### **Iniciar Sistema Completo**

```powershell
.\iniciar-sistema-completo.ps1
```

Este script inicia todos los servicios en orden y verifica su salud.

---

## 🧪 Testing Manual

### **1. Obtener Token JWT**

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
```

### **2. Obtener Dashboard**

```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

$dashboard = Invoke-RestMethod `
    -Uri "http://localhost:8087/api/dashboard" `
    -Method GET `
    -Headers $headers

$dashboard | ConvertTo-Json -Depth 5
```

---

## 🎨 Frontend (Angular) - Pendiente

Para implementar el frontend, seguir la guía completa en:

📖 **`FRONTEND_DASHBOARD_GUIDE.md`**

### **Instalación de dependencias**

```bash
npm install chart.js ng2-charts --save
```

### **Archivos a crear**

1. `src/app/core/services/analytics.service.ts`
2. `src/app/features/admin/dashboard-analytics.component.ts`
3. `src/app/features/admin/dashboard-analytics.component.html`
4. `src/app/features/admin/dashboard-analytics.component.css`

---

## 🐛 Troubleshooting Rápido

### **Error: "Connection refused - localhost:5439"**

➡️ Crear base de datos: `.\deploy-analytics.ps1 -Mode dev -CreateDb`

### **Error: "JWT signature does not match"**

➡️ Copiar `JWT_SECRET` de `auth-service/.env` a `analytics-service/.env`

### **Dashboard vacío (todas las métricas en 0)**

➡️ Crear datos de prueba:
1. Crear eventos en Event-Service
2. Hacer órdenes en Order-Service
3. Procesar pagos en Payment-Service

### **Error: "403 Forbidden"**

➡️ Verificar que el usuario tenga rol `ADMIN` o `SUPER_ADMIN`

---

## 📊 Estructura del Proyecto

```
analytics-service/
├── src/main/java/com/packedgo/analytics/
│   ├── controller/
│   │   └── DashboardController.java
│   ├── service/
│   │   └── AnalyticsService.java
│   ├── security/
│   │   ├── JwtTokenValidator.java
│   │   └── SecurityConfig.java
│   ├── dto/
│   │   ├── DashboardDTO.java
│   │   ├── SalesMetricsDTO.java
│   │   ├── EventMetricsDTO.java
│   │   ├── RevenueMetricsDTO.java
│   │   ├── TopPerformersDTO.java
│   │   └── TrendsDTO.java
│   └── model/
│       ├── Event.java
│       ├── Order.java
│       ├── Payment.java
│       └── Consumption.java
├── pom.xml
├── .env
├── Dockerfile
└── application.properties
```

---

## 📚 Documentación Completa

Para información más detallada, consultar:

| Documento | Descripción |
|-----------|-------------|
| [INDICE_ANALYTICS_IMPLEMENTATION.md](INDICE_ANALYTICS_IMPLEMENTATION.md) | Índice maestro de toda la implementación |
| [ANALYTICS_SERVICE_GUIDE.md](ANALYTICS_SERVICE_GUIDE.md) | Guía completa del backend |
| [FRONTEND_DASHBOARD_GUIDE.md](FRONTEND_DASHBOARD_GUIDE.md) | Guía de implementación Angular |
| [DEPLOYMENT_ANALYTICS_GUIDE.md](DEPLOYMENT_ANALYTICS_GUIDE.md) | Guía de deployment |
| [ARQUITECTURA_ANALYTICS_VISUAL.md](ARQUITECTURA_ANALYTICS_VISUAL.md) | Diagramas visuales |

---

## 🔗 Servicios Dependientes

| Servicio | Puerto | Propósito |
|----------|--------|-----------|
| Auth Service | 8081 | Autenticación JWT |
| Users Service | 8082 | Información de usuarios |
| Event Service | 8086 | Eventos y consumibles |
| Order Service | 8084 | Órdenes de compra |
| Payment Service | 8085 | Pagos procesados |

**Todos estos servicios DEBEN estar activos para que Analytics funcione correctamente.**

---

## ✅ Checklist de Implementación

### **Backend**
- [x] Estructura Maven creada
- [x] Dependencias configuradas
- [x] 13 DTOs implementados
- [x] AnalyticsService completo
- [x] DashboardController con endpoints
- [x] Seguridad JWT configurada
- [x] Dockerfile creado
- [x] docker-compose.yml actualizado

### **Frontend (Pendiente)**
- [ ] Instalar chart.js y ng2-charts
- [ ] Crear AnalyticsService TypeScript
- [ ] Crear DashboardAnalyticsComponent
- [ ] Implementar gráficos Chart.js
- [ ] Configurar routing

### **Testing**
- [x] Suite de 10 tests automatizados
- [ ] Tests de integración con datos reales

### **Deployment**
- [x] Script de deployment automatizado
- [x] Guía de deployment completa
- [ ] Deploy en servidor de producción

---

## 👥 Autores

**David Elías Delfino** (Legajo: 111858)  
**Agustín Luparia Mothe** (Legajo: 113973)

**Institución:** Universidad Tecnológica Nacional - Facultad Regional Córdoba  
**Carrera:** Tecnicatura Universitaria en Programación  
**Proyecto:** PackedGo SaaS Multi-Tenant Platform  
**Fecha:** Noviembre 2025

---

## 📝 Licencia

Este proyecto es parte de un Trabajo Final de Tecnicatura para la UTN FRC.  
Todos los derechos reservados © 2025

---

## 🎉 ¡Listo para usar!

El Analytics Service está completamente implementado en el backend y listo para deployment.

**Siguiente paso:** Implementar el frontend siguiendo `FRONTEND_DASHBOARD_GUIDE.md`

---

**¿Necesitas ayuda?** Consulta la documentación completa en los archivos listados arriba. 📚
