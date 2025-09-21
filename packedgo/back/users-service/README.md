# Users Service - PackedGo

Microservicio de gestión de perfiles de usuario para la plataforma PackedGo.

## ?? Configuración Inicial

### 1. Variables de Entorno

Copia el archivo de ejemplo y configura tus variables:

```bash
cp .env.example .env
```

Luego edita el archivo `.env` con tus credenciales reales:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://db:5432/users_db
DATABASE_USER=tu_usuario_db
DATABASE_PASSWORD=tu_contraseña_db

# Server Configuration
SERVER_PORT=8082
```

### 2. Ejecutar con Docker

```bash
# Construir y ejecutar todos los servicios
docker compose up -d

# Ver logs
docker compose logs -f users-service

# Parar servicios
docker compose down

# Parar servicios y eliminar volúmenes
docker compose down --volumes
```

### 3. Ejecutar en desarrollo local

Si quieres ejecutar la aplicación sin Docker:

1. Configura una base de datos PostgreSQL local en puerto 5434
2. Actualiza las variables de entorno en `.env`
3. Ejecuta: `./mvnw spring-boot:run`

## ?? API Endpoints

### Gestión de Perfiles
- `POST /api/user-profiles` - Crear nuevo perfil
- `GET /api/user-profiles/{id}` - Obtener perfil por ID
- `PUT /api/user-profiles/{id}` - Actualizar perfil
- `GET /api/user-profiles` - Listar todos los perfiles
- `DELETE /api/user-profiles/{id}` - Eliminar perfil

### Health Check
- `GET /actuator/health` - Estado del servicio

## ??? Base de Datos

**Database:** `users_db` (PostgreSQL - Puerto 5434)

**Tablas principales:**
- `user_profiles`: Información personal de usuarios
  - Referencia a `auth_user_id` del auth-service
  - Datos personales: nombre, apellido, género, documento, teléfono
  - Información adicional: fecha nacimiento, imagen de perfil

## ?? Integración con Auth-Service

Este servicio se relaciona con `auth-service` a través del campo `authUserId`:
- `auth-service` maneja credenciales y autenticación
- `users-service` maneja información personal y perfiles

## ?? Testing

Los tests utilizan H2 en memoria para un entorno aislado.

```bash
./mvnw test
```

## ?? Desarrollo

### Estructura de Packages
- `entity/`: Entidades JPA
- `model/`: Modelos de dominio  
- `dto/`: Data Transfer Objects
- `repository/`: Repositorios de datos
- `service/`: Lógica de negocio
- `controller/`: Controladores REST
- `config/`: Configuraciones