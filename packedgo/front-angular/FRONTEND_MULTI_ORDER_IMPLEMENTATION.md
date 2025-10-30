# Frontend Multi-Order Checkout Implementation

## 📋 Resumen

Se implementó el sistema de checkout multi-admin en el frontend de Angular, permitiendo a los usuarios realizar compras de eventos de múltiples organizadores en una sola sesión de checkout.

## 🏗️ Arquitectura Implementada

### 1. **Modelos de Datos** (`src/app/shared/models/order.model.ts`)

Interfaces TypeScript que representan la estructura de datos del sistema de órdenes:

```typescript
interface MultiOrderCheckoutResponse {
  sessionId: string;
  totalAmount: number;
  sessionStatus: string; // PENDING, PARTIAL, COMPLETED
  expiresAt: string;
  totalOrders: number;
  paidOrders: number;
  totalPaid: number;
  totalPending: number;
  paymentGroups: PaymentGroup[];
  message: string;
}

interface PaymentGroup {
  adminId: number;
  orderNumber: string;
  orderId: number;
  amount: number;
  status: string; // PENDING, PAID
  paymentPreferenceId?: string;
  qrUrl?: string;
  initPoint?: string;
  items: OrderItem[];
}
```

### 2. **Servicios**

#### OrderService (`src/app/core/services/order.service.ts`)
- `checkoutMulti()`: Inicia el checkout multi-admin
- `getSessionStatus(sessionId)`: Obtiene el estado actualizado de una sesión
- `getUserOrders()`: Lista todas las órdenes del usuario
- `getOrderById(orderId)`: Obtiene una orden específica

#### PaymentService (`src/app/core/services/payment.service.ts`)
- `createPaymentPreference(adminId, orderNumber, amount)`: Crea preferencia de Mercado Pago
- `getPaymentStatus(preferenceId)`: Verifica el estado de un pago
- `verifyPaymentCallback(paymentId)`: Verifica callback de Mercado Pago

### 3. **Componentes**

#### CheckoutComponent (`src/app/features/customer/checkout/`)
**Funcionalidad principal:**
- Procesa el checkout multi-admin al inicializar
- Genera preferencias de pago para cada grupo (admin)
- Muestra items agrupados por organizador
- Presenta múltiples botones de pago (uno por admin)
- Timer de expiración de sesión (30 minutos)
- Polling automático cada 10 segundos para actualizar estados
- Redirección automática cuando todos los pagos se completan

**Características UI:**
```
┌─────────────────────────────────────────┐
│  🛒 RESUMEN DE TU COMPRA                │
│  Estado: PENDING  ⏱️ 29:45             │
├─────────────────────────────────────────┤
│  Total: $5700  |  2/2 Órdenes          │
│  Pagadas: 0/2  |  Pendiente: $5700     │
├─────────────────────────────────────────┤
│  ⚠️ Tu compra incluye eventos de 2      │
│  organizadores. Deberás completar 2     │
│  pagos separados.                       │
├─────────────────────────────────────────┤
│  📦 Organizador #1                      │
│  Orden: ORD-20250122-001                │
│  ├─ 2x Entrada Rock Fest - $2000       │
│  ├─ 3x Hamburguesa - $1500             │
│  └─ Subtotal: $3500                    │
│  [💳 PAGAR $3500] [📱 Ver QR]          │
├─────────────────────────────────────────┤
│  📦 Organizador #2                      │
│  Orden: ORD-20250122-002                │
│  ├─ 1x Tech Conference - $1500         │
│  ├─ 2x Bebida - $700                   │
│  └─ Subtotal: $2200                    │
│  [💳 PAGAR $2200] [📱 Ver QR]          │
└─────────────────────────────────────────┘
```

#### OrderSuccessComponent (`src/app/features/customer/order-success/`)
**Funcionalidad:**
- Muestra confirmación de compra exitosa
- Lista todas las órdenes completadas
- Botones para ver entradas o volver al dashboard
- Animaciones de éxito (checkmark animado)
- Opciones para compartir en redes sociales

### 4. **Flujo de Usuario Completo**

```
1. Usuario agrega items de múltiples admins al carrito
   └─> CartService.addToCart() guarda adminId

2. Usuario hace clic en "Proceder al Pago"
   └─> Router navega a /customer/checkout

3. CheckoutComponent.ngOnInit()
   ├─> OrderService.checkoutMulti()
   │   └─> Backend agrupa items por adminId
   │   └─> Backend crea MultiOrderSession + Orders
   │   └─> Backend retorna paymentGroups[]
   │
   ├─> Para cada grupo:
   │   └─> PaymentService.createPaymentPreference()
   │       └─> Genera QR y URL de Mercado Pago
   │
   ├─> Inicia timer de expiración (30 min)
   └─> Inicia polling cada 10 segundos

4. Usuario hace clic en botón "PAGAR"
   └─> Abre Mercado Pago en nueva ventana
   └─> Usuario completa pago en MP

5. Webhook de Mercado Pago notifica al backend
   └─> Backend actualiza Order.isPaid = true
   └─> Backend actualiza MultiOrderSession.status

6. Frontend polling detecta cambio
   ├─> Si status == 'PARTIAL': Actualiza UI
   └─> Si status == 'COMPLETED': 
       └─> Router.navigate('/customer/orders/success')

7. OrderSuccessComponent muestra confirmación
   └─> Lista todas las órdenes pagadas
   └─> Opciones: Ver entradas | Volver al inicio
```

## 📡 Endpoints Consumidos

### Order Service (Puerto 8084)
- **POST** `/api/orders/checkout/multi`
  - Crea sesión multi-orden
  - Headers: `Authorization: Bearer <token>`
  - Body: `{}` (userId se extrae del token)
  - Response: `MultiOrderCheckoutResponse`

- **GET** `/api/orders/sessions/{sessionId}`
  - Obtiene estado de sesión
  - Headers: `Authorization: Bearer <token>`
  - Response: `MultiOrderCheckoutResponse`

### Payment Service (Puerto 8085)
- **POST** `/api/payments/create`
  - Crea preferencia de Mercado Pago
  - Headers: `Authorization: Bearer <token>`
  - Body: `{ adminId, orderId, amount }`
  - Response: `{ preferenceId, qrUrl, initPoint }`

## 🎨 Estilos y UX

### Características visuales:
- ✅ **Responsive design** (funciona en mobile y desktop)
- ✅ **Animaciones suaves** (fadeIn, scaleIn, pulse)
- ✅ **Loading states** (spinners durante procesamiento)
- ✅ **Timer visual** con advertencia cuando quedan <5 minutos
- ✅ **Badges de estado** con colores semánticos
- ✅ **Cards con hover effects** para mejor interacción
- ✅ **Iconos Bootstrap Icons** para mejor UX

### Paleta de colores:
- **Primary**: `#007bff` (Azul - acciones principales)
- **Success**: `#28a745` (Verde - pagos completados)
- **Warning**: `#ffc107` (Amarillo - estados pendientes)
- **Danger**: `#dc3545` (Rojo - errores/expiración)

## 🔄 Polling y Estados

### Estado de Sesión:
- **PENDING**: Ningún pago completado
- **PARTIAL**: Algunos pagos completados (1 ≤ paid < total)
- **COMPLETED**: Todos los pagos completados

### Estado de Orden Individual:
- **PENDING**: Esperando pago
- **PAID**: Pago confirmado

### Polling Strategy:
```typescript
interval(10000) // Cada 10 segundos
  .pipe(switchMap(() => getSessionStatus(sessionId)))
  .subscribe(status => {
    if (status.sessionStatus === 'COMPLETED') {
      router.navigate(['/customer/orders/success']);
    }
  });
```

## ⏱️ Gestión de Tiempo

### Timer de Expiración:
- **Duración**: 30 minutos desde creación de sesión
- **Actualización**: Cada 1 segundo
- **Formato**: `MM:SS` (ej: `29:45`)
- **Advertencia visual**: Color rojo y animación pulse cuando quedan <5 minutos
- **Acción al expirar**: Detiene polling y muestra mensaje de error

```typescript
timerSubscription = interval(1000).subscribe(() => {
  const remaining = expiresAt.getTime() - Date.now();
  if (remaining <= 0) {
    stopPolling();
    errorMessage = 'La sesión de pago ha expirado';
  }
  timeRemaining = Math.floor(remaining / 1000);
});
```

## 🚀 Próximos Pasos (Mejoras Futuras)

### Fase 1: Funcionalidad Básica (COMPLETADO ✅)
- ✅ Implementar modelos de datos
- ✅ Crear OrderService y PaymentService
- ✅ Desarrollar CheckoutComponent con UI completa
- ✅ Implementar OrderSuccessComponent
- ✅ Configurar rutas y navegación
- ✅ Integrar con CartService

### Fase 2: Optimizaciones Pendientes
- [ ] **Modal para QR Codes**: Abrir QR en modal interno en lugar de nueva ventana
- [ ] **Notificaciones toast**: Usar librerías como ngx-toastr para alertas
- [ ] **Estado offline**: Detectar pérdida de conexión y pausar polling
- [ ] **Retry logic**: Reintentar llamadas fallidas con backoff exponencial
- [ ] **Caché local**: Guardar estado de sesión en localStorage
- [ ] **Progress bar**: Indicador visual del progreso de pagos (0/2, 1/2, 2/2)

### Fase 3: Características Avanzadas
- [ ] **Notificaciones push**: WebSocket para updates en tiempo real (reemplazar polling)
- [ ] **Pago rápido**: Opción "Pagar todo con un clic" si el usuario tiene tarjeta guardada
- [ ] **Historial de sesiones**: Ver sesiones anteriores y su estado
- [ ] **Compartir sesión**: Generar link para que otra persona vea el estado
- [ ] **Recordatorios por email**: Enviar email si la sesión está por expirar

### Fase 4: Analytics y Monitoreo
- [ ] **Google Analytics**: Trackear eventos de checkout
- [ ] **Error tracking**: Integrar Sentry o similar
- [ ] **Performance monitoring**: Medir tiempos de carga
- [ ] **A/B testing**: Probar diferentes UIs de checkout

## 🐛 Troubleshooting

### Problema: "Cannot find module order.model"
**Causa**: TypeScript no detecta el módulo recién creado
**Solución**: 
1. Reiniciar el servidor de desarrollo de Angular
2. Verificar que `order.model.ts` esté exportado en `shared/models/index.ts`
3. Recargar ventana en VS Code (Ctrl+Shift+P > Reload Window)

### Problema: Polling no se detiene al salir del componente
**Causa**: Subscription no se limpia en ngOnDestroy
**Solución**: Ya implementado en el código
```typescript
ngOnDestroy() {
  this.pollingSubscription?.unsubscribe();
  this.timerSubscription?.unsubscribe();
}
```

### Problema: Timer muestra valores negativos
**Causa**: La sesión ya expiró pero el timer sigue corriendo
**Solución**: Ya implementado - se detiene el timer en 0
```typescript
if (remaining <= 0) {
  this.timeRemaining = 0;
  this.stopTimer();
}
```

## 📝 Configuración de Environment

Asegúrate de que `environment.ts` tenga las URLs correctas:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost',
  authServiceUrl: 'http://localhost:8081/api',
  usersServiceUrl: 'http://localhost:8082/api',
  eventsServiceUrl: 'http://localhost:8086/api',
  ordersServiceUrl: 'http://localhost:8084/api',
  paymentsServiceUrl: 'http://localhost:8085/api', // ← NUEVO
};
```

## 🔐 Seguridad

- ✅ **JWT Authentication**: Todos los endpoints requieren token
- ✅ **CORS configurado**: Frontend puede llamar a los microservicios
- ✅ **Validación de session ownership**: Backend verifica que la sesión pertenezca al usuario
- ✅ **No se exponen datos sensibles**: Preferencias de pago se manejan en backend

## 📦 Dependencias

No se agregaron nuevas dependencias npm. Se utilizó:
- **Angular standalone components**
- **RxJS** (ya incluido)
- **Bootstrap Icons** (ya incluido en el proyecto)
- **CommonModule, RouterModule, FormsModule** (módulos de Angular)

## 🧪 Testing

### Testing manual recomendado:

1. **Caso 1: Carrito con 1 admin**
   - Agregar items de un solo admin
   - Ir a checkout
   - Verificar que se muestre 1 grupo de pago
   - Completar pago
   - Verificar redirección a success

2. **Caso 2: Carrito con múltiples admins**
   - Agregar items de 2+ admins
   - Ir a checkout
   - Verificar que se muestren N grupos (uno por admin)
   - Pagar solo el primero
   - Verificar que status sea 'PARTIAL'
   - Pagar el resto
   - Verificar redirección cuando status sea 'COMPLETED'

3. **Caso 3: Expiración de sesión**
   - Ir a checkout
   - Esperar sin pagar (o ajustar backend para expiración corta)
   - Verificar que el timer llegue a 0
   - Verificar mensaje de expiración

4. **Caso 4: Navegación durante checkout**
   - Ir a checkout
   - Navegar a otra página
   - Volver atrás
   - Verificar que polling se haya detenido correctamente

## 🎯 Conclusión

Se ha implementado exitosamente el frontend del sistema multi-order checkout con:

✅ **3 nuevos componentes** (Checkout, OrderSuccess, y modelos)
✅ **2 nuevos servicios** (OrderService, PaymentService)
✅ **UI completa y responsive** con animaciones
✅ **Polling automático** para actualización de estados
✅ **Timer de expiración** visual
✅ **Integración con Mercado Pago** (generación de QR y redirects)
✅ **Flujo completo de usuario** desde carrito hasta confirmación

El sistema está listo para testing e integración con el backend completado en la fase anterior.
