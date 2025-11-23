-- Agregar columna location_name a la tabla events
-- Esta columna almacena el nombre del lugar (ej: "Espacio Quality", "La Estación")
-- en lugar de depender únicamente de geocoding

ALTER TABLE events 
ADD COLUMN IF NOT EXISTS location_name VARCHAR(255);

-- Comentario para documentar el campo
COMMENT ON COLUMN events.location_name IS 'Nombre del lugar o venue donde se realiza el evento';
