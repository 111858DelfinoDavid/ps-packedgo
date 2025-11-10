# ✅ SOLUCIÓN: Error al cargar categorías

## 🐛 Problema
El frontend mostraba: **"Error al cargar categorías. Por favor, intenta nuevamente."**

## 🔍 Diagnóstico
1. ✅ Event Service corriendo correctamente en puerto 8086
2. ✅ Endpoints configurados correctamente: `/api/event-service/category/active`
3. ❌ **No existían categorías en la base de datos**

## ✅ Solución Aplicada

### Se insertaron categorías por defecto en la base de datos:

#### Categorías de Eventos:
```sql
INSERT INTO event_categories (name, active, created_by) VALUES 
    ('Música', true, 1),
    ('Deportes', true, 1),
    ('Teatro', true, 1),
    ('Conferencias', true, 1),
    ('Festivales', true, 1),
    ('Otros', true, 1);
```

#### Categorías de Consumición:
```sql
INSERT INTO consumption_categories (name, active, created_by) VALUES 
    ('Bebidas', true, 1),
    ('Comida', true, 1),
    ('Snacks', true, 1),
    ('Bebidas Alcohólicas', true, 1),
    ('Otros', true, 1);
```

---

## 🎯 Resultado

### ✅ Endpoints Funcionando:

**Categorías de Eventos (Activas):**
```
GET http://localhost:8086/api/event-service/category/active
```

**Respuesta:**
```json
[
  { "id": 1, "name": "Música", "createdBy": 1 },
  { "id": 2, "name": "Deportes", "createdBy": 1 },
  { "id": 3, "name": "Teatro", "createdBy": 1 },
  { "id": 4, "name": "Conferencias", "createdBy": 1 },
  { "id": 5, "name": "Festivales", "createdBy": 1 },
  { "id": 6, "name": "Otros", "createdBy": 1 }
]
```

**Categorías de Consumición (Activas):**
```
GET http://localhost:8086/api/event-service/consumption-category/active
```

---

## 🛠️ Script de Inicialización

Se creó el script `init-default-data.ps1` para inicializar datos por defecto automáticamente.

**Uso:**
```powershell
.\init-default-data.ps1
```

Este script:
- ✅ Verifica que Event DB esté corriendo
- ✅ Crea categorías de eventos si no existen
- ✅ Crea categorías de consumición si no existen
- ✅ Muestra las categorías creadas
- ✅ Lista los endpoints disponibles

---

## 🔄 Si necesitas resetear o agregar más categorías:

### Ver categorías actuales:
```powershell
docker exec back-event-db-1 psql -U event_user -d event_db -c "SELECT * FROM event_categories;"
```

### Agregar nueva categoría:
```powershell
docker exec back-event-db-1 psql -U event_user -d event_db -c "INSERT INTO event_categories (name, active, created_by) VALUES ('Nueva Categoría', true, 1);"
```

### Eliminar todas las categorías:
```powershell
docker exec back-event-db-1 psql -U event_user -d event_db -c "TRUNCATE TABLE event_categories RESTART IDENTITY CASCADE;"
```

---

## 📝 Notas Importantes

1. **created_by = 1**: Las categorías están asociadas al primer admin creado
2. **active = true**: Todas las categorías están activas por defecto
3. El frontend debería usar el endpoint `/api/event-service/category/active` para obtener solo categorías activas

---

**✅ Problema resuelto - El frontend ahora puede cargar las categorías correctamente**
