# 📱 PackedGo Frontend

Aplicación web moderna desarrollada en **Angular 19** para la gestión integral de eventos, venta de entradas y control de acceso. Este frontend interactúa con una arquitectura de microservicios para ofrecer una experiencia fluida a Administradores, Clientes y Empleados.

## 🛠️ Tecnologías Principales

*   **Framework**: [Angular 19](https://angular.io/) (Standalone Components)
*   **Estilos**: [Bootstrap 5](https://getbootstrap.com/)
*   **Alertas**: [SweetAlert2](https://sweetalert2.github.io/)
*   **Escaneo QR**: [@zxing/ngx-scanner](https://github.com/zxing-js/ngx-scanner)
*   **Cliente HTTP**: Angular HttpClient
*   **Ruteo**: Angular Router

## 📂 Estructura del Proyecto

El proyecto sigue una arquitectura modular basada en características (`features`) y núcleo (`core`).

```
src/app/
├── core/                 # Lógica de negocio singleton
│   ├── guards/           # Guardias de ruta (Auth, Admin, Employee)
│   ├── interceptors/     # Interceptores HTTP (Token, Error)
│   └── services/         # Servicios de comunicación con APIs
├── features/             # Módulos funcionales
│   ├── admin/            # Panel de Administración
│   │   ├── admin-analytics       # Dashboard de métricas
│   │   ├── events-management     # ABM de Eventos
│   │   ├── employee-management   # Gestión de Staff
│   │   └── ...
│   ├── auth/             # Autenticación (Login/Register)
│   ├── customer/         # Área del Cliente
│   │   ├── event-detail          # Compra de entradas
│   │   ├── customer-dashboard    # Mis Tickets y Órdenes
│   │   └── checkout              # Pasarela de pago
│   ├── employee/         # Panel de Empleado
│   │   └── employee-dashboard    # Escáner QR y validación
│   └── landing/          # Página de inicio pública
└── shared/               # Componentes reutilizables (Navbar, Footer, Cards)
```

## 🚀 Características por Rol

### 👤 Cliente (Customer)
*   Exploración de eventos y detalles.
*   Carrito de compras y Checkout integrado.
*   Visualización de entradas adquiridas con código QR.
*   Historial de órdenes.

### 🛡️ Administrador (Admin)
*   Dashboard de analíticas (Ventas, Asistencia, Ingresos).
*   Gestión completa de Eventos (Crear, Editar, Pausar).
*   Gestión de Categorías y Consumiciones.
*   Gestión de Empleados y asignación a eventos.

### 👷 Empleado (Employee)
*   Acceso seguro mediante credenciales generadas por el admin.
*   **Escáner QR integrado** para validación de entradas.
*   Canje de consumiciones (bebidas/comida) mediante QR.
*   Visualización de estadísticas diarias de escaneo.

## ⚙️ Configuración y Ejecución

### Prerrequisitos
*   Node.js (v18 o superior)
*   npm o yarn
*   Angular CLI (`npm install -g @angular/cli`)

### Instalación
```bash
# Instalar dependencias
npm install
```

### Ejecución en Desarrollo
El proyecto utiliza un proxy para redirigir las llamadas a la API hacia los microservicios locales.

```bash
# Iniciar servidor de desarrollo con proxy
npm start
# O directamente:
ng serve --proxy-config proxy.conf.json
```
La aplicación estará disponible en `http://localhost:3000/`.

### Configuración de Proxy (`proxy.conf.json`)
El frontend redirige las peticiones `/api` a los distintos microservicios:
*   `/api` -> Auth Service
*   `/api/users` -> Users Service
*   `/api/events` -> Event Service

## 📦 Build para Producción

```bash
ng build
```
Los archivos compilados se generarán en la carpeta `dist/front-angular`.

## 🧪 Tests

```bash
# Unit Tests
ng test

# End-to-End Tests
ng e2e
```

---
**PackedGo** - Sistema de Gestión de Eventos
Desarrollado por David Elías Delfino y Agustín Luparia Mothe