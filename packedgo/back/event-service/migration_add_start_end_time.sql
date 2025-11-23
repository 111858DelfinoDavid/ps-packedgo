-- Migration: Add start_time and end_time columns to events table
-- Date: 2025-11-23
-- Description: Agrega columnas para hora de inicio y hora de finalización del evento

-- Agregar columna start_time
ALTER TABLE events 
ADD COLUMN start_time TIMESTAMP;

-- Agregar columna end_time
ALTER TABLE events 
ADD COLUMN end_time TIMESTAMP;

-- Comentarios para documentación
COMMENT ON COLUMN events.start_time IS 'Hora de inicio del evento';
COMMENT ON COLUMN events.end_time IS 'Hora de finalización del evento';

-- Actualizar eventos existentes con valores por defecto (opcional)
-- Si hay eventos existentes, se puede establecer un horario por defecto
-- UPDATE events 
-- SET start_time = event_date, 
--     end_time = event_date + INTERVAL '3 hours'
-- WHERE start_time IS NULL;
