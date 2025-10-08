# PackedGo
## Plataforma de Gestión de Eventos con Sistema de Consumiciones Prepagadas
### Documento de Arquitectura y Especificaciones Técnicas

---

**Trabajo Final Integrador**  
**Tecnicatura Universitaria en Programación**  
**Universidad Tecnológica Nacional - Facultad Regional Córdoba**

**Estudiantes:**
- Delfino, David Elías - Legajo: 111858
- Luparia Mothe, Agustín - Legajo: 113973

**Año:** 2025

---

## Resumen Ejecutivo

PackedGo es una plataforma web integral desarrollada bajo una arquitectura de microservicios que revoluciona la gestión y venta de entradas para eventos mediante un innovador sistema de consumiciones prepagadas. La plataforma permite a los usuarios construir paquetes personalizados combinando entradas con consumiciones, realizar pagos seguros a través de MercadoPago, y gestionar el acceso y consumo durante el evento mediante códigos QR únicos.

El sistema está diseñado para servir a dos tipos de usuarios principales: administradores que gestionan eventos, controlan stock, validan entradas y generan reportes; y consumidores que pueden explorar eventos, crear paquetes personalizados, realizar pagos y disfrutar de una experiencia digitalizada durante el evento.

La arquitectura de microservicios implementada garantiza escalabilidad, mantenibilidad y alta disponibilidad, utilizando tecnologías modernas como Spring Boot, Angular, PostgreSQL y Docker, posicionando a PackedGo como una solución robusta y profesional para la industria de eventos.

---

## 1. Introducción y Objetivos

### 1.1 Problemática

La industria de eventos enfrenta múltiples desafíos operativos que impactan tanto en la experiencia del usuario como en la eficiencia administrativa:

- **Gestión fragmentada:** Los sistemas actuales manejan la venta de entradas y la gestión de consumiciones de forma separada, generando incongruencias y pérdida de control operativo.
- **Experiencia del usuario limitada:** Los asistentes deben realizar múltiples transacciones durante el evento, creando colas y demoras que afectan negativamente su experiencia.
- **Control de stock deficiente:** La falta de sincronización en tiempo real entre ventas y consumo genera problemas de sobre-venta y desabastecimiento.
- **Complejidad en la validación:** Los métodos tradicionales de validación de entradas son lentos y propensos a errores humanos.
- **Ausencia de datos unificados:** La información de ventas, consumo y asistencia se encuentra dispersa, dificultando la toma de decisiones basada en datos.

### 1.2 Objetivos Principales

**Objetivo General:**
Desarrollar una plataforma web integral basada en microservicios que unifique la gestión de eventos con un sistema de consumiciones prepagadas, optimizando tanto la experiencia del usuario como la eficiencia operativa.

**Objetivos Específicos:**

1. **Implementar un sistema de paquetes personalizables** que permita a los usuarios combinar entradas con consumiciones según sus preferencias.

2. **Desarrollar un sistema de autenticación diferenciado** que distinga entre administradores (acceso con email) y consumidores (acceso con DNI).

3. **Integrar un sistema de pagos seguro** mediante MercadoPago que garantice transacciones confiables y trazables.

4. **Crear un sistema de validación digital** basado en códigos QR para agilizar el acceso a eventos y el canje de consumiciones.

5. **Implementar un panel de analytics** que proporcione métricas en tiempo real sobre ventas, asistencia y consumo.

6. **Garantizar la escalabilidad del sistema** mediante una arquitectura de microservicios que permita crecimiento independiente de cada componente.

### 1.3 Alcance del Proyecto

**Incluye:**
- Registro y autenticación diferenciada de usuarios (Admin/Cliente)
- Gestión completa de eventos y consumiciones
- Constructor de paquetes personalizados con carrito de compras
- Integración completa con MercadoPago para procesamiento de pagos
- Sistema de códigos QR para validación de entrada y canje de consumiciones
- Panel administrativo con métricas y reportes
- API Gateway para gestión centralizada de requests
- Base de datos distribuida con PostgreSQL
- Aplicación frontend responsive en Angular

**No Incluye:**
- Aplicación móvil nativa (solo web responsive)
- Integración con otros procesadores de pago además de MercadoPago
- Sistema de notificaciones push
- Funcionalidades de redes sociales
- Sistema de reviews o calificaciones
- Gestión de proveedores externos

### 1.4 Usuarios Objetivo

**Administradores de Eventos:**
- Organizadores de eventos corporativos, sociales y culturales
- Gestores de bares, restaurantes y venues
- Empresas de catering y servicios de eventos

**Consumidores Finales:**
- Asistentes a eventos de 18-65 años
- Usuarios familiarizados con tecnología digital
- Personas que valoran la experiencia personalizada y la conveniencia

---

## 2. Arquitectura General del Sistema

### 2.1 Filosofía de Microservicios

PackedGo adopta una arquitectura de microservicios fundamentada en los principios de Domain-Driven Design (DDD), donde cada servicio representa un dominio de negocio bien definido y encapsulado. Esta decisión arquitectónica se basa en los siguientes pilares:

**Separación de Responsabilidades:**
Cada microservicio maneja un aspecto específico del negocio (autenticación, eventos, pagos, etc.), permitiendo que los equipos de desarrollo trabajen de forma independiente y especializada.

**Escalabilidad Independent:**
Los servicios pueden escalarse individualmente según la demanda específica. Por ejemplo, el servicio de pagos puede requerir más recursos durante picos de venta.

**Tolerancia a Fallos:**
El fallo de un servicio no compromete la disponibilidad completa del sistema, mejorando la resilencia general de la plataforma.

**Tecnología Agnóstica:**
Aunque actualmente todos los servicios utilizan Java/Spring Boot, la arquitectura permite adoptar diferentes tecnologías según las necesidades específicas de cada dominio.

### 2.2 Justificación de la Elección Arquitectónica

La arquitectura de microservicios se justifica por:

1. **Complejidad del Dominio:** PackedGo maneja múltiples dominios complejos (autenticación, eventos, pagos, QR) que se benefician de la separación.

2. **Requisitos de Escalabilidad:** Diferentes componentes del sistema experimentan cargas variables (ej: picos en pagos durante lanzamiento de eventos).

3. **Mantenibilidad:** Facilita el mantenimiento y evolución del sistema, permitiendo actualizaciones independientes.

4. **Demostración Académica:** Evidencia dominio de patrones arquitectónicos avanzados requeridos en el desarrollo profesional.

### 2.3 Patrones de Comunicación

**Comunicación Síncrona (HTTP/REST):**
- Validación de tokens de autenticación
- Consultas de datos en tiempo real
- Operaciones que requieren respuesta inmediata

**Comunicación Asíncrona (Events):**
- Actualizaciones de métricas y analytics
- Notificaciones de cambios de estado
- Procesamiento de webhooks de MercadoPago

**Database per Service:**
- Cada microservicio mantiene su propia base de datos
- Garantiza el desacoplamiento total entre servicios
- Evita dependencias de esquema entre equipos

---

## 3. Stack Tecnológico

### 3.1 Backend

**Java 21**
- Versión LTS más reciente con mejoras en performance y características modernas
- Soporte nativo para patrones modernos de desarrollo
- Ecosistema maduro y estable para aplicaciones empresariales

**Spring Boot 3.2**
- Framework principal para desarrollo de microservicios
- Auto-configuración que reduce significativamente el boilerplate
- Integración nativa con herramientas de monitoreo y observabilidad
- Amplio ecosistema de starters para funcionalidades específicas

**Spring Security 6**
- Gestión robusta de autenticación y autorización
- Soporte nativo para JWT y OAuth2
- Configuración flexible para diferentes tipos de usuarios

**Spring Data JPA / Hibernate**
- ORM robusto para gestión de persistencia
- Repositorios automáticos que reducen código repetitivo
- Soporte para consultas complejas y optimizaciones

**Maven**
- Gestión de dependencias y construcción de proyectos
- Estructura estándar reconocida en la industria
- Integración con herramientas de CI/CD

### 3.2 Frontend

**Angular 17**
- Framework SPA moderno con arquitectura basada en componentes
- TypeScript nativo para desarrollo tipado y robusto
- CLI potente para generación y gestión de código
- Ecosystem maduro con librerías especializadas

**TypeScript**
- Tipado estático que reduce errores en tiempo de desarrollo
- Mejor experiencia de desarrollo con IntelliSense
- Refactoring seguro y mantenible

**Angular Material / Bootstrap**
- Componentes UI consistentes y accesibles
- Responsive design out-of-the-box
- Themes personalizables para branding

### 3.3 Base de Datos

**PostgreSQL 15**
- Base de datos relacional robusta y escalable
- Soporte nativo para JSON (JSONB) para datos semi-estructurados
- Rendimiento superior en consultas complejas
- Funcionalidades avanzadas como indexes parciales y triggers
- Replicación nativa para alta disponibilidad

**Justificación de no usar Redis:**
- Simplicidad arquitectónica para un proyecto académico
- PostgreSQL ofrece capacidades de caché suficientes para el alcance actual
- Reduce la complejidad operativa y de configuración
- Permite focus en la lógica de negocio principal

### 3.4 Infraestructura

**Docker & Docker Compose**
- Containerización para consistencia entre entornos
- Despliegue simplificado y reproducible
- Aislamiento de servicios y dependencias
- Escalabilidad horizontal facilitada

**Nginx (API Gateway)**
- Punto único de entrada para todos los requests
- Load balancing entre instancias de servicios
- Rate limiting y cache HTTP
- Terminación SSL y compresión

### 3.5 Integración Externa

**MercadoPago SDK**
- Procesador de pagos líder en Latinoamérica
- Soporte para múltiples métodos de pago
- Webhooks para notificaciones de estado
- Entorno sandbox para desarrollo y testing

**JavaMail / SendGrid**
- Envío de emails transaccionales
- Delivery de códigos QR y confirmaciones
- Templates HTML personalizados

### 3.6 Testing

**JUnit 5**
- Framework de testing unitario estándar para Java
- Soporte para testing parameterizado y dinámico
- Integración con IDE y herramientas de CI

**Mockito**
- Mocking framework para aislamiento de dependencias
- Testing de interacciones entre componentes
- Simulación de servicios externos

**TestContainers**
- Testing de integración con bases de datos reales
- Containerización de dependencias para tests
- Ambiente de testing aislado y reproducible

---

## 4. Especificación de Microservicios

### 4.1 AUTH-SERVICE (Puerto 8081)

**Responsabilidades Principales:**
- Autenticación diferenciada para Administradores (email + password) y Clientes (DNI + password)
- Generación y validación de tokens JWT
- Gestión de sesiones de usuario
- Control de permisos basado en roles
- Recuperación de contraseñas mediante email
- Auditoría de intentos de login

**Base de Datos:** auth_db (PostgreSQL - Puerto 5433)

**Funcionalidades Principales:**
- Login diferenciado por tipo de usuario
- Validación de tokens para otros microservicios
- Gestión de roles y permisos
- Seguimiento de sesiones activas
- Sistema de bloqueo por intentos fallidos
- Logs de auditoría de seguridad

**Dependencias:**
- Servicio de Email para recuperación de contraseñas
- Sin dependencias con otros microservicios internos (por diseño)

**APIs Principales:**
- POST /auth/admin/login - Autenticación de administradores
- POST /auth/customer/login - Autenticación de clientes
- POST /auth/validate - Validación de tokens para otros servicios
- POST /auth/logout - Cierre de sesión
- GET /auth/permissions/{userId} - Obtener permisos de usuario

### 4.2 USERS-SERVICE (Puerto 8082)

**Responsabilidades Principales:**
- Gestión de perfiles de usuario
- Almacenamiento de información personal
- Gestión de preferencias de usuario
- Administración de direcciones
- Actualización de datos personales

**Base de Datos:** users_db (PostgreSQL - Puerto 5434)

**Funcionalidades Principales:**
- CRUD de perfiles de usuario
- Gestión de información personal (nombre, apellido, documento, teléfono)
- Almacenamiento de preferencias del usuario
- Gestión de múltiples direcciones por usuario
- Validación de datos personales

**Dependencias:**
- AUTH-SERVICE para validación de tokens
- Sincronización con AUTH-SERVICE para mantener coherencia de datos

**APIs Principales:**
- GET /users/profile - Obtener perfil del usuario autenticado
- PUT /users/profile - Actualizar información personal
- POST /users/addresses - Agregar nueva dirección
- GET /users/preferences - Obtener preferencias del usuario

### 4.3 EVENT-SERVICE (Puerto 8083)

**Responsabilidades Principales:**
- Gestión completa de eventos (CRUD)
- Administración de consumiciones y categorías
- Control de stock en tiempo real
- Vinculación de consumiciones con eventos específicos
- Gestión de capacidad de eventos

**Base de Datos:** events_db (PostgreSQL - Puerto 5435)

**Funcionalidades Principales:**
- Creación y gestión de eventos
- Definición de consumiciones y categorías
- Control de stock disponible, reservado y vendido
- Configuración de precios por evento
- Gestión de capacidad máxima de eventos
- Historial de movimientos de stock

**Dependencias:**
- AUTH-SERVICE para validación de permisos de administrador
- Comunicación con ORDER-SERVICE para reserva de stock

**APIs Principales:**
- GET /events/public - Eventos públicos disponibles
- POST /events - Crear nuevo evento (Admin)
- GET /events/{id}/stock - Consultar stock disponible
- POST /events/{id}/consumptions - Agregar consumiciones al evento
- PUT /events/{id}/stock - Actualizar stock

### 4.4 ORDER-SERVICE (Puerto 8084)

**Responsabilidades Principales:**
- Gestión de carritos de compra temporales
- Procesamiento de órdenes de compra
- Construcción de paquetes personalizados
- Validación de disponibilidad de items
- Gestión del ciclo de vida de órdenes

**Base de Datos:** orders_db (PostgreSQL - Puerto 5436)

**Funcionalidades Principales:**
- Carrito de compras persistente con expiración
- Validación de stock antes de agregar items
- Cálculo automático de totales e impuestos
- Generación de números de orden únicos
- Gestión de estados de orden (Pending, Paid, Completed, Cancelled)
- Configuración de paquetes con descuentos

**Dependencias:**
- AUTH-SERVICE para validación de tokens
- EVENT-SERVICE para verificación de stock y precios
- USER-SERVICE para información del comprador
- Comunicación con PAYMENT-SERVICE para confirmación de pagos

**APIs Principales:**
- POST /orders/cart/add - Agregar item al carrito
- GET /orders/cart - Obtener carrito actual
- POST /orders/checkout - Procesar checkout
- GET /orders/{id} - Obtener detalle de orden
- PUT /orders/{id}/status - Actualizar estado de orden

### 4.5 PAYMENT-SERVICE (Puerto 8085)

**Responsabilidades Principales:**
- Integración completa con MercadoPago
- Procesamiento de pagos y transacciones
- Manejo de webhooks de notificación
- Gestión de estados de pago
- Reconciliación de transacciones

**Base de Datos:** payments_db (PostgreSQL - Puerto 5437)

**Funcionalidades Principales:**
- Creación de preferencias de pago en MercadoPago
- Procesamiento de respuestas de pago
- Manejo de webhooks para actualizaciones de estado
- Gestión de reembolsos y contracargos
- Auditoría completa de transacciones
- Soporte para múltiples métodos de pago

**Dependencias:**
- AUTH-SERVICE para validación de tokens
- ORDER-SERVICE para actualización de estado de órdenes
- MercadoPago API para procesamiento de pagos
- Servicio de Email para confirmaciones

**APIs Principales:**
- POST /payments/create - Crear nueva transacción de pago
- POST /payments/webhook - Endpoint para webhooks de MercadoPago
- GET /payments/{id}/status - Consultar estado de pago
- POST /payments/refund - Procesar reembolso

### 4.6 QR-SERVICE (Puerto 8086)

**Responsabilidades Principales:**
- Generación de códigos QR únicos por orden
- Validación de entrada a eventos
- Gestión de canje de consumiciones
- Auditoría de validaciones y canjes
- Control de uso de códigos QR

**Base de Datos:** qr_validation_db (PostgreSQL - Puerto 5438)

**Funcionalidades Principales:**
- Generación de códigos QR únicos y seguros
- Validación de entrada al evento (una sola vez)
- Canje progresivo de consumiciones incluidas en la orden
- Registro detallado de todas las validaciones
- Control de expiración de códigos QR
- Geolocalización de validaciones

**Dependencias:**
- AUTH-SERVICE para validación de permisos
- ORDER-SERVICE para obtener detalles de compra
- Servicio de Email para envío de códigos QR

**APIs Principales:**
- POST /qr/generate - Generar código QR para orden
- POST /qr/validate - Validar entrada al evento
- POST /qr/redeem - Canjear consumición
- GET /qr/{hash}/details - Obtener detalles del código QR
- GET /qr/user/{userId} - Códigos QR del usuario

### 4.7 ANALYTICS-SERVICE (Puerto 8087)

**Responsabilidades Principales:**
- Recopilación de métricas de negocio
- Generación de reportes de ventas y asistencia
- Análisis de consumiciones más populares
- Dashboard administrativo en tiempo real
- Tracking de comportamiento de usuarios

**Base de Datos:** analytics_db (PostgreSQL - Puerto 5439)

**Funcionalidades Principales:**
- Métricas de ventas por evento y período
- Análisis de asistencia y tasa de conversión
- Reportes de consumiciones más populares
- Seguimiento de comportamiento de compra
- Generación de reportes exportables
- Dashboard con visualizaciones en tiempo real

**Dependencias:**
- AUTH-SERVICE para validación de permisos de administrador
- EVENT-SERVICE para datos de eventos
- ORDER-SERVICE para datos de ventas
- PAYMENT-SERVICE para datos de transacciones
- QR-SERVICE para datos de asistencia y consumo

**APIs Principales:**
- GET /analytics/events/{id}/metrics - Métricas del evento
- GET /analytics/sales/reports - Reportes de ventas
- GET /analytics/dashboard - Datos para dashboard
- POST /analytics/export - Exportar reportes

---

## 5. Modelo de Datos

### 5.1 Estrategia de Persistencia

PackedGo implementa el patrón "Database per Service", donde cada microservicio mantiene su propia base de datos PostgreSQL independiente. Esta estrategia garantiza:

- **Desacoplamiento total:** Ningún servicio puede acceder directamente a los datos de otro
- **Evolución independiente:** Los esquemas pueden modificarse sin impactar otros servicios
- **Escalabilidad específica:** Cada base de datos puede optimizarse según sus patrones de acceso
- **Tolerancia a fallos:** El fallo de una base de datos no afecta otros servicios

### 5.2 Descripción Conceptual por Base de Datos

**auth_db (AUTH-SERVICE):**
Contiene toda la información relacionada con autenticación y autorización. Gestiona usuarios con diferentes tipos de login (email para admins, DNI para clientes), sesiones activas, tokens de recuperación de contraseña, permisos por rol y auditoría de intentos de acceso. El diseño permite escalabilidad horizontal mediante réplicas de lectura.

**users_db (USER-SERVICE):**
Almacena perfiles completos de usuarios con información personal, direcciones múltiples y preferencias personalizables. Los datos están normalizados para evitar redundancia y optimizar consultas de perfil. El uso de JSONB permite almacenar preferencias flexibles sin cambios de esquema.

**events_db (EVENT-SERVICE):**
Gestiona eventos, consumiciones y stock con alta consistencia transaccional. Incluye categorización de consumiciones, control de stock por evento y auditoría de movimientos. El diseño optimiza consultas de disponibilidad y actualización atómica de stock.

**orders_db (ORDER-SERVICE):**
Maneja el ciclo completo de órdenes desde carrito temporal hasta pedido confirmado. Incluye configuración de paquetes personalizados, cálculo de descuentos y trazabilidad completa del proceso de compra. El diseño facilita consultas de historial y reportes.

**payments_db (PAYMENT-SERVICE):**
Almacena transacciones de pago con integración completa con MercadoPago. Incluye gestión de webhooks, reconciliación automática y auditoría de todas las operaciones financieras. Los datos están optimizados para consultas de estado y reporting financiero.

**qr_validation_db (QR-SERVICE):**
Gestiona códigos QR únicos con validación de entrada y canje de consumiciones. Incluye auditoría completa de validaciones, geolocalización de canjes y control de expiración. El diseño optimiza consultas de validación en tiempo real.

**analytics_db (ANALYTICS-SERVICE):**
Contiene métricas agregadas y datos analíticos optimizados para reporting. Incluye vistas pre-calculadas para dashboards, tracking de comportamiento y reportes exportables. El diseño facilita consultas complejas de análisis temporal.

### 5.3 Consideraciones de Integridad y Consistencia

**Consistencia Eventual:**
Dado que los datos están distribuidos, el sistema implementa consistencia eventual mediante eventos asíncronos para mantener sincronización entre servicios cuando es necesario.

**Transacciones Distribuidas:**
Se evitan las transacciones distribuidas complejas en favor de patrones como Saga para mantener consistencia en operaciones que abarcan múltiples servicios.

**Integridad Referencial:**
Cada servicio mantiene integridad referencial dentro de su dominio, utilizando IDs externos para referenciar entidades de otros servicios sin crear dependencias directas.

---

## 6. Flujos de Negocio Principales

### 6.1 Flujo del Administrador

El flujo administrativo comienza con la autenticación mediante email y contraseña, dirigiendo al usuario a un dashboard especializado donde puede gestionar todos los aspectos del evento.

**Etapa 1: Autenticación Administrativa**
El administrador accede mediante credenciales de email, el AUTH-SERVICE valida permisos específicos y genera un token JWT con scope administrativo que permite acceso a funcionalidades restringidas.

**Etapa 2: Gestión de Eventos**
Desde el panel administrativo, puede crear nuevos eventos definiendo fecha, ubicación, capacidad máxima y precio base. El EVENT-SERVICE almacena esta información y la hace disponible para consultas públicas.

**Etapa 3: Configuración de Consumiciones**
El administrador define categorías de consumiciones (bebidas, comidas, extras) y asigna stock específico para cada evento. Esta configuración permite control granular de disponibilidad por evento.

**Etapa 4: Monitoreo en Tiempo Real**
Durante la venta, puede monitorear stock en tiempo real, ver métricas de ventas y ajustar disponibilidad según demanda. El ANALYTICS-SERVICE proporciona dashboard actualizado constantemente.

**Etapa 5: Validación Durante el Evento**
En el día del evento, utiliza herramientas de validación de QR para controlar acceso y procesar canjes de consumiciones, con actualizaciones inmediatas en el sistema.

### 6.2 Flujo del Cliente/Consumidor

El flujo del consumidor está diseñado para ser intuitivo y eficiente, desde el descubrimiento del evento hasta el consumo durante el mismo.

**Etapa 1: Descubrimiento y Registro**
Los usuarios pueden explorar eventos disponibles sin registro, pero deben crear una cuenta utilizando su DNI para proceder con compras. El proceso de registro es simplificado solicitando solo información esencial.

**Etapa 2: Exploración de Eventos**
La interfaz presenta eventos disponibles con información detallada, disponibilidad de stock y opciones de consumiciones. Los usuarios pueden filtrar por fecha, ubicación y tipo de evento.

**Etapa 3: Construcción de Paquetes**
El constructor de paquetes permite combinar entrada con consumiciones según preferencias personales. El sistema valida disponibilidad en tiempo real y calcula precios automáticamente.

**Etapa 4: Proceso de Compra**
El checkout guía al usuario través de confirmación de items, datos de facturación y proceso de pago mediante MercadoPago. El sistema mantiene reserva temporal durante el proceso de pago.

**Etapa 5: Confirmación y QR**
Tras confirmación de pago, el usuario recibe inmediatamente un código QR único por email que contiene toda la información de su compra y consumiciones incluidas.

**Etapa 6: Experiencia en el Evento**
Durante el evento, presenta el QR para ingreso (validación única) y posteriormente para canjear consumiciones de manera progresiva, con seguimiento en tiempo real de saldo disponible.

### 6.3 Casos de Uso Críticos

**Concurrencia en Compras:**
Cuando múltiples usuarios intentan comprar los últimos items disponibles, el sistema implementa reservas temporales durante el proceso de checkout para evitar sobre-venta, liberando automáticamente si el pago no se completa en el tiempo establecido.

**Fallo en Procesamiento de Pago:**
Si MercadoPago experimenta intermitencias, el sistema mantiene las reservas de stock y permite reintentar el pago. Los usuarios reciben notificaciones claras del estado y opciones para completar la transacción.

**Validación Offline de QR:**
Para situaciones donde la conectividad en el evento es limitada, los códigos QR incluyen información suficiente para validación offline, con sincronización posterior cuando se restaure la conectividad.

### 6.4 Manejo de Excepciones Principales

**Stock Insuficiente:**
Cuando el stock se agota durante la construcción del paquete, el sistema notifica inmediatamente al usuario y sugiere alternativas similares o permite modificar cantidades.

**Timeout en Pagos:**
Si el proceso de pago excede el tiempo límite, el sistema libera automáticamente las reservas y notifica al usuario, preservando la integridad del stock.

**Códigos QR Comprometidos:**
El sistema incluye mecanismos de detección de códigos duplicados o comprometidos, permitiendo invalidación inmediata y generación de códigos de reemplazo.

---

## 7. Seguridad y Autenticación

### 7.1 Estrategia de Autenticación Diferenciada

PackedGo implementa un sistema de autenticación dual que se adapta a las necesidades específicas de cada tipo de usuario:

**Autenticación de Administradores:**
- Credenciales: Email + Contraseña
- Justificación: Los administradores requieren acceso desde múltiples dispositivos y ubicaciones, haciendo el email un identificador más práctico y profesional
- Validaciones adicionales: Verificación de dominio de email corporativo cuando corresponda
- Recuperación: Sistema de reset por email con tokens temporales de 24 horas

**Autenticación de Clientes:**
- Credenciales: DNI + Contraseña
- Justificación: El DNI es un identificador único, fácil de recordar y verificable, ideal para usuarios ocasionales
- Validaciones adicionales: Algoritmo de validación de DNI argentino
- Recuperación: Requiere verificación adicional por email para mayor seguridad

### 7.2 Autorización Basada en Roles

**Modelo RBAC (Role-Based Access Control):**
El sistema implementa un modelo de permisos granular donde cada acción está asociada a un permiso específico:

- **CUSTOMER:** Permisos limitados a consulta de eventos, gestión de perfil, creación de órdenes y visualización de códigos QR propios
- **ADMIN:** Permisos completos sobre eventos, stock, validación de QR, analytics y gestión de usuarios
- **SUPER_ADMIN:** Acceso total al sistema incluyendo configuración de permisos y administración de otros administradores

**Validación de Permisos:**
Cada endpoint implementa validación automática mediante anotaciones que verifican tanto el token JWT válido como los permisos específicos requeridos para la operación.

### 7.3 Gestión de Tokens JWT

**Estructura del Token:**
Los tokens JWT incluyen información del usuario (ID, rol), permisos específicos, timestamp de emisión y expiración configurables por tipo de usuario.

**Estrategia de Renovación:**
- Tokens de acceso con expiración corta (1 hora para admins, 4 horas para clientes)
- Refresh tokens con expiración extendida para renovación automática
- Invalidación inmediata en caso de logout o actividad sospechosa

**Almacenamiento Seguro:**
- Los tokens se almacenan en httpOnly cookies cuando es posible
- Implementación de secure flags y SameSite para prevenir ataques XSS/CSRF
- Rotación automática de secrets para firmado de tokens

### 7.4 Comunicación Segura entre Servicios

**Service-to-Service Authentication:**
Cada microservicio incluye un cliente HTTP configurado que automáticamente incluye tokens de servicio para comunicación interna.

**Network Segmentation:**
La configuración de Docker Compose establece redes internas que aíslan la comunicación entre servicios de acceso externo directo.

**API Gateway Security:**
El gateway implementa rate limiting, validación de entrada y logging de seguridad para detectar patrones de ataque.

---

## 8. Integración con Servicios Externos

### 8.1 MercadoPago: Procesamiento de Pagos

**Integración Completa:**
PackedGo utiliza el SDK oficial de MercadoPago para Java, implementando el flujo completo de checkout desde la creación de preferencias hasta el procesamiento de webhooks de confirmación.

**Flujo de Pago:**
1. **Creación de Preferencia:** El PAYMENT-SERVICE genera una preferencia de pago con los detalles de la orden
2. **Redirección Segura:** El usuario es redirigido a la página de checkout de MercadoPago
3. **Procesamiento:** MercadoPago procesa el pago utilizando el método elegido por el usuario
4. **Notificación:** Webhooks notifican el cambio de estado de la transacción
5. **Confirmación:** El sistema actualiza automáticamente el estado de la orden y libera el stock

**Gestión de Webhooks:**
Los webhooks de MercadoPago son procesados de forma asíncrona para garantizar disponibilidad. El sistema implementa validación de firma y deduplicación de eventos para evitar procesamiento múltiple.

**Manejo de Errores:**
- Timeout de pagos con liberación automática de stock
- Reintentos automáticos para fallos de comunicación
- Notificaciones claras al usuario sobre estado de transacciones
- Reconciliación diaria para detectar discrepancias

### 8.2 Servicio de Email: Notificaciones y QR

**Configuración SMTP:**
El sistema utiliza JavaMail con configuración SMTP para SendGrid, proporcionando alta deliverability y tracking de emails.

**Tipos de Emails:**
- **Bienvenida:** Confirmación de registro con link de activación
- **Confirmación de Compra:** Detalles de la orden y código QR adjunto
- **Recuperación de Contraseña:** Link temporal para reset de credenciales
- **Notificaciones de Evento:** Recordatorios y actualizaciones importantes

**Templates HTML:**
Emails responsivos con branding consistente, información clara de la compra y códigos QR optimizados para lectura móvil.

### 8.3 Consideraciones de Disponibilidad

**Circuit Breaker Pattern:**
Implementación de circuit breakers para servicios externos, permitiendo degradación elegante cuando MercadoPago o el servicio de email experimentan intermitencias.

**Timeouts y Reintentos:**
Configuración de timeouts apropiados para cada servicio externo con estrategias de reintento exponencial para fallos temporales.

**Fallback Strategies:**
- Emails pueden reenviarse automáticamente si falla el primer intento
- Códigos QR están disponibles en el perfil del usuario como respaldo
- Pagos pendientes pueden completarse posteriormente desde el perfil

---

## 9. Estrategia de Despliegue

### 9.1 Containerización con Docker

**Estructura de Contenedores:**
Cada microservicio se empaqueta en su propio contenedor Docker con imagen base OpenJDK 21, optimizada para tiempo de startup y uso de memoria.

**Multi-stage Builds:**
Los Dockerfiles implementan builds multi-etapa para optimizar el tamaño final de las imágenes, separando la compilación de Maven del runtime final.

**Health Checks:**
Cada contenedor incluye health checks específicos que verifican tanto la disponibilidad del servicio como la conectividad a su base de datos.

### 9.2 Docker Compose para Desarrollo

**Orquestación Local:**
Docker Compose gestiona todos los servicios, bases de datos y dependencias, permitiendo levantar el entorno completo con un solo comando.

**Redes Internas:**
Configuración de redes Docker que aíslan la comunicación entre servicios mientras permiten acceso controlado desde el gateway.

**Volúmenes Persistentes:**
Datos de PostgreSQL y logs se almacenan en volúmenes Docker para persistencia entre reinicios del entorno.

### 9.3 Variables de Entorno y Configuración

**Externalización de Configuración:**
Todas las configuraciones sensibles (URLs de bases de datos, secrets de JWT, credenciales de MercadoPago) se manejan a través de variables de entorno.

**Perfiles de Spring:**
Diferentes perfiles (development, testing, production) permiten configuraciones específicas para cada entorno sin cambios de código.

**Secrets Management:**
Para producción, integración con herramientas como Docker Secrets o AWS Secrets Manager para gestión segura de credenciales.

### 9.4 Estrategia de Escalabilidad

**Escalabilidad Horizontal:**
Cada microservicio puede escalarse independientemente agregando instancias adicionales detrás del load balancer.

**Database Connection Pooling:**
HikariCP optimiza el uso de conexiones a PostgreSQL, permitiendo mayor concurrencia con menos recursos.

**Optimizaciones de Performance:**
- JVM tuning específico para cada servicio
- Configuración de garbage collection optimizada
- Caching a nivel de aplicación para datos frecuentemente accedidos

---

## 10. Testing y Calidad

### 10.1 Estrategia de Testing por Capas

**Unit Testing:**
Cada clase y método crítico incluye tests unitarios con JUnit 5, alcanzando cobertura mínima del 80% en lógica de negocio.

**Integration Testing:**
Tests de integración utilizan TestContainers para levantar instancias reales de PostgreSQL, garantizando que las consultas y transacciones funcionen correctamente.

**Contract Testing:**
Verificación de contratos entre microservicios para detectar cambios que puedan romper la comunicación entre servicios.

**End-to-End Testing:**
Tests automatizados que verifican flujos completos desde la interfaz de usuario hasta la persistencia en base de datos.

### 10.2 Testing de Integración entre Servicios

**Mock Services:**
Para testing aislado, cada servicio incluye mocks de sus dependencias externas, permitiendo testing independiente sin requerir todo el ecosistema.

**Service Virtualization:**
Simulación de servicios externos como MercadoPago para testing de flujos de pago sin costo ni dependencias externas.

**Data Test Management:**
Scripts automatizados para setup y teardown de datos de testing, garantizando tests determinísticos y reproducibles.

### 10.3 Criterios de Aceptación

**Funcionales:**
- Todos los flujos principales deben completarse exitosamente
- Validaciones de negocio deben prevenir estados inconsistentes
- Integración con MercadoPago debe manejar todos los estados de pago

**No Funcionales:**
- Tiempo de respuesta promedio < 500ms para operaciones críticas
- Disponibilidad del 99.5% durante horas de operación
- Capacidad para 100 usuarios concurrentes en picos de venta

### 10.4 Métricas de Calidad

**Cobertura de Código:**
Objetivo mínimo del 80% de cobertura en lógica de negocio, medido con JaCoCo y reportado en CI/CD.

**Quality Gates:**
SonarQube analiza código estático detectando bugs, vulnerabilidades y code smells antes de merge a main branch.

**Performance Testing:**
JMeter valida que el sistema mantenga performance aceptable bajo diferentes cargas de trabajo.

---

## 11. Consideraciones de Rendimiento

### 11.1 Optimizaciones de Base de Datos

**Indexing Strategy:**
Índices específicos en columnas frecuentemente consultadas (email, document, order_number, qr_hash) para optimizar tiempo de respuesta.

**Query Optimization:**
- Uso de consultas específicas evitando SELECT *
- Implementación de paginación para listados grandes
- Eager/Lazy loading optimizado según patrones de acceso

**Connection Pooling:**
HikariCP configurado con pool sizes apropiados para cada servicio basado en patrones de uso esperados.

### 11.2 Gestión de Conexiones

**Database Connection Management:**
- Pool mínimo de 5 conexiones por servicio
- Pool máximo escalable basado en carga
- Timeout de conexiones idle para liberar recursos

**HTTP Connection Pooling:**
Cliente HTTP configurado con pool de conexiones para comunicación entre servicios, reduciendo overhead de establecimiento de conexiones.

### 11.3 Estrategias de Caché

**Application-Level Caching:**
Caché en memoria para datos frecuentemente accedidos como permisos de usuario, configuraciones de eventos y precios.

**Query Result Caching:**
Spring Cache implementa caché de resultados de consultas costosas con invalidación automática cuando los datos subyacentes cambian.

**HTTP Caching:**
Headers de caché apropiados para recursos estáticos y respuestas que no cambian frecuentemente.

### 11.4 Rate Limiting

**API Gateway Level:**
Nginx implementa rate limiting basado en IP para prevenir abuso y garantizar disponibilidad equitativa.

**Service Level:**
Rate limiting específico por usuario autenticado para operaciones costosas como generación de reportes.

**Graceful Degradation:**
Cuando se alcanzan límites, el sistema responde con códigos HTTP apropiados y sugerencias de retry.

---

## 12. Monitoreo y Observabilidad

### 12.1 Logs Centralizados

**Structured Logging:**
Logback configurado para generar logs estructurados en formato JSON, facilitando parsing y análisis automatizado.

**Log Levels:**
- ERROR: Fallos que requieren intervención inmediata
- WARN: Situaciones anómalas que no impiden operación
- INFO: Eventos importantes del flujo de negocio
- DEBUG: Información detallada para troubleshooting

**Correlation IDs:**
Cada request incluye un ID único que se propaga a través de todos los servicios involucrados, permitiendo trazabilidad completa.

### 12.2 Métricas de Salud

**Health Endpoints:**
Cada servicio expone endpoints /health que verifican:
- Conectividad a base de datos
- Estado de dependencias externas
- Uso de memoria y CPU
- Número de conexiones activas

**Business Metrics:**
Métricas específicas del negocio como:
- Número de órdenes procesadas por minuto
- Tiempo promedio de checkout
- Tasa de éxito de pagos
- Utilización de stock por evento

### 12.3 Alertas Básicas

**Threshold-based Alerts:**
Alertas automáticas cuando métricas exceden umbrales predefinidos:
- CPU usage > 80% por más de 5 minutos
- Error rate > 5% en 10 minutos
- Database connection pool > 90% utilizado
- Disk space < 20% disponible

**Business Process Alerts:**
Alertas para eventos críticos de negocio:
- Fallo en procesamiento de pagos
- Stock agotado sin reposición
- Códigos QR comprometidos

### 12.4 Dashboard de Administración

**Real-time Monitoring:**
Dashboard web que muestra métricas en tiempo real de todos los servicios, accesible para administradores del sistema.

**Historical Trends:**
Gráficos de tendencias históricas para identificar patrones y planificar capacidad futura.

**Operational Insights:**
Métricas operativas como:
- Servicios más utilizados
- Patrones de carga por hora del día
- Eficiencia de recursos por servicio

---

## 13. Plan de Implementación

### 13.1 Fases de Desarrollo

**Fase 1: Fundación (Semanas 1-4)**
- Setup inicial de arquitectura de microservicios
- Implementación de AUTH-SERVICE con autenticación diferenciada
- USER-SERVICE con gestión básica de perfiles
- Configuración de base de datos y Docker Compose

**Fase 2: Core Business Logic (Semanas 5-8)**
- EVENT-SERVICE con gestión completa de eventos y stock
- ORDER-SERVICE con carrito y procesamiento de órdenes
- Integración básica entre servicios
- Frontend Angular con componentes principales

**Fase 3: Pagos y QR (Semanas 9-12)**
- PAYMENT-SERVICE con integración completa de MercadoPago
- QR-SERVICE con generación y validación
- Testing de flujos end-to-end
- Manejo de webhooks y notificaciones

**Fase 4: Analytics y Optimización (Semanas 13-16)**
- ANALYTICS-SERVICE con métricas y reportes
- Optimizaciones de performance
- Testing de carga y stress
- Documentación y deployment final

### 13.2 Priorización de Funcionalidades

**MVP (Minimum Viable Product):**
1. Autenticación diferenciada (Admin/Cliente)
2. Gestión básica de eventos
3. Carrito de compras y checkout
4. Integración de pagos con MercadoPago
5. Generación y validación de códigos QR

**Funcionalidades de Valor Agregado:**
1. Analytics y reportes avanzados
2. Gestión granular de permisos
3. Optimizaciones de performance
4. Notificaciones por email
5. Dashboard administrativo completo

### 13.3 Hitos Principales

**Hito 1 (Semana 4):** Arquitectura base funcionando con autenticación
**Hito 2 (Semana 8):** Flujo completo de compra sin pagos reales
**Hito 3 (Semana 12):** Integración de pagos y QR funcionando
**Hito 4 (Semana 16):** Sistema completo con analytics y optimizaciones

### 13.4 Cronograma Estimado

**Total: 16 semanas de desarrollo**
- Investigación y setup: 2 semanas
- Desarrollo core: 10 semanas
- Testing e integración: 3 semanas
- Documentación y deployment: 1 semana

---

## 14. Riesgos y Mitigaciones

### 14.1 Riesgos Técnicos

**Complejidad de Microservicios:**
- Riesgo: Overhead de comunicación entre servicios puede impactar performance
- Mitigación: Diseño cuidadoso de APIs, caching estratégico y optimización de comunicación

**Integración con MercadoPago:**
- Riesgo: Cambios en API externa o problemas de conectividad
- Mitigación: Uso de SDK oficial, testing con sandbox, implementación de circuit breakers

**Consistencia de Datos:**
- Riesgo: Inconsistencias entre bases de datos distribuidas
- Mitigación: Diseño de eventos para sincronización, implementación de patrones de consistencia eventual

### 14.2 Estrategias de Contingencia

**Fallback para Servicios Externos:**
- Modo degradado cuando MercadoPago no está disponible
- Colas de email para reintento automático de notificaciones
- Validación offline de códigos QR en casos extremos

**Backup y Recovery:**
- Backups automáticos diarios de todas las bases de datos
- Scripts de restauración automática
- Documentación detallada de procedimientos de recovery

### 14.3 Dependencias Críticas

**Tecnológicas:**
- Estabilidad de versiones de Spring Boot y Angular
- Disponibilidad de MercadoPago API
- Performance de PostgreSQL bajo carga

**De Desarrollo:**
- Coordinación entre desarrollo frontend y backend
- Testing adecuado de integración entre servicios
- Manejo de dependencias de Maven y npm

### 14.4 Mitigación de Riesgos de Proyecto

**Gestión de Tiempo:**
- Buffer de tiempo en cronograma para imprevistos
- Priorización clara de funcionalidades MVP vs nice-to-have
- Checkpoints regulares para evaluar progreso

**Gestión de Alcance:**
- Documentación clara de límites del proyecto
- Proceso definido para cambios de alcance
- Focus en demostración de competencias técnicas

---

## 15. Conclusiones y Trabajo Futuro

### 15.1 Resumen de Logros Esperados

**Demostración Técnica:**
PackedGo evidencia dominio de arquitecturas modernas de microservicios, integrando tecnologías relevantes de la industria en una solución cohesiva y escalable.

**Aplicación Práctica:**
El proyecto resuelve un problema real de la industria de eventos, demostrando capacidad para analizar requisitos de negocio y traducirlos en soluciones técnicas efectivas.

**Competencias Desarrolladas:**
- Arquitectura de sistemas distribuidos
- Integración de servicios externos
- Gestión de seguridad y autenticación
- Desarrollo full-stack con tecnologías modernas
- Testing y aseguramiento de calidad

### 15.2 Valor Diferencial

**Innovación en UX:**
La combinación de paquetes personalizables con validación digital representa una mejora significativa sobre soluciones tradicionales de venta de entradas.

**Arquitectura Escalable:**
El diseño de microservicios permite crecimiento orgánico del sistema, agregando funcionalidades sin impactar componentes existentes.

**Integración Robusta:**
La integración completa con MercadoPago y el sistema de notificaciones por email proporcionan una experiencia de usuario profesional y confiable.

### 15.3 Posibles Mejoras Futuras

**Aplicación Móvil Nativa:**
Desarrollo de apps iOS y Android para mejorar la experiencia móvil, especialmente durante el evento para validación de QR.

**Inteligencia Artificial:**
- Recomendaciones personalizadas de eventos basadas en historial
- Predicción de demanda para optimización de stock
- Detección automática de patrones de fraude

**Funcionalidades Sociales:**
- Sistema de reviews y calificaciones
- Compartir eventos en redes sociales
- Grupos de compra para descuentos

**Expansión de Integraciones:**
- Múltiples procesadores de pago
- Integración con sistemas de gestión de venues
- APIs para partners y distribuidores

### 15.4 Escalabilidad a Largo Plazo

**Escalabilidad Técnica:**
La arquitectura de microservicios facilita escalamiento horizontal, implementación de múltiples regiones y optimizaciones específicas por servicio.

**Escalabilidad de Negocio:**
El diseño modular permite expansión a diferentes tipos de eventos, geografías y modelos de negocio sin requerimientos arquitectónicos fundamentales.

**Evolución Continua:**
La separación clara de responsabilidades y el uso de tecnologías estándar de la industria facilitan la evolución continua del sistema y la incorporación de nuevas tecnologías según surjan necesidades.

---

## Anexos

### Anexo A: Referencias Tecnológicas

- Spring Boot Documentation: https://docs.spring.io/spring-boot/
- Angular Documentation: https://angular.io/docs
- PostgreSQL Documentation: https://www.postgresql.org/docs/
- MercadoPago Developers: https://www.mercadopago.com.ar/developers/
- Docker Documentation: https://docs.docker.com/

### Anexo B: Configuraciones de Ejemplo

*[Nota: Las configuraciones específicas se incluirían en archivos separados del proyecto]*

### Anexo C: Diagramas de Arquitectura

*[Nota: Los diagramas Mermaid se renderizarían en el documento final]*

---

**Documento versión 1.0**  
**Fecha de elaboración:** Septiembre 2025  
**Autores:** David Elías Delfino, Agustín Luparia Mothe  
**Universidad Tecnológica Nacional - Facultad Regional Córdoba**