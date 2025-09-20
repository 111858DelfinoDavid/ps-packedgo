# Auth Service - PackedGo

Microservicio de autenticación para la plataforma PackedGo.

## ?? Configuración Inicial

### 1. Variables de Entorno

Copia el archivo de ejemplo y configura tus variables:

```bash
cp .env.example .env
```

Luego edita el archivo `.env` con tus credenciales reales:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://db:5432/auth_db
DATABASE_USER=tu_usuario_db
DATABASE_PASSWORD=tu_contraseña_db

# JWT Configuration
JWT_SECRET=tu_clave_secreta_jwt_muy_segura

# Server Configuration
SERVER_PORT=8081
```

### 2. Ejecutar con Docker

```bash
# Construir y ejecutar todos los servicios
docker compose up -d

# Ver logs
docker compose logs -f auth-service

# Parar servicios
docker compose down

# Parar servicios y eliminar volúmenes
docker compose down --volumes
```

### 3. Ejecutar en desarrollo local

Si quieres ejecutar la aplicación sin Docker:

1. Configura una base de datos PostgreSQL local
2. Actualiza las variables de entorno en `.env`
3. Ejecuta: `./mvnw spring-boot:run`

## ?? API Endpoints

### Autenticación de Administradores
- `POST /api/v1/auth/admin/login` - Login de admin
- `POST /api/v1/auth/admin/register` - Registro de admin

### Autenticación de Customers
- `POST /api/v1/auth/customer/login` - Login de customer
- `POST /api/v1/auth/customer/register` - Registro de customer

### Utilidades
- `GET /api/v1/users/admin/exists/{email}` - Verificar si existe admin
- `GET /api/v1/users/customer/exists/{email}` - Verificar si existe customer

## ?? Seguridad

- Las credenciales están configuradas en variables de entorno
- El archivo `.env` está excluido del repositorio por seguridad
- Usa JWT para autenticación de tokens

## ?? Testing

Usa la colección de Postman incluida para probar los endpoints.