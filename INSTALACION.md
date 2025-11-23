# PackedGo - Guía de Instalación Completa

Este documento proporciona instrucciones paso a paso para instalar y ejecutar el proyecto PackedGo desde cero.

## 📋 Requisitos Previos

### Software Necesario

1. **Docker Desktop** (v20.10 o superior)
   - Descargar de: https://www.docker.com/products/docker-desktop/
   - Asegúrate de que Docker Compose esté incluido

2. **Node.js** (v18 o superior) y **npm**
   - Descargar de: https://nodejs.org/
   - Verifica la instalación: `node --version` y `npm --version`

3. **Angular CLI** (v19 o superior)
   - Instalar globalmente: `npm install -g @angular/cli@latest`
   - Verifica la instalación: `ng version`

4. **Java JDK** (v17 o superior) - Solo para desarrollo local sin Docker
   - Descargar de: https://adoptium.net/

5. **Maven** (v3.8 o superior) - Solo para desarrollo local sin Docker
   - Descargar de: https://maven.apache.org/download.cgi

## 🚀 Instalación y Configuración

### 1. Clonar el Repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd ps-packedgo
```

### 2. Variables de Entorno

Los archivos `.env` ya están incluidos en el repositorio con las configuraciones necesarias para desarrollo. No necesitas copiar archivos `.env.example`.

**Servicios Backend con .env incluidos:**
- `auth-service`
- `event-service`
- `order-service`
- `payment-service`
- `users-service`
- `analytics-service`

**Configuraciones importantes en `.env`:**
- `JWT_SECRET`: Clave secreta para tokens JWT (debe ser la misma en todos los servicios)
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`: Credenciales de base de datos
- `SERVER_PORT`: Puerto del servicio
- `CORS_ORIGINS`: Orígenes permitidos para CORS

> ⚠️ **Nota**: Para producción, actualiza las credenciales y el `JWT_SECRET` con valores seguros.

### 3. Levantar Backend con Docker

Desde el directorio `/packedgo/back/`:

```bash
cd packedgo/back

# Levantar todos los servicios con Docker Compose
docker-compose up -d --build

# Verificar que los contenedores estén corriendo
docker ps

# Ver logs de un servicio específico
docker logs back-event-service-1 -f
```

**Servicios y Puertos:**
- `auth-service`: http://localhost:8080
- `event-service`: http://localhost:8086
- `order-service`: http://localhost:8082
- `payment-service`: http://localhost:8083
- `users-service`: http://localhost:8081
- `analytics-service`: http://localhost:8084

**Bases de Datos PostgreSQL:**
- `auth-db`: localhost:5433
- `event-db`: localhost:5434
- `order-db`: localhost:5435
- `user-db`: localhost:5436

### 4. Configurar y Ejecutar Frontend

Desde el directorio `/packedgo/front-angular/`:

```bash
cd packedgo/front-angular

# Instalar dependencias
npm install

# Ejecutar servidor de desarrollo
npm start
# O alternativamente:
ng serve --proxy-config proxy.conf.json
```

El frontend estará disponible en: http://localhost:3000

### 5. Verificar Instalación

1. **Backend**: Verifica que todos los servicios estén corriendo:
   ```bash
   docker ps
   ```

2. **Frontend**: Abre http://localhost:3000 en tu navegador

3. **Health Checks**: Verifica los endpoints de salud de cada servicio:
   - http://localhost:8080/actuator/health (auth-service)
   - http://localhost:8086/actuator/health (event-service)
   - etc.

## 🗄️ Inicialización de Base de Datos

Las bases de datos se crean automáticamente al levantar Docker. Los esquemas se generan mediante Hibernate al iniciar cada servicio.

### Migraciones de Base de Datos

Si necesitas ejecutar migraciones SQL manualmente, están disponibles en cada servicio:

**Event Service** (`/packedgo/back/event-service/`):
- `migration_add_location_name.sql`: Agrega campo `location_name` a eventos
- `migration_add_start_end_time.sql`: Agrega campos de hora de inicio/fin
- `migration_many_to_many.sql`: Relación muchos a muchos eventos-consumiciones
- `fix_inactive_categories_and_events.sql`: Correcciones de datos

**Ejecutar migración manualmente:**
```bash
# Conectarse a la base de datos del servicio
docker exec -it back-event-db-1 psql -U event_user -d event_db

# Ejecutar el archivo SQL
\i /path/to/migration.sql
```

## 🔧 Comandos Útiles

### Docker

```bash
# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs de un servicio específico
docker logs back-event-service-1 -f

# Reconstruir y reiniciar un servicio
docker-compose up -d --build event-service

# Detener todos los servicios
docker-compose down

# Detener y eliminar volúmenes (¡CUIDADO: elimina datos!)
docker-compose down -v

# Ver estado de contenedores
docker ps

# Ejecutar comando dentro de un contenedor
docker exec -it back-event-service-1 bash
```

### Maven (compilación local)

```bash
# Limpiar y compilar un servicio
cd packedgo/back/event-service
./mvnw clean package -DskipTests

# Ejecutar tests
./mvnw test

# Solo compilar sin empaquetar
./mvnw compile
```

### Angular

```bash
# Instalar dependencias
npm install

# Ejecutar en desarrollo
npm start

# Compilar para producción
npm run build

# Ejecutar tests
npm test

# Linting
npm run lint
```

## 🐛 Solución de Problemas

### Backend no inicia

1. Verifica que Docker Desktop esté corriendo
2. Verifica que no haya conflictos de puertos: `netstat -an | findstr "8080"`
3. Revisa logs: `docker logs back-event-service-1`
4. Reconstruye contenedores: `docker-compose down && docker-compose up -d --build`

### Frontend no compila

1. Elimina `node_modules` y reinstala:
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```
2. Verifica versión de Node: `node --version` (debe ser v18+)
3. Limpia caché de Angular: `ng cache clean`

### Errores de CORS

Verifica que `CORS_ORIGINS` en los archivos `.env` incluya `http://localhost:3000`

### Base de datos no conecta

1. Verifica que el contenedor de la BD esté corriendo: `docker ps | grep db`
2. Verifica credenciales en `.env`
3. Prueba conexión manual:
   ```bash
   docker exec -it back-event-db-1 psql -U event_user -d event_db
   ```

## 📁 Estructura del Proyecto

```
ps-packedgo/
├── packedgo/
│   ├── back/                          # Backend con microservicios
│   │   ├── docker-compose.yml         # Orquestación de contenedores
│   │   ├── auth-service/              # Servicio de autenticación
│   │   ├── event-service/             # Servicio de eventos
│   │   ├── order-service/             # Servicio de órdenes
│   │   ├── payment-service/           # Servicio de pagos
│   │   ├── users-service/             # Servicio de usuarios
│   │   └── analytics-service/         # Servicio de analíticas
│   │
│   └── front-angular/                 # Frontend Angular
│       ├── src/
│       │   ├── app/
│       │   │   ├── core/              # Servicios y guards
│       │   │   ├── features/          # Módulos de funcionalidades
│       │   │   └── shared/            # Componentes compartidos
│       │   └── environments/
│       ├── proxy.conf.json            # Configuración de proxy
│       └── package.json
│
└── README.md
```

## 🔐 Usuarios por Defecto

Los usuarios se crean automáticamente en el primer inicio:

**Administrador:**
- Email: `admin@packedgo.com`
- Password: `admin123`

**Cliente:**
- Email: `customer@packedgo.com`
- Password: `customer123`

## 📚 Documentación Adicional

- **Backend**: Ver `EMPLOYEE-SYSTEM-BACKEND-SUMMARY.md`
- **Frontend**: Ver `FRONTEND_MULTI_ORDER_IMPLEMENTATION.md`
- **Sistema QR**: Ver `SISTEMA_CANJE_QR.md`
- **Migraciones**: Archivos `migration_*.sql` en cada servicio

## 🤝 Contribuir

1. Crea una rama para tu feature: `git checkout -b feature/nueva-funcionalidad`
2. Realiza commits con mensajes descriptivos
3. Asegúrate de que todo compila y los tests pasan
4. Crea un Pull Request

## 📝 Notas Importantes

- Los archivos `.env` están incluidos en el repositorio para facilitar el desarrollo
- Para producción, actualiza las credenciales en los archivos `.env`
- Las migraciones SQL están incluidas en cada servicio para referencia
- El proyecto usa Lombok en Java - asegúrate de tener el plugin en tu IDE
- Angular usa standalone components (Angular 19+)
- Archivos `.env.example` están disponibles como respaldo y plantillas

## 🆘 Soporte

Para problemas o preguntas, crea un issue en el repositorio del proyecto.

---

**Última actualización:** 23 de noviembre de 2025
