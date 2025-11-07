# 📊 ANALYTICS SERVICE - PACKEDGO

## 🎯 Descripción

El **Analytics-Service** es un microservicio especializado en la generación de métricas y estadísticas agregadas para productoras de eventos. Consume datos de otros microservicios (Event, Order, Payment) y calcula:

- 📈 **Métricas de ventas**: tickets vendidos, órdenes, tasas de conversión
- 🎪 **Métricas de eventos**: capacidad, ocupación, eventos más populares
- 🍔 **Métricas de consumiciones**: ventas, canjes, consumiciones más vendidas
- 💰 **Métricas de ingresos**: revenue total, por periodo, por evento
- 🏆 **Top performers**: eventos y consumiciones más exitosos
- 📉 **Tendencias**: gráficos diarios y mensuales de ventas e ingresos

---

## 🏗️ Arquitectura

### **Puerto**: 8087
### **Base de Datos**: PostgreSQL (Puerto 5439) - `analytics_db`
### **Dependencias**:
- **Event-Service** (Puerto 8086): Obtiene eventos y consumiciones
- **Order-Service** (Puerto 8084): Obtiene órdenes y ventas
- **Payment-Service** (Puerto 8085): Obtiene información de pagos

---

## 🚀 Inicio Rápido

### **1. Configurar variables de entorno**

El archivo `.env` ya está configurado en el repositorio:

```bash
SERVER_PORT=8087
DATABASE_URL=jdbc:postgresql://localhost:5439/analytics_db
DATABASE_USER=analytics_user
DATABASE_PASSWORD=analytics_password
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
AUTH_SERVICE_URL=http://localhost:8081
USERS_SERVICE_URL=http://localhost:8082
EVENT_SERVICE_URL=http://localhost:8086
ORDER_SERVICE_URL=http://localhost:8084
PAYMENT_SERVICE_URL=http://localhost:8085
```

### **2. Crear base de datos (PostgreSQL local)**

```bash
# Conectar a PostgreSQL
psql -U postgres -h localhost

# Crear base de datos
CREATE DATABASE analytics_db;
CREATE USER analytics_user WITH PASSWORD 'analytics_password';
GRANT ALL PRIVILEGES ON DATABASE analytics_db TO analytics_user;
\q
```

### **3. Compilar y ejecutar**

**Opción A: Desarrollo local (Maven)**
```powershell
cd packedgo\back\analytics-service
.\mvnw clean install
.\mvnw spring-boot:run
```

**Opción B: Docker**
```powershell
cd packedgo\back
docker-compose up analytics-service analytics-db --build
```

### **4. Verificar que está corriendo**

```powershell
# Health check
curl http://localhost:8087/api/dashboard/health

# Respuesta esperada: "Analytics Service is UP"
```

---

## 📡 Endpoints

### **1. GET /api/dashboard**
Obtiene el dashboard completo del organizador autenticado (extrae `userId` del JWT).

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Respuesta (200 OK):**
```json
{
  "organizerId": 1,
  "organizerName": "Organizador 1",
  "lastUpdated": "2025-11-07T10:30:00",
  "salesMetrics": {
    "totalTicketsSold": 150,
    "ticketsSoldToday": 12,
    "ticketsSoldThisWeek": 45,
    "ticketsSoldThisMonth": 120,
    "totalOrders": 100,
    "paidOrders": 85,
    "pendingOrders": 10,
    "cancelledOrders": 5,
    "conversionRate": 85.0,
    "averageOrderValue": 2500.50
  },
  "eventMetrics": {
    "totalEvents": 10,
    "activeEvents": 7,
    "completedEvents": 2,
    "cancelledEvents": 1,
    "upcomingEvents": 5,
    "totalCapacity": 5000,
    "occupiedCapacity": 3200,
    "averageOccupancyRate": 64.0,
    "mostSoldEventId": 5,
    "mostSoldEventName": "Fiesta de Año Nuevo",
    "mostSoldEventTickets": 800
  },
  "consumptionMetrics": {
    "totalConsumptions": 25,
    "activeConsumptions": 20,
    "totalConsumptionsSold": 450,
    "consumptionsRedeemed": 225,
    "consumptionsPending": 225,
    "redemptionRate": 50.0,
    "mostSoldConsumptionId": 3,
    "mostSoldConsumptionName": "Cerveza Artesanal",
    "mostSoldConsumptionQuantity": 120
  },
  "revenueMetrics": {
    "totalRevenue": 212542.75,
    "revenueToday": 15000.00,
    "revenueThisWeek": 68000.50,
    "revenueThisMonth": 180000.00,
    "revenueFromTickets": 148780.00,
    "revenueFromConsumptions": 63762.75,
    "averageRevenuePerEvent": 21254.28,
    "averageRevenuePerCustomer": 2500.50
  },
  "topPerformers": {
    "topEvents": [
      {
        "eventId": 5,
        "eventName": "Fiesta de Año Nuevo",
        "ticketsSold": 800,
        "revenue": 120000.00,
        "occupancyRate": 80.0
      },
      {
        "eventId": 3,
        "eventName": "Concierto de Rock",
        "ticketsSold": 650,
        "revenue": 97500.00,
        "occupancyRate": 75.5
      }
    ],
    "topConsumptions": [
      {
        "consumptionId": 3,
        "consumptionName": "Cerveza Artesanal",
        "quantitySold": 120,
        "revenue": 18000.00,
        "redemptionRate": 65.0
      }
    ],
    "topEventCategories": [],
    "topConsumptionCategories": []
  },
  "trends": {
    "dailySales": [
      {
        "date": "2025-11-01",
        "count": 10,
        "amount": 15000.00
      },
      {
        "date": "2025-11-02",
        "count": 8,
        "amount": 12000.00
      }
    ],
    "dailyRevenue": [...],
    "monthlySales": [
      {
        "year": 2025,
        "month": 11,
        "monthName": "Nov",
        "count": 85,
        "amount": 180000.00
      }
    ],
    "monthlyRevenue": [...]
  }
}
```

**Errores:**
- `403 Forbidden`: Usuario no es ADMIN
- `500 Internal Server Error`: Error al generar el dashboard

---

### **2. GET /api/dashboard/{organizerId}**
Obtiene el dashboard de un organizador específico (solo SUPER_ADMIN o el mismo organizador).

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Params:**
- `organizerId`: ID del organizador

**Respuesta**: Mismo formato que endpoint anterior

---

### **3. GET /api/dashboard/health**
Health check endpoint para verificar que el servicio está activo.

**Respuesta (200 OK):**
```
Analytics Service is UP
```

---

## 🔐 Seguridad Multi-Tenant

El Analytics-Service respeta el modelo **SaaS Multi-Tenant**:

1. **Validación de JWT**: Extrae `userId` y `role` del token
2. **Autorización**:
   - Solo usuarios con rol `ADMIN` o `SUPER_ADMIN` pueden acceder al dashboard
   - Un ADMIN solo puede ver su propio dashboard
   - Un SUPER_ADMIN puede ver el dashboard de cualquier organizador
3. **Aislamiento de datos**: Consulta solo eventos/órdenes del organizador autenticado

---

## 📊 Métricas Disponibles

### **Sales Metrics (Ventas)**
| Métrica | Descripción |
|---------|-------------|
| `totalTicketsSold` | Total de tickets vendidos (histórico) |
| `ticketsSoldToday` | Tickets vendidos hoy |
| `ticketsSoldThisWeek` | Tickets vendidos últimos 7 días |
| `ticketsSoldThisMonth` | Tickets vendidos este mes |
| `totalOrders` | Total de órdenes creadas |
| `paidOrders` | Órdenes pagadas (status: PAID) |
| `pendingOrders` | Órdenes pendientes de pago |
| `cancelledOrders` | Órdenes canceladas |
| `conversionRate` | % de órdenes que culminan en pago |
| `averageOrderValue` | Valor promedio de una orden |

### **Event Metrics (Eventos)**
| Métrica | Descripción |
|---------|-------------|
| `totalEvents` | Total de eventos creados |
| `activeEvents` | Eventos activos (status: ACTIVE) |
| `completedEvents` | Eventos finalizados |
| `cancelledEvents` | Eventos cancelados |
| `upcomingEvents` | Eventos futuros (eventDate > hoy) |
| `totalCapacity` | Capacidad total de todos los eventos |
| `occupiedCapacity` | Total de passes vendidos |
| `averageOccupancyRate` | % promedio de ocupación |
| `mostSoldEventName` | Evento más vendido |

### **Consumption Metrics (Consumiciones)**
| Métrica | Descripción |
|---------|-------------|
| `totalConsumptions` | Total de consumiciones creadas |
| `activeConsumptions` | Consumiciones activas |
| `totalConsumptionsSold` | Consumiciones vendidas (en tickets) |
| `consumptionsRedeemed` | Consumiciones canjeadas (simulado) |
| `consumptionsPending` | Consumiciones pendientes de canje |
| `redemptionRate` | % de consumiciones canjeadas |
| `mostSoldConsumptionName` | Consumición más vendida |

### **Revenue Metrics (Ingresos)**
| Métrica | Descripción |
|---------|-------------|
| `totalRevenue` | Ingresos totales (histórico) |
| `revenueToday` | Ingresos de hoy |
| `revenueThisWeek` | Ingresos últimos 7 días |
| `revenueThisMonth` | Ingresos este mes |
| `revenueFromTickets` | Ingresos por venta de entradas (70%) |
| `revenueFromConsumptions` | Ingresos por consumiciones (30%) |
| `averageRevenuePerEvent` | Ingreso promedio por evento |
| `averageRevenuePerCustomer` | Ingreso promedio por cliente |

### **Top Performers**
- Top 5 eventos más vendidos
- Top 5 consumiciones más vendidas
- Top 5 categorías de eventos
- Top 5 categorías de consumiciones

### **Trends (Tendencias)**
- Ventas diarias (últimos 30 días)
- Ingresos diarios (últimos 30 días)
- Ventas mensuales (último año)
- Ingresos mensuales (último año)

---

## 🧪 Testing

### **Test 1: Health Check**
```powershell
curl http://localhost:8087/api/dashboard/health
```

### **Test 2: Dashboard (con token JWT)**
```powershell
# 1. Hacer login en auth-service para obtener token
$loginResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"email":"admin@example.com","password":"admin123"}'

$token = $loginResponse.access_token

# 2. Obtener dashboard
$headers = @{ "Authorization" = "Bearer $token" }
$dashboard = Invoke-RestMethod -Uri "http://localhost:8087/api/dashboard" `
    -Method GET `
    -Headers $headers

$dashboard | ConvertTo-Json -Depth 10
```

---

## 🛠️ Dependencias Externas

El Analytics-Service consulta los siguientes endpoints:

### **Event-Service**
- `GET /api/events/my-events` - Obtiene eventos del organizador
- `GET /api/consumptions/my-consumptions` - Obtiene consumiciones del organizador

### **Order-Service**
- `GET /api/orders/organizer/{organizerId}` - Obtiene órdenes del organizador

### **Payment-Service**
- `GET /api/payments/organizer/{organizerId}` - Obtiene pagos del organizador

**⚠️ IMPORTANTE**: Para que Analytics-Service funcione correctamente, **TODOS los servicios deben estar activos**.

---

## 🔧 Troubleshooting

### **Problema: "Analytics Service is DOWN"**
**Solución**: Verificar que PostgreSQL esté corriendo y la base de datos `analytics_db` exista.

### **Problema: "403 Forbidden" al acceder a /api/dashboard**
**Solución**: Verificar que el usuario tenga rol `ADMIN` o `SUPER_ADMIN` en el JWT.

### **Problema: Dashboard vacío o con métricas en 0**
**Solución**: Verificar que:
1. Otros servicios (Event, Order, Payment) estén activos
2. Existan eventos, órdenes y pagos en la base de datos
3. Los endpoints de los otros servicios sean accesibles

### **Problema: Error "cannot find symbol Stream"**
**Solución**: El import de `java.util.stream.Stream` está agregado. Si persiste, hacer `mvn clean install`.

---

## 📚 Próximos Pasos

1. **Frontend Angular**: Crear componente de dashboard con gráficos (Chart.js)
2. **Caché**: Implementar Redis para cachear métricas (evitar consultas constantes)
3. **Filtros**: Permitir filtrar por fecha, evento específico, categoría
4. **Exportar reportes**: PDF, Excel, CSV
5. **Alertas**: Notificar cuando ventas caen por debajo de umbral
6. **Comparativas**: Comparar periodos (este mes vs mes anterior)

---

## 👨‍💻 Autores

- **David Elías Delfino** (Legajo: 111858)
- **Agustín Luparia Mothe** (Legajo: 113973)

**Universidad Tecnológica Nacional - Facultad Regional Córdoba**
**Tecnicatura Universitaria en Programación**
**Año**: 2025
