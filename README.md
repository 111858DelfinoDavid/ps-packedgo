# PackedGo

## Plataforma de Gestión de Eventos con Sistema de Consumiciones Prepagadas

PackedGo es una plataforma web integral desarrollada bajo una arquitectura de microservicios que revoluciona la gestión y venta de entradas para eventos mediante un innovador sistema de consumiciones prepagadas. La plataforma permite a los usuarios construir paquetes personalizados combinando entradas con consumiciones, realizar pagos seguros y gestionar el acceso durante el evento mediante códigos QR únicos.

---

## 🚀 Inicio Rápido

### Requisitos
- Docker Desktop
- Node.js 18+ y npm
- Angular CLI 19+

### Instalación en 3 pasos

```bash
# 1. Clonar el repositorio
git clone <URL_DEL_REPOSITORIO>
cd ps-packedgo

# 2. Levantar backend con Docker
cd packedgo/back
docker-compose up -d --build

# 3. Ejecutar frontend
cd ../front-angular
npm install
npm start
```

**¡Listo!** Abre http://localhost:3000 en tu navegador.

📖 **Documentación completa**: Ver [INSTALACION.md](./INSTALACION.md)

---

## 🏗️ Arquitectura del Sistema

### Stack Tecnológico
- **Backend:** Java 17 + Spring Boot 3.2
- **Frontend:** Angular 17 + TypeScript
- **Base de Datos:** PostgreSQL 15 (Database per Service)
- **Containerización:** Docker + Docker Compose
- **API Gateway:** Nginx
- **Seguridad:** Spring Security 6 + JWT
- **Procesamiento de Pagos:** MercadoPago SDK

### Microservicios

| Servicio | Puerto | Base de Datos | Responsabilidad |
|----------|--------|---------------|-----------------|
| [**auth-service**](./packedgo/back/auth-service/) | 8081 | auth_db (5433) | Autenticación y autorización |
| [**users-service**](./packedgo/back/users-service/) | 8082 | users_db (5434) | Gestión de perfiles de usuario |
| **event-service** | 8086 | event_db (5435) | Gestión de eventos, consumiciones y validación QR |
| **order-service** | 8084 | order_db (5436) | Carritos y órdenes de compra |
| **payment-service** | 8085 | payment_db (5437) | Procesamiento de pagos (Stripe) |
| **analytics-service** | 8087 | - | Métricas y reportes |
| **api-gateway** | 8080 | - | Gateway y load balancer |

---

## 🚀 Características Principales

### Para Administradores
- **Autenticación diferenciada:** Login con email + contraseña
- **Gestión completa de eventos:** Creación, configuración y monitoreo
- **Control de stock:** Gestión en tiempo real de disponibilidad
- **Panel de analytics:** Métricas, reportes y dashboards
- **Validación de entrada:** Sistema QR para control de acceso
- **Gestión de permisos:** Roles y permisos granulares

### Para Clientes
- **Autenticación simplificada:** Login con DNI + contraseña
- **Exploración de eventos:** Catálogo completo con filtros
- **Constructor de paquetes:** Personalización de entrada + consumiciones
- **Pagos seguros:** Integración completa con MercadoPago
- **Códigos QR únicos:** Acceso y canje digital de consumiciones
- **Perfil de usuario:** Gestión de datos personales

---

## 🔒 Sistema de Autenticación Diferenciada

### Administradores (EMAIL)
- Login: `email + contraseña`
- Roles: `ADMIN`, `SUPER_ADMIN`
- Permisos: Gestión completa del sistema
- Registro: Requiere código de autorización

### Clientes (DOCUMENT)
- Login: `DNI + contraseña`
- Rol: `CUSTOMER`
- Permisos: Navegación y compras
- Registro: Validación automática de email

---

## 🛠️ Configuración y Despliegue

### Prerrequisitos
- Docker >= 20.10
- Docker Compose >= 2.0
- Java 21 (para desarrollo)
- Node.js 18+ (para frontend)
- PostgreSQL 15 (si no usar Docker)

### Instalación Rápida

```bash
# Clonar el repositorio
git clone https://github.com/username/ps-packedgo.git
cd ps-packedgo

# Configurar variables de entorno
cp packedgo/back/auth-service/.env.example packedgo/back/auth-service/.env
cp packedgo/back/users-service/.env.example packedgo/back/users-service/.env
# ... repetir para todos los servicios

# Levantar todos los servicios
cd packedgo/back
docker-compose up --build
```

### Configuración Individual por Servicio

Cada microservicio incluye su propio archivo `.env.example` con todas las variables necesarias. Copiar a `.env` y completar con valores reales.

#### Variables Críticas Comunes
```bash
# Base de datos
DATABASE_URL=jdbc:postgresql://service-db:5432/service_db
DATABASE_USER=db_user
DATABASE_PASSWORD=secure_password

# JWT (solo auth-service)
JWT_SECRET=your_jwt_secret_minimum_32_characters_here
JWT_EXPIRATION=3600000

# Email (auth-service)
EMAIL_USERNAME=your_gmail@gmail.com
EMAIL_PASSWORD=your_app_password

# Stripe (payment-service)
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_WEBHOOK_SECRET=your_stripe_webhook_secret
```

---

## 📊 Base de Datos

### Estrategia: Database per Service
Cada microservicio mantiene su propia base de datos PostgreSQL independiente para garantizar desacoplamiento total y escalabilidad independiente.

| Base de Datos | Puerto | Descripción |
|---------------|--------|-------------|
| auth_db | 5433 | Usuarios, sesiones, tokens de verificación |
| users_db | 5434 | Perfiles de usuario y datos personales |
| event_db | 5435 | Eventos, consumiciones y stock |
| payment_db | 5437 | Transacciones y pagos Stripe |
| payment_db | 5437 | Transacciones y pagos MercadoPago |

---

## 🔄 Flujos de Negocio Principales

### Flujo de Registro y Autenticación Cliente
1. **Cliente se registra** → AUTH-SERVICE valida datos únicos
2. **Creación exitosa** → AUTO-llamada a USERS-SERVICE para crear perfil
3. **Email de verificación** → Cliente confirma cuenta
4. **Login con DNI** → Generación de JWT + permisos

### Flujo de Compra de Eventos
1. **Exploración** → EVENT-SERVICE muestra eventos disponibles
2. **Construcción de paquete** → ORDER-SERVICE gestiona carrito
3. **Checkout** → Validación de stock y creación de orden
4. **Procesamiento de pago** → PAYMENT-SERVICE + MercadoPago
5. **Confirmación** → QR-SERVICE genera código único
6. **Email con QR** → Cliente recibe entrada digital

### Flujo de Validación en Evento
1. **Entrada al evento** → Escaneo QR para acceso (una vez)
2. **Consumo de productos** → Escaneo QR para canje progresivo
3. **Auditoría** → Registro completo de validaciones y consumos

---

## 📁 Estructura del Proyecto

```
ps-packedgo/
├── packedgo/
│   ├── back/                          # Backend - Microservicios
│   │   ├── auth-service/              # Autenticación y autorización
│   │   ├── users-service/             # Perfiles de usuario
│   │   ├── event-service/             # Gestión de eventos
│   │   ├── order-service/             # Órdenes y carritos
│   │   ├── payment-service/           # Procesamiento de pagos
│   │   ├── qr-service/                # Códigos QR y validación
│   │   ├── analytics-service/         # Métricas y reportes
│   │   ├── api-gateway/               # Gateway y balanceador
│   │   └── docker-compose.yml         # Orquestación completa
│   └── front/                         # Frontend Angular
│       └── packedgo-app/
├── docs/                              # Documentación
├── README.md
└── packedgo_architecture_document.md  # Arquitectura detallada
```

---

## 🔧 Scripts de Desarrollo

### Backend
```bash
# Levantar todos los servicios
cd packedgo/back
docker-compose up --build

# Levantar servicio específico
docker-compose up auth-service users-service

# Logs de servicio específico
docker-compose logs -f auth-service

# Rebuild sin cache
docker-compose build --no-cache auth-service
```

### Frontend
```bash
# Instalar dependencias
cd packedgo/front/packedgo-app
npm install

# Servidor de desarrollo
ng serve

# Build de producción
ng build --prod
```

---

## 🧪 Testing

### Testing por Microservicio
Cada servicio incluye su suite completa de tests:

```bash
# Tests unitarios
./mvnw test

# Tests de integración
./mvnw test -Dtest=**/*IntegrationTest

# Coverage report
./mvnw jacoco:report
```

### Testing de Integración Inter-Servicios
```bash
# Levantar stack completo para testing
docker-compose -f docker-compose.test.yml up

# Ejecutar tests end-to-end
npm run e2e
```

---

## 📖 Documentación Detallada

### Documentación por Microservicio
- [AUTH-SERVICE](./packedgo/back/auth-service/README.md) - Sistema de autenticación diferenciada
- [USERS-SERVICE](./packedgo/back/users-service/README.md) - Gestión de perfiles de usuario
- [Próximamente] EVENT-SERVICE - Gestión de eventos y consumiciones
- [Próximamente] ORDER-SERVICE - Procesamiento de órdenes
- [Próximamente] PAYMENT-SERVICE - Integración MercadoPago
- [Próximamente] QR-SERVICE - Códigos QR y validación
- [Próximamente] ANALYTICS-SERVICE - Métricas y reportes

### Documentación Técnica
- [Arquitectura Completa](./packedgo_architecture_document.md) - Especificaciones técnicas detalladas
- [Base de Datos](./Lista%20de%20Bases%20de%20Datos%20y%20Tablas%20PackedGo.txt) - Schema completo de todas las bases

---

## 🚦 Estado del Desarrollo

### ✅ Completado
- [x] AUTH-SERVICE - Autenticación diferenciada completa
- [x] USERS-SERVICE - Gestión de perfiles básica
- [x] Arquitectura de microservicios base
- [x] Configuración Docker Compose
- [x] Integración auth-service ↔ users-service

### 🚧 En Desarrollo
- [ ] EVENT-SERVICE - Gestión de eventos y stock
- [ ] ORDER-SERVICE - Carrito y procesamiento de órdenes
- [ ] PAYMENT-SERVICE - Integración MercadoPago
- [ ] Frontend Angular - Interfaces de usuario

### 📅 Próximas Fases
- [ ] QR-SERVICE - Sistema de códigos QR
- [ ] ANALYTICS-SERVICE - Dashboard y reportes
- [ ] API Gateway - Configuración Nginx
- [ ] Testing completo e2e
- [ ] Deployment en producción

---

## 👥 Equipo de Desarrollo

**Estudiantes - Tecnicatura Universitaria en Programación**  
**Universidad Tecnológica Nacional - Facultad Regional Córdoba**

- **David Elías Delfino** - Legajo: 111858
- **Agustín Luparia Mothe** - Legajo: 113973

**Año:** 2025  
**Proyecto:** Trabajo Final Integrador

---

## 📄 Licencia

Proyecto académico desarrollado para la Universidad Tecnológica Nacional - Facultad Regional Córdoba.

---

## 🔗 Links Útiles

- [Documentación Spring Boot](https://docs.spring.io/spring-boot/)
- [Angular Documentation](https://angular.io/docs)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [MercadoPago Developers](https://www.mercadopago.com.ar/developers/)
- [Docker Documentation](https://docs.docker.com/)

---

**Última actualización:** Septiembre 2025