# 🔐 Guía de Seguridad - PackedGo Backend

## ⚠️ IMPORTANTE: Configuración de Credenciales

Este proyecto contiene múltiples microservicios que requieren configuración de credenciales y secretos. **NUNCA** commitees credenciales reales a Git.

## 📁 Archivos Protegidos

Los siguientes archivos están en `.gitignore` y **NO** deben ser commiteados:

- `**/.env` - Variables de entorno con secretos
- `**/database-init.sql` - Scripts SQL con credenciales de BD

## 🚀 Configuración Inicial

### 1. Clonar el Repositorio

```bash
git clone https://github.com/111858DelfinoDavid/ps-packedgo.git
cd ps-packedgo/back
```

### 2. Configurar Servicios

Cada servicio tiene un archivo `.env.example`. Cópialo y configúralo:

#### Order Service
```bash
cd order-service
cp .env.example .env
# Editar .env con tus valores
```

#### Payment Service
```bash
cd payment-service
cp .env.example .env
cp database-init.sql.example database-init.sql
# Editar ambos archivos con tus credenciales de MercadoPago
```

Ver [payment-service/SECURITY.md](payment-service/SECURITY.md) para más detalles.

#### Event Service
```bash
cd event-service
cp .env.example .env
```

### 3. Obtener Credenciales de MercadoPago

1. Ir a https://www.mercadopago.com.ar/developers/panel
2. Crear una aplicación
3. Copiar `Access Token` y `Public Key`
4. Configurar en `payment-service/database-init.sql`

### 4. Levantar los Servicios

```bash
docker-compose up -d --build
```

## 🔑 Secretos por Servicio

| Servicio | Secretos Requeridos |
|----------|-------------------|
| **payment-service** | MercadoPago Access Token, Public Key |
| **order-service** | JWT Secret, DB Password |
| **auth-service** | JWT Secret, DB Password |
| **event-service** | DB Password |
| **users-service** | DB Password |

## 🛡️ Mejores Prácticas de Seguridad

### ✅ DO (Hacer)

- ✅ Usar archivos `.env` para secretos
- ✅ Copiar `.env.example` a `.env` antes de configurar
- ✅ Rotar credenciales regularmente
- ✅ Usar credenciales diferentes para dev/prod
- ✅ Revisar `.gitignore` antes de commitear
- ✅ Usar HTTPS en producción para webhooks

### ❌ DON'T (No Hacer)

- ❌ NUNCA commitear archivos `.env`
- ❌ NUNCA hardcodear secretos en el código
- ❌ NUNCA compartir credenciales por canales inseguros
- ❌ NUNCA usar las mismas credenciales en dev y prod
- ❌ NUNCA usar credenciales de producción en sandbox

## 🔍 Verificar que NO hay Credenciales en Git

```bash
# Ver archivos trackeados que contengan "env" o "init.sql"
git ls-files | grep -E "\.env$|database-init\.sql$"

# No debería mostrar ningún resultado
```

Si ves archivos sensibles:

```bash
# Remover del índice de Git (sin borrar del disco)
git rm --cached ruta/al/archivo.env

# Commitear el cambio
git commit -m "Remove sensitive files from Git tracking"
```

## 🚨 ¿Commiteaste Credenciales Accidentalmente?

Si commiteaste credenciales reales:

1. **ROTAR INMEDIATAMENTE** las credenciales comprometidas
2. Remover del historial de Git:
   ```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch ruta/al/archivo" \
     --prune-empty --tag-name-filter cat -- --all
   ```
3. Forzar push (⚠️ cuidado con repos compartidos):
   ```bash
   git push origin --force --all
   ```

## 📚 Recursos

- [MercadoPago Developers](https://www.mercadopago.com.ar/developers)
- [12 Factor App - Config](https://12factor.net/config)
- [OWASP Secrets Management](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)

## 📝 Checklist Antes de Commitear

- [ ] Revisé que no hay archivos `.env` en staging
- [ ] Revisé que no hay `database-init.sql` en staging
- [ ] Actualicé `.env.example` si agregué nuevas variables
- [ ] Documenté nuevos secretos en este README
- [ ] Verifiqué con `git status` que solo commiteo lo necesario

## 🔐 Archivo .gitignore Global

Asegúrate de que tu `.gitignore` incluya:

```gitignore
# Environment Variables
.env
.env.local
.env.*.local
*.env

# Database init files with credentials
**/database-init.sql

# Logs que puedan contener datos sensibles
*.log
logs/
```
