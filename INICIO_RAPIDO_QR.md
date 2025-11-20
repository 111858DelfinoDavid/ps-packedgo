# 🚀 INICIO RÁPIDO - Sistema de Canje QR

## ✅ El Sistema YA ESTÁ IMPLEMENTADO

**Todo está listo para usar. No necesitas programar nada más.**

---

## 🎯 Para Empezar (3 pasos)

### 1️⃣ Acceder al Dashboard del Empleado
```
http://localhost:3000/employee/dashboard
```

### 2️⃣ Iniciar Sesión
Si el empleado `sasha@test.com` ya existe:
```
Email: sasha@test.com
Password: password123
```

Si no existe, créalo desde:
```
http://localhost:3000/admin/employee-management
```

### 3️⃣ Empezar a Escanear
1. Selecciona el evento (ej: "Nina Kraviz")
2. Click en "Escanear Ticket de Entrada"
3. Apunta la cámara al QR del cliente
4. ¡Listo! El sistema hace todo automáticamente

---

## 📱 ¿Cómo Funciona?

### **Primer Escaneo: ENTRADA** ✅
```
Cliente llega → Escanear QR → ✅ "Entrada autorizada"
```
- ✅ Solo puede entrar UNA VEZ
- ❌ Si intenta volver a entrar: "Entrada ya utilizada"

### **Siguientes Escaneos: CONSUMICIONES** 🍺
```
Cliente pide cerveza → Escanear QR → Seleccionar "Coca Cola" → Canjear 1
```
- ✅ Puede volver MÚLTIPLES VECES
- ✅ Canjea de a poco hasta agotar
- ✅ Sistema controla stock automáticamente

---

## 🎨 Lo Que Verás en Pantalla

### Pantalla Principal
```
┌─────────────────────────────────────┐
│ 👤 Panel de Empleado                │
│    sasha@test.com      🕐 15:30:42  │
└─────────────────────────────────────┘

📅 Eventos Asignados:
[✓ Nina Kraviz] [  Otro Evento  ]

📊 Estadísticas:
🎫 15 Tickets | 🍺 23 Consumos | 📈 38 Total

┌─────────────────────────────────────┐
│  📷 Escanear Ticket de Entrada      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  🍔 Escanear Consumo                │
└─────────────────────────────────────┘

🕐 Historial:
✅ Entrada autorizada - Usuario 3 - 15:28:15
✅ Coca Cola canjeada - Restante: 1 - 15:29:42
```

### Cuando Escaneas Entrada
```
✅ ¡Entrada autorizada!

Cliente: Usuario 3
Pass: VIP_PASS
```

### Cuando Escaneas Consumición
```
Selecciona qué canjear:

🍺 Coca Cola 500ml - Disponible: 2
🍔 Hamburguesa Completa - Disponible: 1
🍟 Papas Fritas - Disponible: 1

[Click en la que el cliente pide]
```

Luego te pregunta:
```
¿Cuántas unidades canjear?

Disponible: 2
Cantidad: [1] ⬆️⬇️

[Canjear] [Cancelar]
```

Y muestra:
```
✅ ¡Consumición canjeada!

Coca Cola 500ml
Cantidad: 1
Restante: 1
```

---

## 🎯 Ejemplos de Uso Real

### Ejemplo 1: Cliente llega y consume todo
```
15:00 → Escanea entrada → ✅ "Autorizado"
15:30 → Escanea consumo → Coca Cola x1 → ✅ "Restante: 1"
16:00 → Escanea consumo → Coca Cola x1 → ✅ "Canjeado"
16:30 → Escanea consumo → Hamburguesa x1 → ✅ "Canjeado"
```

### Ejemplo 2: Cliente intenta entrar dos veces
```
15:00 → Escanea entrada → ✅ "Autorizado"
15:30 → Sale del evento
16:00 → Intenta volver → Escanea entrada → ❌ "Ya utilizada el 20/11/2025 15:00"
```

### Ejemplo 3: Cliente sin consumiciones
```
15:00 → Escanea entrada → ✅ "Autorizado"
15:30 → Escanea consumo → ⚠️ "Sin consumiciones disponibles"
```

---

## 🔧 Requisitos Técnicos

### ✅ Ya Configurado (No tocar)
- Backend: Servicios corriendo en Docker
- Base de datos: PostgreSQL configurada
- Frontend: Angular corriendo en port 3000
- Scanner QR: ZXing integrado

### 📱 Requisitos del Navegador
- ✅ Chrome/Edge/Firefox/Safari
- ✅ Permitir acceso a cámara (popup la primera vez)
- ✅ Conexión a internet

---

## 🎓 Capacitación de Empleados (5 minutos)

### **Paso 1: Mostrar cómo entrar**
```
1. Abrir: http://localhost:3000/employee/login
2. Ingresar email y contraseña
3. Ver el dashboard
```

### **Paso 2: Explicar los 2 botones**
```
"Escanear Ticket de Entrada" → Primera vez que cliente llega
"Escanear Consumo" → Cuando cliente pide comida/bebida
```

### **Paso 3: Hacer una prueba**
```
1. Click en "Escanear Entrada"
2. Mostrar un QR de prueba
3. Ver mensaje de éxito
4. Hacer lo mismo con "Escanear Consumo"
```

### **Paso 4: Explicar reglas**
```
✅ Entrada: Solo 1 vez
✅ Consumiciones: Muchas veces hasta agotar
✅ Verde = OK, Rojo = Error
✅ Historial abajo muestra todo
```

---

## 🆘 Solución de Problemas

### "No puedo escanear"
✅ Verificar que diste permiso a la cámara
✅ Recargar la página (F5)
✅ Probar con otro navegador

### "Entrada ya utilizada"
✅ Es normal, el cliente ya entró
✅ No puede volver a entrar con el mismo ticket
✅ Explicárselo al cliente

### "Sin consumiciones"
✅ Cliente ya canjeó todo
✅ O su pase no incluía consumiciones
✅ Verificar en historial si ya las canjeó

### "QR no se reconoce"
✅ Acercar o alejar el QR
✅ Mejor iluminación
✅ Mantener estable
✅ Verificar que el QR esté completo en pantalla

---

## 📊 Monitoreo

### Dashboard del Admin
```
http://localhost:3000/admin/dashboard
```
Ver estadísticas totales del sistema

### Gestión de Empleados
```
http://localhost:3000/admin/employee-management
```
Crear, editar, asignar eventos

---

## 📝 Documentación Completa

Si necesitas más detalles técnicos:

1. **SISTEMA_CANJE_QR.md** → Documentación técnica completa
2. **GUIA_USO_SISTEMA_QR.md** → Guía detallada para empleados
3. **SISTEMA_QR_VISTA_RAPIDA.md** → Referencia rápida
4. **RESUMEN_IMPLEMENTACION_QR.md** → Resumen ejecutivo

---

## ✨ Características Principales

✅ **Fácil**: Solo 2 botones, todo automático
✅ **Rápido**: Escaneo en 2 segundos
✅ **Seguro**: Imposible hacer trampa
✅ **Visual**: Colores indican estado
✅ **Trazable**: Historial completo
✅ **Flexible**: Cliente canjea a su ritmo

---

## 🎉 ¡Listo para Usar!

El sistema está completamente funcional. Solo necesitas:

1. ✅ Crear empleados (si no existen)
2. ✅ Asignar eventos a empleados
3. ✅ Dar credenciales a empleados
4. ✅ Empleados abren navegador y empiezan a escanear

**¡Así de simple!** 🚀

---

## 🔗 Links Rápidos

| Función | URL |
|---------|-----|
| **Login Empleado** | http://localhost:3000/employee/login |
| **Dashboard Empleado** | http://localhost:3000/employee/dashboard |
| **Gestión Empleados** | http://localhost:3000/admin/employee-management |
| **Analytics** | http://localhost:3000/admin/analytics |

---

**¿Dudas?** Lee la documentación completa en los archivos .md del proyecto.

**¿Problemas?** Revisa la sección "Solución de Problemas" arriba.

**¿Todo OK?** ¡Empieza a escanear! 🎫🍺✨
