-- Script de inicialización de base de datos para Payment Service
-- PostgreSQL 12+

-- Crear base de datos (ejecutar como superuser)
-- CREATE DATABASE payment_service_db;

-- Conectar a la base de datos
\c payment_service_db;

-- Crear usuario para la aplicación (opcional)
-- CREATE USER payment_user WITH PASSWORD 'secure_password';
-- GRANT ALL PRIVILEGES ON DATABASE payment_service_db TO payment_user;

-- Nota: Las tablas se crearán automáticamente por Hibernate
-- Este script solo es para referencia y configuraciones adicionales

-- Ejemplo de inserción de credenciales de admin (SANDBOX)
-- IMPORTANTE: Reemplazar con credenciales reales de MercadoPago
INSERT INTO admin_credentials (
    admin_id, 
    access_token, 
    public_key, 
    is_active, 
    is_sandbox, 
    created_at
) VALUES (
    1,  -- ID del administrador
    'TEST-123456789-010101-abc123def456-789012345',  -- Access Token de Sandbox
    'TEST-abc123def-456789-012345-678901-234567',    -- Public Key de Sandbox
    true,
    true,  -- Sandbox mode
    NOW()
) ON CONFLICT (admin_id) DO NOTHING;

-- Índices adicionales para mejorar el rendimiento (opcional, Hibernate ya crea algunos)
-- CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
-- CREATE INDEX IF NOT EXISTS idx_payments_admin_status ON payments(admin_id, status);

-- Ver las tablas creadas
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;

-- Ver las credenciales configuradas
SELECT 
    id,
    admin_id,
    is_active,
    is_sandbox,
    created_at
FROM admin_credentials;

-- Consultar pagos (ejemplo)
-- SELECT 
--     id,
--     admin_id,
--     order_id,
--     amount,
--     status,
--     created_at
-- FROM payments
-- ORDER BY created_at DESC
-- LIMIT 10;
