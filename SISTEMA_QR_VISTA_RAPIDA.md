# 📱 Sistema de Canje QR - Vista Rápida

## 🎯 Acceso Directo
```
Dashboard Empleado: http://localhost:3000/employee/login
Credenciales prueba: sasha@test.com / password123
```

## 🔄 Flujo Completo en 3 Pasos

```
┌─────────────────────────────────────────────────────────┐
│  PASO 1: VALIDAR ENTRADA (Una sola vez)                │
└─────────────────────────────────────────────────────────┘
Cliente llega al evento
    ↓
Empleado escanea QR del ticket
    ↓
Sistema valida:
  ✅ QR formato correcto
  ✅ Ticket pertenece al evento
  ✅ Ticket NO usado anteriormente
    ↓
✅ ENTRADA AUTORIZADA
   (ticket.redeemed = true)
    ↓
❌ Cliente NO puede volver a entrar


┌─────────────────────────────────────────────────────────┐
│  PASO 2: CANJEAR CONSUMICIONES (Múltiples veces)       │
└─────────────────────────────────────────────────────────┘
Cliente pide canjear consumición
    ↓
Empleado escanea el MISMO QR
    ↓
Sistema muestra lista:
  🍺 Coca Cola 500ml - Disponible: 2
  🍔 Hamburguesa - Disponible: 1
  🍟 Papas Fritas - Disponible: 1
    ↓
Empleado selecciona "Coca Cola"
    ↓
Sistema pregunta cantidad: ¿Cuántas? [1]
    ↓
✅ CONSUMICIÓN CANJEADA
   Cantidad: 1
   Restante: 1
    ↓
Cliente puede volver más tarde a canjear la restante


┌─────────────────────────────────────────────────────────┐
│  PASO 3: REPETIR PASO 2 hasta agotar consumiciones     │
└─────────────────────────────────────────────────────────┘
Cliente vuelve y pide otra Coca Cola
    ↓
Empleado escanea QR nuevamente
    ↓
Selecciona "Coca Cola"
    ↓
Canjea: 1 (última unidad)
    ↓
✅ TOTALMENTE CANJEADO
   Coca Cola: 0 restantes
    ↓
Ahora solo le quedan:
  🍔 Hamburguesa - Disponible: 1
  🍟 Papas Fritas - Disponible: 1
```

## 🎨 Interfaz del Dashboard

```
╔═══════════════════════════════════════════════════════╗
║  👤 Panel de Empleado          🕐 15:30:42  [Salir]  ║
║     sasha@test.com                                    ║
╚═══════════════════════════════════════════════════════╝

📅 Selecciona el evento:
┌──────────────────┐ ┌──────────────────┐
│ ✓ Nina Kraviz    │ │   Otro Evento    │
│ 20/11/2025       │ │ 25/11/2025       │
│ Club Groove      │ │ Arena Norte      │
└──────────────────┘ └──────────────────┘
       (activo)             (inactivo)

📊 Estadísticas de Hoy
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   🎫 15      │ │   🍺 23      │ │   📈 38      │
│  Tickets     │ │  Consumos    │ │   Total      │
└──────────────┘ └──────────────┘ └──────────────┘

┌────────────────────────────────────────────────────┐
│  📷 Escanear Ticket de Entrada                    │
│     Validar entrada al evento                     │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│  🍔 Escanear Consumo                              │
│     Registrar consumo del cliente                 │
└────────────────────────────────────────────────────┘

───────────────────────────────────────────────────────
🕐 Historial de Escaneos                   [Limpiar]

┌────────────────────────────────────────────────────┐
│ 🎫 Ticket de Entrada                          ✅  │
│    ✅ Entrada autorizada                          │
│    Cliente: Usuario 3 | Pass: VIP_PASS            │
│    📅 Nina Kraviz                                  │
│    🕐 15:28:15                                     │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ 🍺 Consumo                                    ✅  │
│    Coca Cola 500ml - Canjeado 1, Restante: 1      │
│    📅 Nina Kraviz                                  │
│    🕐 15:29:42                                     │
└────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│ 🍺 Consumo                                    ✅  │
│    Coca Cola 500ml - Totalmente canjeado          │
│    📅 Nina Kraviz                                  │
│    🕐 16:15:08                                     │
└────────────────────────────────────────────────────┘
```

## 📊 Estados del Sistema

### ✅ Operaciones Exitosas
```
Entrada Autorizada
├── ✅ QR válido
├── ✅ Evento correcto
├── ✅ Primera vez que entra
└── 🔒 Ticket marcado como usado (no puede volver a entrar)

Consumición Canjeada
├── ✅ QR válido
├── ✅ Consumición disponible
├── ✅ Cantidad suficiente
└── 📉 Cantidad decrementada (puede canjear más tarde si quedan)
```

### ❌ Operaciones Rechazadas
```
Entrada Denegada
├── ❌ Ticket ya fue usado
├── ❌ QR inválido o corrupto
├── ❌ Evento incorrecto
└── ❌ Ticket inactivo

Consumición Denegada
├── ❌ Sin consumiciones disponibles
├── ❌ Cantidad insuficiente
├── ❌ Consumición ya totalmente canjeada
└── ❌ QR inválido
```

## 🎯 Diferencia Clave: Entrada vs Consumición

| Característica | 🎫 Entrada | 🍺 Consumición |
|----------------|-----------|---------------|
| **Escaneos permitidos** | ❌ Solo 1 vez | ✅ Múltiples veces |
| **Estado después del canje** | 🔒 Bloqueado permanente | 🔄 Disponible si quedan unidades |
| **Campo en BD** | `redeemed = true` | `quantity` se decrementa |
| **Puede volver?** | ❌ No puede reingresar | ✅ Sí, hasta agotar stock |
| **Ejemplo** | Entró a las 15:00, no puede volver | Canjeó 1 cerveza, le quedan 2 más |

## 🔐 Formato del QR

```
Entrada:
PACKEDGO|T:1|E:1|U:3|TS:1732140000000
         ↑   ↑  ↑  ↑
         │   │  │  └─ Timestamp
         │   │  └──── User ID
         │   └─────── Event ID (debe coincidir)
         └─────────── Ticket ID

Consumición (mismo QR):
PACKEDGO|T:1|TC:5|E:1|U:3|TS:1732140000000
         ↑   ↑    ↑  ↑  ↑
         │   │    │  │  └─ Timestamp
         │   │    │  └──── User ID
         │   │    └─────── Event ID
         │   └──────────── TicketConsumption ID
         └──────────────── Ticket ID
```

## 🚀 Quick Start para Empleados

1. **Abrir**: `http://localhost:3000/employee/login`
2. **Login**: `sasha@test.com` / `password123`
3. **Seleccionar**: Evento "Nina Kraviz"
4. **Primera vez con cliente**: Escanear entrada
5. **Cuando cliente pida consumición**: Escanear consumo → Seleccionar producto → Confirmar cantidad
6. **Repetir**: Cliente puede volver múltiples veces a canjear

## 📝 Checklist Pre-Evento

- [ ] Verificar login funciona
- [ ] Verificar cámara del dispositivo funciona
- [ ] Verificar conexión a internet
- [ ] Probar escanear un QR de prueba
- [ ] Familiarizarse con la interfaz
- [ ] Tener claro: 1 entrada, N consumiciones progresivas

## ✨ Ventajas del Sistema

✅ **Simple**: Solo 2 botones principales
✅ **Rápido**: Escaneo en menos de 2 segundos
✅ **Seguro**: Imposible hacer trampa o duplicar
✅ **Flexible**: Cliente canjea a su ritmo
✅ **Trazable**: Historial completo de operaciones
✅ **Visual**: Feedback inmediato con colores

---

**¿Listo para empezar?** 🚀
Solo abre el navegador y escanea → ¡Así de fácil!
