-- ========================================
-- MIGRATION: Add createdBy Multi-Tenant Security
-- ========================================
-- Fecha: 2025-10-28
-- Descripción: Agrega campo created_by a Consumption y Event para seguridad multi-tenant
-- Estrategia: NO migrar - confiar en JPA auto-ddl (hibernate.ddl-auto=update)
-- ========================================

-- ⚠️ IMPORTANTE: Este archivo es solo de REFERENCIA
-- La migración se hará automáticamente cuando levantes Docker Compose
-- gracias a spring.jpa.hibernate.ddl-auto=update en application.properties

-- ========================================
-- CAMBIOS ESPERADOS EN BASE DE DATOS
-- ========================================

-- Tabla: consumptions
-- - Se agregará columna: created_by BIGINT NOT NULL
-- - Los datos existentes pueden causar error si JPA intenta crear NOT NULL
-- - SOLUCIÓN: Eliminar volumenes de Docker antes de levantar

-- Tabla: events
-- - Ya tiene columna: created_by BIGINT
-- - No requiere cambios adicionales

-- ========================================
-- COMANDOS DOCKER PARA REINICIAR BD LIMPIA
-- ========================================

-- 1. Detener servicios
-- docker-compose down

-- 2. Eliminar volúmenes (⚠️ ELIMINA TODOS LOS DATOS)
-- docker volume rm packedgo_event-db-data

-- 3. Levantar servicios
-- docker-compose up -d --build

-- ========================================
-- VERIFICACIÓN POST-MIGRACIÓN
-- ========================================

-- Verificar estructura de consumptions
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'consumptions' 
  AND column_name = 'created_by';

-- Verificar estructura de events
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'events' 
  AND column_name = 'created_by';
