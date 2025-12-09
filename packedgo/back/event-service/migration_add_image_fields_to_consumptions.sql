-- ===============================================
-- Migración: Agregar campos de imagen a consumptions
-- Propósito: Permitir carga local de imágenes en Base64
-- Fecha: 2025-01-XX
-- ===============================================

-- Agregar columnas para almacenamiento de imágenes
ALTER TABLE consumptions 
ADD COLUMN IF NOT EXISTS image_data BYTEA,
ADD COLUMN IF NOT EXISTS image_content_type VARCHAR(100);

-- Comentarios para documentación
COMMENT ON COLUMN consumptions.image_data IS 'Imagen almacenada en formato binario (Base64 decodificado)';
COMMENT ON COLUMN consumptions.image_content_type IS 'Tipo MIME de la imagen (ej: image/jpeg, image/png)';

-- Verificar que las columnas fueron agregadas
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'consumptions' 
  AND column_name IN ('image_data', 'image_content_type');

-- ===============================================
-- NOTAS DE MIGRACIÓN:
-- ===============================================
-- 1. image_data: Almacena bytes de imagen (BYTEA). Frontend envía Base64, backend decodifica.
-- 2. image_content_type: Almacena tipo MIME (image/jpeg, image/png, etc.)
-- 3. Prioridad de visualización: image_data > image_url
-- 4. Si ambos campos están vacíos, se muestra imagen por defecto
-- 5. Compatible con sistema existente de image_url
-- ===============================================
