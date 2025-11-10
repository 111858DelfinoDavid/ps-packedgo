# 🎨 FRONTEND ANGULAR - DASHBOARD ANALYTICS

## 📋 Guía para Implementar el Dashboard de Analytics en Angular

### **1. Instalar dependencias para gráficos**

```bash
cd packedgo/front-angular
npm install chart.js ng2-charts --save
```

---

### **2. Crear el servicio de Analytics**

**Archivo**: `src/app/core/services/analytics.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardData {
  organizerId: number;
  organizerName: string;
  lastUpdated: string;
  salesMetrics: SalesMetrics;
  eventMetrics: EventMetrics;
  consumptionMetrics: ConsumptionMetrics;
  revenueMetrics: RevenueMetrics;
  topPerformers: TopPerformers;
  trends: Trends;
}

export interface SalesMetrics {
  totalTicketsSold: number;
  ticketsSoldToday: number;
  ticketsSoldThisWeek: number;
  ticketsSoldThisMonth: number;
  totalOrders: number;
  paidOrders: number;
  pendingOrders: number;
  cancelledOrders: number;
  conversionRate: number;
  averageOrderValue: number;
}

export interface EventMetrics {
  totalEvents: number;
  activeEvents: number;
  completedEvents: number;
  cancelledEvents: number;
  upcomingEvents: number;
  totalCapacity: number;
  occupiedCapacity: number;
  averageOccupancyRate: number;
  mostSoldEventName: string;
  mostSoldEventTickets: number;
}

export interface ConsumptionMetrics {
  totalConsumptions: number;
  activeConsumptions: number;
  totalConsumptionsSold: number;
  consumptionsRedeemed: number;
  consumptionsPending: number;
  redemptionRate: number;
  mostSoldConsumptionName: string;
  mostSoldConsumptionQuantity: number;
}

export interface RevenueMetrics {
  totalRevenue: number;
  revenueToday: number;
  revenueThisWeek: number;
  revenueThisMonth: number;
  revenueFromTickets: number;
  revenueFromConsumptions: number;
  averageRevenuePerEvent: number;
  averageRevenuePerCustomer: number;
}

export interface TopPerformers {
  topEvents: EventPerformance[];
  topConsumptions: ConsumptionPerformance[];
}

export interface EventPerformance {
  eventId: number;
  eventName: string;
  ticketsSold: number;
  revenue: number;
  occupancyRate: number;
}

export interface ConsumptionPerformance {
  consumptionId: number;
  consumptionName: string;
  quantitySold: number;
  revenue: number;
  redemptionRate: number;
}

export interface Trends {
  dailySales: DailyTrend[];
  dailyRevenue: DailyTrend[];
  monthlySales: MonthlyTrend[];
  monthlyRevenue: MonthlyTrend[];
}

export interface DailyTrend {
  date: string;
  count: number;
  amount: number;
}

export interface MonthlyTrend {
  year: number;
  month: number;
  monthName: string;
  count: number;
  amount: number;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private apiUrl = 'http://localhost:8087/api/dashboard';

  constructor(private http: HttpClient) {}

  /**
   * Obtiene el dashboard completo del organizador autenticado
   */
  getDashboard(): Observable<DashboardData> {
    const token = localStorage.getItem('access_token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<DashboardData>(this.apiUrl, { headers });
  }

  /**
   * Verifica el health del servicio de analytics
   */
  checkHealth(): Observable<string> {
    return this.http.get(`${this.apiUrl}/health`, { responseType: 'text' });
  }
}
```

---

### **3. Crear el componente de Dashboard**

**Archivo**: `src/app/features/admin/dashboard-analytics/dashboard-analytics.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { AnalyticsService, DashboardData } from '../../../core/services/analytics.service';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

@Component({
  selector: 'app-dashboard-analytics',
  templateUrl: './dashboard-analytics.component.html',
  styleUrls: ['./dashboard-analytics.component.css']
})
export class DashboardAnalyticsComponent implements OnInit {
  
  dashboardData: DashboardData | null = null;
  loading = true;
  error: string | null = null;

  // Configuración de gráficos
  public lineChartType: ChartType = 'line';
  public barChartType: ChartType = 'bar';
  public pieChartType: ChartType = 'pie';

  // Datos para gráficos de ventas diarias
  public dailySalesChartData: ChartData<'line'> = {
    labels: [],
    datasets: []
  };

  // Datos para gráficos de ingresos mensuales
  public monthlyRevenueChartData: ChartData<'bar'> = {
    labels: [],
    datasets: []
  };

  // Datos para gráfico de distribución de órdenes
  public ordersDistributionChartData: ChartData<'pie'> = {
    labels: ['Pagadas', 'Pendientes', 'Canceladas'],
    datasets: []
  };

  constructor(private analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.analyticsService.getDashboard().subscribe({
      next: (data) => {
        this.dashboardData = data;
        this.prepareCharts();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error cargando dashboard:', err);
        this.error = 'Error al cargar las métricas. Por favor, inténtalo de nuevo.';
        this.loading = false;
      }
    });
  }

  prepareCharts(): void {
    if (!this.dashboardData) return;

    // Gráfico de ventas diarias (últimos 30 días)
    this.dailySalesChartData = {
      labels: this.dashboardData.trends.dailySales.map(d => d.date),
      datasets: [
        {
          data: this.dashboardData.trends.dailySales.map(d => d.count),
          label: 'Ventas Diarias',
          borderColor: '#4CAF50',
          backgroundColor: 'rgba(76, 175, 80, 0.2)',
          fill: true
        }
      ]
    };

    // Gráfico de ingresos mensuales
    this.monthlyRevenueChartData = {
      labels: this.dashboardData.trends.monthlySales.map(m => m.monthName),
      datasets: [
        {
          data: this.dashboardData.trends.monthlySales.map(m => m.amount),
          label: 'Ingresos Mensuales',
          backgroundColor: '#2196F3'
        }
      ]
    };

    // Gráfico de distribución de órdenes
    this.ordersDistributionChartData = {
      labels: ['Pagadas', 'Pendientes', 'Canceladas'],
      datasets: [
        {
          data: [
            this.dashboardData.salesMetrics.paidOrders,
            this.dashboardData.salesMetrics.pendingOrders,
            this.dashboardData.salesMetrics.cancelledOrders
          ],
          backgroundColor: ['#4CAF50', '#FF9800', '#F44336']
        }
      ]
    };
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-AR', {
      style: 'currency',
      currency: 'ARS'
    }).format(value);
  }

  formatPercent(value: number): string {
    return `${value.toFixed(2)}%`;
  }

  refresh(): void {
    this.loadDashboard();
  }
}
```

---

### **4. HTML del Dashboard**

**Archivo**: `src/app/features/admin/dashboard-analytics/dashboard-analytics.component.html`

```html
<div class="container-fluid dashboard-analytics mt-4">
  
  <!-- Header -->
  <div class="row mb-4">
    <div class="col-12">
      <div class="d-flex justify-content-between align-items-center">
        <h1 class="display-5">📊 Dashboard de Analytics</h1>
        <button class="btn btn-primary" (click)="refresh()" [disabled]="loading">
          <i class="bi bi-arrow-clockwise"></i> Actualizar
        </button>
      </div>
      <p class="text-muted" *ngIf="dashboardData">
        Última actualización: {{ dashboardData.lastUpdated | date:'dd/MM/yyyy HH:mm' }}
      </p>
    </div>
  </div>

  <!-- Loading State -->
  <div *ngIf="loading" class="text-center my-5">
    <div class="spinner-border text-primary" role="status">
      <span class="visually-hidden">Cargando...</span>
    </div>
    <p class="mt-3">Cargando métricas...</p>
  </div>

  <!-- Error State -->
  <div *ngIf="error" class="alert alert-danger" role="alert">
    {{ error }}
  </div>

  <!-- Dashboard Content -->
  <div *ngIf="!loading && !error && dashboardData">

    <!-- === KPIs PRINCIPALES === -->
    <div class="row mb-4">
      <!-- Total Revenue -->
      <div class="col-md-3 mb-3">
        <div class="card text-white bg-success">
          <div class="card-body">
            <h6 class="card-title">💰 Ingresos Totales</h6>
            <h2>{{ formatCurrency(dashboardData.revenueMetrics.totalRevenue) }}</h2>
            <small>{{ formatCurrency(dashboardData.revenueMetrics.revenueThisMonth) }} este mes</small>
          </div>
        </div>
      </div>

      <!-- Tickets Sold -->
      <div class="col-md-3 mb-3">
        <div class="card text-white bg-info">
          <div class="card-body">
            <h6 class="card-title">🎟️ Tickets Vendidos</h6>
            <h2>{{ dashboardData.salesMetrics.totalTicketsSold }}</h2>
            <small>{{ dashboardData.salesMetrics.ticketsSoldThisMonth }} este mes</small>
          </div>
        </div>
      </div>

      <!-- Active Events -->
      <div class="col-md-3 mb-3">
        <div class="card text-white bg-primary">
          <div class="card-body">
            <h6 class="card-title">🎪 Eventos Activos</h6>
            <h2>{{ dashboardData.eventMetrics.activeEvents }}</h2>
            <small>{{ dashboardData.eventMetrics.upcomingEvents }} próximos</small>
          </div>
        </div>
      </div>

      <!-- Conversion Rate -->
      <div class="col-md-3 mb-3">
        <div class="card text-white bg-warning">
          <div class="card-body">
            <h6 class="card-title">📈 Tasa de Conversión</h6>
            <h2>{{ formatPercent(dashboardData.salesMetrics.conversionRate) }}</h2>
            <small>{{ dashboardData.salesMetrics.paidOrders }}/{{ dashboardData.salesMetrics.totalOrders }} órdenes</small>
          </div>
        </div>
      </div>
    </div>

    <!-- === GRÁFICOS === -->
    <div class="row mb-4">
      <!-- Ventas Diarias -->
      <div class="col-md-8 mb-3">
        <div class="card">
          <div class="card-header">
            <h5>📈 Ventas Diarias (Últimos 30 días)</h5>
          </div>
          <div class="card-body">
            <canvas baseChart
              [data]="dailySalesChartData"
              [type]="lineChartType">
            </canvas>
          </div>
        </div>
      </div>

      <!-- Distribución de Órdenes -->
      <div class="col-md-4 mb-3">
        <div class="card">
          <div class="card-header">
            <h5>🥧 Distribución de Órdenes</h5>
          </div>
          <div class="card-body">
            <canvas baseChart
              [data]="ordersDistributionChartData"
              [type]="pieChartType">
            </canvas>
          </div>
        </div>
      </div>
    </div>

    <!-- Ingresos Mensuales -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card">
          <div class="card-header">
            <h5>💵 Ingresos Mensuales (Último Año)</h5>
          </div>
          <div class="card-body">
            <canvas baseChart
              [data]="monthlyRevenueChartData"
              [type]="barChartType">
            </canvas>
          </div>
        </div>
      </div>
    </div>

    <!-- === MÉTRICAS DETALLADAS === -->
    <div class="row mb-4">
      <!-- Sales Metrics -->
      <div class="col-md-6 mb-3">
        <div class="card">
          <div class="card-header bg-primary text-white">
            <h5>🎟️ Métricas de Ventas</h5>
          </div>
          <div class="card-body">
            <ul class="list-group list-group-flush">
              <li class="list-group-item d-flex justify-content-between">
                <span>Tickets Vendidos Hoy:</span>
                <strong>{{ dashboardData.salesMetrics.ticketsSoldToday }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Tickets Esta Semana:</span>
                <strong>{{ dashboardData.salesMetrics.ticketsSoldThisWeek }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Valor Promedio de Orden:</span>
                <strong>{{ formatCurrency(dashboardData.salesMetrics.averageOrderValue) }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Órdenes Pendientes:</span>
                <strong class="text-warning">{{ dashboardData.salesMetrics.pendingOrders }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Órdenes Canceladas:</span>
                <strong class="text-danger">{{ dashboardData.salesMetrics.cancelledOrders }}</strong>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- Event Metrics -->
      <div class="col-md-6 mb-3">
        <div class="card">
          <div class="card-header bg-info text-white">
            <h5>🎪 Métricas de Eventos</h5>
          </div>
          <div class="card-body">
            <ul class="list-group list-group-flush">
              <li class="list-group-item d-flex justify-content-between">
                <span>Total Eventos:</span>
                <strong>{{ dashboardData.eventMetrics.totalEvents }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Eventos Completados:</span>
                <strong>{{ dashboardData.eventMetrics.completedEvents }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Capacidad Total:</span>
                <strong>{{ dashboardData.eventMetrics.totalCapacity }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Ocupación:</span>
                <strong>{{ dashboardData.eventMetrics.occupiedCapacity }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Tasa de Ocupación:</span>
                <strong class="text-success">{{ formatPercent(dashboardData.eventMetrics.averageOccupancyRate) }}</strong>
              </li>
              <li class="list-group-item d-flex justify-content-between">
                <span>Evento Más Vendido:</span>
                <strong class="text-truncate" style="max-width: 60%;">{{ dashboardData.eventMetrics.mostSoldEventName }}</strong>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- === TOP PERFORMERS === -->
    <div class="row mb-4">
      <!-- Top Events -->
      <div class="col-md-6 mb-3">
        <div class="card">
          <div class="card-header bg-success text-white">
            <h5>🏆 Top 5 Eventos Más Vendidos</h5>
          </div>
          <div class="card-body">
            <table class="table table-hover">
              <thead>
                <tr>
                  <th>Evento</th>
                  <th>Tickets</th>
                  <th>Ingresos</th>
                  <th>Ocupación</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let event of dashboardData.topPerformers.topEvents; let i = index">
                  <td>
                    <span class="badge bg-primary me-2">{{ i + 1 }}</span>
                    {{ event.eventName }}
                  </td>
                  <td>{{ event.ticketsSold }}</td>
                  <td>{{ formatCurrency(event.revenue) }}</td>
                  <td>
                    <span class="badge bg-success">{{ formatPercent(event.occupancyRate) }}</span>
                  </td>
                </tr>
                <tr *ngIf="dashboardData.topPerformers.topEvents.length === 0">
                  <td colspan="4" class="text-center text-muted">No hay datos disponibles</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Top Consumptions -->
      <div class="col-md-6 mb-3">
        <div class="card">
          <div class="card-header bg-warning text-dark">
            <h5>🍔 Top 5 Consumiciones Más Vendidas</h5>
          </div>
          <div class="card-body">
            <table class="table table-hover">
              <thead>
                <tr>
                  <th>Consumición</th>
                  <th>Cantidad</th>
                  <th>Ingresos</th>
                  <th>Canje</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let consumption of dashboardData.topPerformers.topConsumptions; let i = index">
                  <td>
                    <span class="badge bg-primary me-2">{{ i + 1 }}</span>
                    {{ consumption.consumptionName }}
                  </td>
                  <td>{{ consumption.quantitySold }}</td>
                  <td>{{ formatCurrency(consumption.revenue) }}</td>
                  <td>
                    <span class="badge bg-info">{{ formatPercent(consumption.redemptionRate) }}</span>
                  </td>
                </tr>
                <tr *ngIf="dashboardData.topPerformers.topConsumptions.length === 0">
                  <td colspan="4" class="text-center text-muted">No hay datos disponibles</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- === INGRESOS === -->
    <div class="row mb-4">
      <div class="col-12">
        <div class="card">
          <div class="card-header bg-success text-white">
            <h5>💰 Desglose de Ingresos</h5>
          </div>
          <div class="card-body">
            <div class="row">
              <div class="col-md-3">
                <h6>Hoy</h6>
                <h3>{{ formatCurrency(dashboardData.revenueMetrics.revenueToday) }}</h3>
              </div>
              <div class="col-md-3">
                <h6>Esta Semana</h6>
                <h3>{{ formatCurrency(dashboardData.revenueMetrics.revenueThisWeek) }}</h3>
              </div>
              <div class="col-md-3">
                <h6>Este Mes</h6>
                <h3>{{ formatCurrency(dashboardData.revenueMetrics.revenueThisMonth) }}</h3>
              </div>
              <div class="col-md-3">
                <h6>Total</h6>
                <h3>{{ formatCurrency(dashboardData.revenueMetrics.totalRevenue) }}</h3>
              </div>
            </div>
            <hr>
            <div class="row mt-3">
              <div class="col-md-4">
                <p><strong>Por Entradas:</strong> {{ formatCurrency(dashboardData.revenueMetrics.revenueFromTickets) }}</p>
              </div>
              <div class="col-md-4">
                <p><strong>Por Consumiciones:</strong> {{ formatCurrency(dashboardData.revenueMetrics.revenueFromConsumptions) }}</p>
              </div>
              <div class="col-md-4">
                <p><strong>Promedio por Cliente:</strong> {{ formatCurrency(dashboardData.revenueMetrics.averageRevenuePerCustomer) }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</div>
```

---

### **5. Estilos CSS**

**Archivo**: `src/app/features/admin/dashboard-analytics/dashboard-analytics.component.css`

```css
.dashboard-analytics {
  padding: 20px;
}

.card {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border-radius: 8px;
  transition: transform 0.2s;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.card-body h2 {
  font-weight: 700;
  margin-bottom: 5px;
}

.card-body small {
  font-size: 0.875rem;
  opacity: 0.9;
}

canvas {
  max-height: 300px;
}

.text-truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-group-item {
  border: none;
  padding: 12px 15px;
}

.table {
  margin-bottom: 0;
}

.badge {
  font-size: 0.875rem;
}

/* Responsive */
@media (max-width: 768px) {
  .card-body h2 {
    font-size: 1.5rem;
  }
}
```

---

### **6. Agregar ruta en el routing**

**Archivo**: `src/app/app.routes.ts`

```typescript
import { Routes } from '@angular/router';
import { DashboardAnalyticsComponent } from './features/admin/dashboard-analytics/dashboard-analytics.component';
import { AuthGuard } from './core/guards/auth.guard';
import { AdminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // ... otras rutas
  {
    path: 'admin/analytics',
    component: DashboardAnalyticsComponent,
    canActivate: [AuthGuard, AdminGuard]
  },
  // ... otras rutas
];
```

---

### **7. Actualizar proxy.conf.json**

**Archivo**: `proxy.conf.json`

```json
{
  "/api/auth": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  },
  "/api/users": {
    "target": "http://localhost:8082",
    "secure": false",
    "changeOrigin": true
  },
  "/api/events": {
    "target": "http://localhost:8086",
    "secure": false",
    "changeOrigin": true
  },
  "/api/orders": {
    "target": "http://localhost:8084",
    "secure": false",
    "changeOrigin": true
  },
  "/api/cart": {
    "target": "http://localhost:8084",
    "secure": false",
    "changeOrigin": true
  },
  "/api/payments": {
    "target": "http://localhost:8085",
    "secure": false",
    "changeOrigin": true
  },
  "/api/dashboard": {
    "target": "http://localhost:8087",
    "secure": false",
    "changeOrigin": true
  }
}
```

---

## 🚀 Pasos para Probar

### 1. Compilar backend
```powershell
cd packedgo\back\analytics-service
.\mvnw clean install
```

### 2. Iniciar Analytics-Service
```powershell
.\mvnw spring-boot:run
```

### 3. Instalar dependencias de frontend
```powershell
cd packedgo\front-angular
npm install
```

### 4. Iniciar frontend
```powershell
ng serve
```

### 5. Acceder al dashboard
- URL: http://localhost:4200/admin/analytics
- Requiere: Login con usuario ADMIN

---

## 📚 Recursos Adicionales

- **Chart.js**: https://www.chartjs.org/
- **ng2-charts**: https://valor-software.com/ng2-charts/
- **Bootstrap Icons**: https://icons.getbootstrap.com/

---

¡El dashboard de analytics está listo para usar! 🎉
