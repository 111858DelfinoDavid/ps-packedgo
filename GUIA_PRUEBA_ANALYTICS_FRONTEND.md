# 🎯 Guía Completa de Prueba - Dashboard de Analytics

## ✅ Estado Actual del Sistema

### Backend
- ✅ **analytics-service**: Running en puerto 8087
- ✅ **auth-service**: Running en puerto 8081
- ✅ **users-service**: Running en puerto 8082
- ✅ **event-service**: Running en puerto 8086
- ✅ **order-service**: Running en puerto 8084
- ✅ **payment-service**: Running en puerto 8085

### Frontend
- ✅ **Angular App**: Running en puerto 3000
- ✅ **Componente Analytics**: Implementado en `/admin/analytics`
- ✅ **Servicio Analytics**: Implementado con DTOs completos
- ✅ **Routing**: Configurado con guards (adminGuard + emailVerifiedGuard)

---

## 📋 Pasos para Probar el Sistema Completo

### Paso 1: Crear Usuario Administrador

Ejecuta este comando para crear un usuario ADMIN:

```powershell
# Navegar al directorio raíz
cd c:\Users\david\Documents\ps-packedgo

# Ejecutar script de inicialización de datos
.\init-default-data.ps1
```

**Credenciales del usuario ADMIN creado:**
- Email: `admin@packedgo.com`
- Password: `Admin123!`
- Role: `ADMIN`

---

### Paso 2: Verificar que el Backend Funciona

```powershell
# Verificar health del analytics service
curl http://localhost:8087/api/api/dashboard/health
# Debe responder: "Analytics Service is UP"

# Verificar que todos los servicios están corriendo
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | Select-String "service"
```

---

### Paso 3: Probar el Frontend

#### 3.1 Abrir el navegador
```
http://localhost:3000/admin/login
```

#### 3.2 Iniciar sesión
- **Email**: `admin@packedgo.com`
- **Password**: `Admin123!`

#### 3.3 Navegar al Dashboard de Analytics

Después del login exitoso, deberías estar en el **Admin Dashboard**.

**Opción A**: Hacer clic en el botón **"Analíticas"** que ya existe en el dashboard.

**Opción B**: Navegar directamente a:
```
http://localhost:3000/admin/analytics
```

---

## 🎨 Lo que Deberías Ver en el Dashboard

### 1. **KPI Cards** (4 tarjetas superiores)
```
┌─────────────────┬─────────────────┬─────────────────┬─────────────────┐
│ 💰 Ingresos     │ 🎫 Tickets      │ 🎉 Eventos      │ 📊 Ocupación    │
│    $0.00        │    0            │    0            │    0%           │
│    ± 0%         │    ± 0          │    ± 0          │    ± 0%         │
└─────────────────┴─────────────────┴─────────────────┴─────────────────┘
```

### 2. **Desglose de Ingresos**
- Tickets vs Consumos con barras de progreso
- Porcentajes y valores en ARS

### 3. **Crecimiento Mensual**
- Porcentaje grande con icono (↑ verde / ↓ rojo)

### 4. **Top 5 Eventos**
- Tabla con ranking, nombre, ingresos, tickets vendidos

### 5. **Top 5 Consumos**
- Tabla con ranking, producto, ingresos, cantidad vendida

### 6. **Tendencias Diarias**
- Últimos 7 días con fecha, tickets, ingresos

### 7. **Botón de Actualización**
- En la esquina superior derecha
- Auto-refresh cada 5 minutos

---

## 🔧 Si Ves Datos en Cero (Estado Inicial)

Es **NORMAL** ver todos los valores en cero si no hay datos. El sistema muestra:

- ✅ **Mensajes de estado vacío**: "No hay eventos disponibles", "No hay consumos registrados", etc.
- ✅ **KPIs en 0**: Normal sin transacciones
- ✅ **Estructura completa**: Todas las secciones visibles

### Para Generar Datos de Prueba

1. **Crear Eventos** (usa el módulo de gestión de eventos del admin)
2. **Registrar Consumidores** (crear cuentas de prueba)
3. **Hacer Pedidos** (simular compras de tickets)
4. **Procesar Pagos** (completar transacciones)

Después de crear datos, **refresca el dashboard** (botón o espera 5 min) y verás las métricas pobladas.

---

## 🐛 Troubleshooting

### Problema: "Error al cargar el dashboard"

**Solución 1**: Verificar que el backend responde
```powershell
curl http://localhost:8087/api/api/dashboard/health
```

**Solución 2**: Verificar el token JWT
- Abre DevTools (F12) → Application → Local Storage
- Verifica que existe `access_token`
- Si no existe, cierra sesión e inicia sesión de nuevo

**Solución 3**: Revisar consola del navegador
- F12 → Console
- Buscar errores 401 (no autorizado) o 500 (error servidor)

---

### Problema: "No puedo hacer login"

**Causa**: No existe el usuario ADMIN

**Solución**: Ejecutar script de inicialización
```powershell
.\init-default-data.ps1
```

---

### Problema: "Página Analytics no carga / 404"

**Causa**: Ruta incorrecta o guards bloqueando

**Solución**:
1. Verificar que estás logueado como ADMIN
2. Verificar la URL: `http://localhost:3000/admin/analytics`
3. Revisar consola del navegador para errores de routing

---

### Problema: "Auto-refresh no funciona"

**Es normal**: El auto-refresh ocurre cada **5 minutos**. 

Para ver la actualización inmediata:
- Haz clic en el botón **"Actualizar"** (esquina superior derecha)

---

## 📊 Arquitectura de la Implementación

### Backend
```
analytics-service:8087
├── GET /api/api/dashboard/health          → Health check
├── GET /api/api/dashboard                 → Dashboard completo (requiere JWT)
└── GET /api/api/dashboard/organizer/{id}  → Dashboard por organizador (SUPER_ADMIN)
```

### Frontend
```
src/app/
├── services/
│   └── analytics.service.ts               → Servicio HTTP con 13 DTOs
├── components/analytics/
│   ├── admin-analytics.component.ts       → Lógica del componente
│   ├── admin-analytics.component.html     → Template con 260+ líneas
│   └── admin-analytics.component.css      → Estilos modernos
└── app.routes.ts                          → Ruta protegida con guards
```

### Flujo de Datos
```
1. Usuario hace clic en "Analíticas"
2. Angular navega a /admin/analytics
3. adminGuard verifica rol ADMIN
4. emailVerifiedGuard verifica email confirmado
5. Componente carga → llama analytics.service.getDashboard()
6. Servicio extrae JWT de localStorage
7. HTTP GET a http://localhost:8087/api/api/dashboard
8. Backend valida JWT y retorna DashboardDTO
9. Componente recibe datos y actualiza la vista
10. Template renderiza métricas, tablas, gráficos
11. Auto-refresh cada 5 minutos repite desde paso 5
```

---

## 🎉 Prueba Exitosa - Checklist

- [ ] Backend health check responde "Analytics Service is UP"
- [ ] Login exitoso con admin@packedgo.com
- [ ] Dashboard de admin carga correctamente
- [ ] Botón "Analíticas" visible en el dashboard
- [ ] Al hacer clic, navega a /admin/analytics
- [ ] Página de analytics carga sin errores
- [ ] Se ven las 4 KPI cards superiores
- [ ] Se ve la sección de desglose de ingresos
- [ ] Se ve el panel de crecimiento mensual
- [ ] Se ven las tablas de Top 5 (eventos y consumos)
- [ ] Se ve la tabla de tendencias diarias
- [ ] Botón "Actualizar" funciona (recarga datos)
- [ ] DevTools Console sin errores 404/500
- [ ] DevTools Network muestra petición exitosa a /api/api/dashboard

---

## 📝 Notas Importantes

1. **Primer Arranque**: Es normal ver valores en 0 sin datos históricos
2. **JWT Expiración**: Token expira según configuración del auth-service
3. **CORS**: Configurado para http://localhost:3000
4. **Guards**: Protegen la ruta - solo ADMIN con email verificado
5. **Auto-refresh**: 5 minutos configurable en `refreshInterval`
6. **Context Path**: Backend usa `/api/api/dashboard` (configuración heredada)

---

## 🚀 Siguiente Paso

Una vez verificado que todo funciona:

1. **Generar datos de prueba** para ver métricas reales
2. **Personalizar estilos** si es necesario
3. **Ajustar intervalos** de auto-refresh
4. **Agregar más gráficos** (Chart.js, ng2-charts, etc.)
5. **Exportar a PDF/Excel** funcionalidad

---

## 📞 Soporte

Si encuentras errores:
1. Revisar logs de Docker: `docker logs back-analytics-service-1`
2. Revisar consola del navegador (F12)
3. Verificar Network tab para ver requests/responses
4. Revisar que todos los servicios estén UP: `docker ps`

---

**Desarrollado por**: PackedGo Team  
**Fecha**: Noviembre 2025  
**Versión**: 1.0.0
