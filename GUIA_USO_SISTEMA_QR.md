# 🎯 Guía Rápida: Sistema de Canje QR - PackedGo

## ✅ Estado del Sistema

El sistema de canje de QR está **100% implementado y funcional**. Incluye:

1. ✅ **Validación de Entrada** (primer escaneo obligatorio)
2. ✅ **Canje Progresivo de Consumiciones** (uno por uno hasta agotar stock)
3. ✅ **Dashboard de Empleado** con escáner QR en tiempo real
4. ✅ **Historial de Escaneos** con feedback visual
5. ✅ **Estadísticas en Tiempo Real**

---

## 🚀 Cómo Usar el Sistema

### 📱 Acceso al Dashboard del Empleado

1. **Abrir navegador** y acceder a:
   ```
   http://localhost:3000/employee/login
   ```

2. **Iniciar sesión** con las credenciales del empleado:
   ```
   Email: sasha@test.com
   Password: password123
   ```

### 🎫 Flujo de Trabajo del Empleado

#### **Paso 1: Seleccionar Evento**
- Al entrar al dashboard, se muestra lista de eventos asignados
- Click en el evento donde estás trabajando (ej: "Nina Kraviz")
- El evento seleccionado se marca con ✅

#### **Paso 2: Validar Entrada del Cliente**
1. Cliente llega al evento con su QR
2. Empleado hace click en **"Escanear Ticket de Entrada"**
3. Apuntar cámara al QR del cliente
4. Sistema valida automáticamente:
   - ✅ **Si es válido**: Muestra "¡Entrada autorizada!" con datos del cliente
   - ❌ **Si ya fue usado**: Muestra "Entrada ya utilizada el [fecha/hora]"
   - ❌ **Si es inválido**: Muestra error específico

> 💡 **Importante**: La entrada se marca como usada y no puede volver a entrar

#### **Paso 3: Canjear Consumiciones (Progresivo)**
1. Cliente pide canjear una consumición (ej: cerveza)
2. Empleado hace click en **"Escanear Consumo"**
3. Apuntar cámara al mismo QR del cliente
4. Sistema muestra lista de consumiciones disponibles:
   ```
   🍺 Coca Cola 500ml - Disponible: 2
   🍔 Hamburguesa Completa - Disponible: 1
   🍟 Papas Fritas - Disponible: 1
   ```
5. Empleado selecciona la consumición que el cliente solicita
6. Sistema pregunta cuántas unidades canjear (ej: 1 de 2)
7. Confirmar el canje
8. ✅ Sistema muestra: "¡Consumición canjeada! Restante: 1"

> 💡 **El cliente puede volver múltiples veces** a canjear sus consumiciones progresivamente hasta agotarlas

#### **Paso 4: Repetir para Más Clientes**
- Una vez terminado con un cliente, puedes escanear el siguiente
- El historial se guarda automáticamente en la parte inferior

---

## 📊 Características del Dashboard

### **Estadísticas en Vivo**
```
┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ 🎫 15       │ │ 🍺 23       │ │ 📈 38       │
│ Tickets     │ │ Consumos    │ │ Total       │
└─────────────┘ └─────────────┘ └─────────────┘
```

### **Historial de Escaneos**
Cada operación queda registrada con:
- ✅ Icono de estado (success/error)
- 📝 Tipo de operación (Ticket/Consumo)
- 💬 Mensaje descriptivo
- 📅 Evento asociado
- 🕐 Hora exacta del escaneo

---

## 🎯 Casos de Uso Reales

### ✅ **Caso 1: Cliente Normal**
```
15:00 - Cliente llega
     → Escanear entrada → ✅ "Entrada autorizada"

15:30 - Cliente pide cerveza
     → Escanear consumo → Seleccionar "Coca Cola" → Canjear 1 → ✅ "Restante: 1"

16:15 - Cliente pide otra cerveza
     → Escanear consumo → Seleccionar "Coca Cola" → Canjear 1 → ✅ "Totalmente canjeado"

17:00 - Cliente pide hamburguesa
     → Escanear consumo → Seleccionar "Hamburguesa" → Canjear 1 → ✅ "Canjeado"
```

### ❌ **Caso 2: Cliente Intenta Entrar Dos Veces**
```
15:00 - Primera entrada
     → ✅ "Entrada autorizada"

15:30 - Cliente sale y vuelve a intentar entrar
     → ❌ "Entrada ya utilizada el 20/11/2025 15:00"
```

### ⚠️ **Caso 3: Cliente Sin Consumiciones Restantes**
```
Cliente ya canjeó todas sus consumiciones
     → Escanear consumo → ⚠️ "Sin consumiciones disponibles"
```

---

## 🔐 Seguridad y Validaciones

### **Protecciones Implementadas**

1. **Autenticación Obligatoria**
   - Solo empleados con cuenta pueden acceder
   - Token JWT en cada operación

2. **Autorización por Evento**
   - Empleado solo ve eventos asignados
   - No puede operar en eventos de otros empleados

3. **Validación de QR**
   - Formato estricto verificado
   - Evento del QR debe coincidir con evento seleccionado

4. **Prevención de Reutilización**
   - Tickets: Una sola entrada por QR
   - Consumiciones: Control de cantidad exacto

5. **Transaccionalidad**
   - Operaciones atómicas en BD
   - No hay posibilidad de canjes duplicados

---

## 📱 Requisitos del Navegador

### **Permisos Necesarios**
- ✅ Acceso a cámara (para escanear QR)
- ✅ JavaScript habilitado
- ✅ Conexión a internet

### **Navegadores Compatibles**
- ✅ Google Chrome (recomendado)
- ✅ Microsoft Edge
- ✅ Firefox
- ✅ Safari (iOS 11+)

> 💡 La primera vez que uses el escáner, el navegador pedirá permiso para acceder a la cámara. Debes aceptar.

---

## 🎓 Capacitación del Empleado

### **Puntos Clave a Explicar**

1. **Primer escaneo es la entrada** - Solo se puede usar una vez
2. **Mismo QR para todo** - Entrada y consumiciones usan el mismo código
3. **Canje progresivo** - Cliente puede volver varias veces a canjear
4. **Verificar evento** - Asegurarse de estar en el evento correcto
5. **Feedback visual** - Verde = éxito, Rojo = error, Amarillo = advertencia

### **Simulacro de Práctica**

1. Login con credenciales de prueba
2. Seleccionar evento "Nina Kraviz"
3. Escanear QR de prueba (puedes generarlo desde el sistema de admin)
4. Practicar validar entrada
5. Practicar canjear consumiciones múltiples veces

---

## 🛠️ Troubleshooting

### **Problema: "Cámara no funciona"**
- ✅ Verificar permisos del navegador
- ✅ Recargar página (F5)
- ✅ Probar con otro navegador
- ✅ Verificar que no hay otra app usando la cámara

### **Problema: "QR no se reconoce"**
- ✅ Asegurar buena iluminación
- ✅ Mantener el QR estable
- ✅ Verificar que el QR está completo en la pantalla
- ✅ Acercar o alejar el dispositivo

### **Problema: "Entrada ya utilizada"**
- ✅ **Es normal** - El sistema previene doble entrada
- ✅ Explicar al cliente que ya ingresó anteriormente
- ✅ Si el cliente insiste, contactar supervisor

### **Problema: "Sin consumiciones disponibles"**
- ✅ Verificar que el cliente tenga consumiciones en su pase
- ✅ Confirmar que no las haya canjeado todas ya
- ✅ Ver historial de escaneos para verificar

---

## 📞 Soporte

### **Para Empleados**
- Consultar con supervisor del evento
- Revisar historial de escaneos en el dashboard

### **Para Administradores**
- Revisar logs en `/admin/analytics`
- Consultar empleados en `/admin/employee-management`
- Ver base de datos directamente si es necesario

---

## 🎉 ¡Listo para Usar!

El sistema está completamente funcional y listo para producción. Solo necesitas:

1. ✅ Crear empleados en `/admin/employee-management`
2. ✅ Asignar eventos a cada empleado
3. ✅ Dar credenciales a los empleados
4. ✅ Los empleados acceden a `/employee/login`
5. ✅ ¡Empezar a escanear!

---

**Desarrollado con ❤️ para PackedGo**
*Versión 1.0 - Noviembre 2025*
