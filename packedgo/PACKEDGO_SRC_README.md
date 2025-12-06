# 📦 PackedGo

**PackedGo** es una plataforma integral para la gestión de eventos, venta de entradas y control de acceso mediante códigos QR. Este repositorio contiene el código fuente completo del sistema, dividido en una arquitectura moderna de backend y frontend.

---

## 📂 Estructura del Proyecto

El proyecto se organiza en dos directorios principales:

### 🔙 [Backend (`/back`)](./back/BACKEND_README.md)
Contiene la arquitectura de **microservicios** construida con **Java 17** y **Spring Boot**.
- **Servicios:** Auth, Users, Event, Order, Payment, Analytics.
- **Infraestructura:** Docker, PostgreSQL, Nginx.
- **Documentación:** Ver [README del Backend](./back/BACKEND_README.md) para detalles de arquitectura y endpoints.

### 📱 [Frontend (`/front-angular`)](./front-angular/FRONTEND_README.md)
Contiene la aplicación web SPA desarrollada con **Angular 19**.
- **Características:** Paneles para Administradores, Clientes y Empleados.
- **Tecnologías:** Bootstrap 5, ZXing (Scanner QR), SweetAlert2.
- **Documentación:** Ver [README del Frontend](./front-angular/FRONTEND_README.md) para detalles de componentes y configuración.

---

## 🚀 Guía de Inicio Rápido

Para ejecutar el sistema completo en tu entorno local, sigue estos pasos:

### 1. Iniciar el Backend (Docker)
El backend está contenerizado para facilitar su despliegue.

```bash
cd back
# Levantar todos los servicios y bases de datos
docker-compose up -d --build
```
> **Nota:** Asegúrate de tener Docker Desktop corriendo. Los servicios estarán disponibles en los puertos definidos en `docker-compose.yml` (ej. Auth en 8081, Users en 8082).

### 2. Iniciar el Frontend (Angular)
El frontend requiere Node.js y se conecta a los microservicios mediante un proxy.

```bash
cd front-angular
# Instalar dependencias
npm install

# Iniciar servidor de desarrollo
npm start
```
> La aplicación estará disponible en: **http://localhost:3000/**

---

## 🛠️ Stack Tecnológico

| Capa | Tecnologías |
|------|-------------|
| **Frontend** | Angular 19, TypeScript, Bootstrap 5, HTML5, CSS3 |
| **Backend** | Java 17, Spring Boot 3, Spring Cloud, Maven |
| **Base de Datos** | PostgreSQL (Database per Service) |
| **DevOps** | Docker, Docker Compose |
| **Integraciones** | Stripe (Pagos), ZXing (QR) |

---

## 👥 Autores

Proyecto desarrollado como Trabajo Final Integrador para la **Tecnicatura Universitaria en Programación** (UTN-FRC).

*   **David Elías Delfino**
*   **Agustín Luparia Mothe**

---
© 2025 PackedGo
