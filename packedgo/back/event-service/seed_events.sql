-- Data Seeding: 25 eventos para pruebas de paginación y filtros
-- Categorías: 2=Electrónica, 3=Rock, 4=Folklore, 5=Comedia
-- Usuario: created_by = 4
-- Campos obligatorios: sold_passes=0, status='SCHEDULED', total_passes=available_passes

-- EVENTOS DE ELECTRÓNICA (6 items)
INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Festival Electrónico Summer', 'Gran festival de música electrónica con los mejores DJs internacionales', '2025-12-15 20:00:00', '2025-12-16 06:00:00', -34.6037, -58.3816, 'Complejo Costa Salguero', 5000, 15000, 'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?w=800', 2, true, 4, 5000, 0, 'SCHEDULED', 5000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Techno Night Buenos Aires', 'Noche de techno con productores locales e internacionales', '2025-12-20 23:00:00', '2025-12-21 07:00:00', -34.5922, -58.3732, 'Club Niceto', 1500, 8000, 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=800', 2, true, 4, 1500, 0, 'SCHEDULED', 1500);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('House Music Marathon', 'Maratón de 12 horas de house music', '2026-01-10 18:00:00', '2026-01-11 06:00:00', -34.6118, -58.3723, 'Groove Buenos Aires', 2000, 10000, 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=800', 2, true, 4, 2000, 0, 'SCHEDULED', 2000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Trance Experience', 'Experiencia inmersiva de trance psicodélico', '2026-01-25 21:00:00', '2026-01-26 05:00:00', -34.5875, -58.3974, 'La Plata Polo Club', 3000, 12000, 'https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=800', 2, true, 4, 3000, 0, 'SCHEDULED', 3000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Drum & Bass Session', 'Sesión especial de drum and bass con DJs británicos', '2026-02-05 22:00:00', '2026-02-06 04:00:00', -34.6037, -58.3816, 'Bahrein Club', 800, 7000, 'https://images.unsplash.com/photo-1571266028243-d220c1acd08e?w=800', 2, true, 4, 800, 0, 'SCHEDULED', 800);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Sunset Electronic Beach', 'Festival electrónico en la playa al atardecer', '2026-02-14 18:00:00', '2026-02-15 02:00:00', -38.0055, -57.5426, 'Playa Grande Mar del Plata', 4000, 11000, 'https://images.unsplash.com/photo-1506157786151-b8491531f063?w=800', 2, true, 4, 4000, 0, 'SCHEDULED', 4000);

-- EVENTOS DE ROCK (7 items)
INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Rock Nacional Clásico', 'Los grandes del rock argentino en un solo escenario', '2025-12-18 20:00:00', '2025-12-19 01:00:00', -34.5494, -58.4492, 'Estadio Vélez Sarsfield', 8000, 18000, 'https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=800', 3, true, 4, 8000, 0, 'SCHEDULED', 8000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Metal Fest Argentina', 'Festival de heavy metal con bandas nacionales e internacionales', '2025-12-28 19:00:00', '2025-12-29 02:00:00', -34.6118, -58.4173, 'Club Ciudad de Buenos Aires', 3000, 16000, 'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?w=800', 3, true, 4, 3000, 0, 'SCHEDULED', 3000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Indie Rock Night', 'Noche de rock independiente con bandas emergentes', '2026-01-08 21:00:00', '2026-01-09 03:00:00', -34.5986, -58.3931, 'Club Vorterix', 1200, 9000, 'https://images.unsplash.com/photo-1501612780327-45045538702b?w=800', 3, true, 4, 1200, 0, 'SCHEDULED', 1200);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Classic Rock Tribute', 'Tributo a las leyendas del rock de los 70s y 80s', '2026-01-18 20:30:00', '2026-01-19 00:30:00', -34.6037, -58.3816, 'Teatro Coliseo', 2500, 14000, 'https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=800', 3, true, 4, 2500, 0, 'SCHEDULED', 2500);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Punk Rock Festival', 'Festival de punk rock con bandas de toda Latinoamérica', '2026-01-30 19:00:00', '2026-01-31 02:00:00', -34.6118, -58.3723, 'Microestadio Malvinas Argentinas', 4000, 12000, 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=800', 3, true, 4, 4000, 0, 'SCHEDULED', 4000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Progressive Rock Experience', 'Experiencia única de rock progresivo', '2026-02-08 20:00:00', '2026-02-09 01:00:00', -34.5986, -58.3931, 'Luna Park', 5000, 17000, 'https://images.unsplash.com/photo-1506157786151-b8491531f063?w=800', 3, true, 4, 5000, 0, 'SCHEDULED', 5000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Garage Rock Jam', 'Jam session de garage rock en formato íntimo', '2026-02-20 21:30:00', '2026-02-21 02:00:00', -34.6037, -58.3816, 'Club Matiné', 600, 6000, 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800', 3, true, 4, 600, 0, 'SCHEDULED', 600);

-- EVENTOS DE FOLKLORE (6 items)
INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Peña Folclórica del Norte', 'Celebración de la música folclórica del norte argentino', '2025-12-22 20:00:00', '2025-12-23 02:00:00', -24.7859, -65.4117, 'Centro Cultural Salta', 1500, 8000, 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800', 4, true, 4, 1500, 0, 'SCHEDULED', 1500);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Festival de Chacareras', 'Encuentro de bailarines y músicos de chacarera', '2026-01-05 19:00:00', '2026-01-06 01:00:00', -27.4692, -58.8306, 'Anfiteatro Corrientes', 2000, 9000, 'https://images.unsplash.com/photo-1429962714451-bb934ecdc4ec?w=800', 4, true, 4, 2000, 0, 'SCHEDULED', 2000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Zambas y Cuecas Argentinas', 'Noche dedicada a zambas y cuecas tradicionales', '2026-01-15 20:30:00', '2026-01-16 00:30:00', -34.6037, -58.3816, 'Casa del Folklore', 800, 7000, 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?w=800', 4, true, 4, 800, 0, 'SCHEDULED', 800);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Folklore Pampeano', 'Festival de folklore de la región pampeana', '2026-01-28 19:30:00', '2026-01-29 01:00:00', -36.6167, -64.2833, 'Plaza San Martín Santa Rosa', 3000, 10000, 'https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=800', 4, true, 4, 3000, 0, 'SCHEDULED', 3000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Coplas del Noroeste', 'Encuentro de copleros y músicos del NOA', '2026-02-10 20:00:00', '2026-02-11 01:00:00', -26.8241, -65.2226, 'Teatro Mercedes Sosa Tucumán', 1800, 8500, 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800', 4, true, 4, 1800, 0, 'SCHEDULED', 1800);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Folklore Cuyano', 'Celebración del folklore de la región de Cuyo', '2026-02-22 19:00:00', '2026-02-23 00:00:00', -32.8895, -68.8458, 'Anfiteatro Frank Romero Day Mendoza', 2500, 9500, 'https://images.unsplash.com/photo-1510906594845-bc082582c8cc?w=800', 4, true, 4, 2500, 0, 'SCHEDULED', 2500);

-- EVENTOS DE COMEDIA (6 items)
INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Stand Up Comedy Night', 'Los mejores comediantes del país en una noche épica', '2025-12-12 21:00:00', '2025-12-12 23:30:00', -34.6037, -58.3816, 'Teatro Gran Rex', 3000, 12000, 'https://images.unsplash.com/photo-1585699324551-f6c309eedeca?w=800', 5, true, 4, 3000, 0, 'SCHEDULED', 3000);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Impro Comedy Show', 'Show de comedia improvisada con participación del público', '2026-01-12 20:30:00', '2026-01-12 22:30:00', -34.5986, -58.3931, 'Teatro Picadilly', 800, 7000, 'https://images.unsplash.com/photo-1527224857830-43a7acc85260?w=800', 5, true, 4, 800, 0, 'SCHEDULED', 800);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Humor Negro & Absurdo', 'Noche de humor negro y comedia absurda', '2026-01-20 22:00:00', '2026-01-21 00:00:00', -34.6118, -58.3723, 'Club de la Comedia', 400, 6000, 'https://images.unsplash.com/photo-1541167760496-1628856ab772?w=800', 5, true, 4, 400, 0, 'SCHEDULED', 400);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Monólogos de Bar', 'Monólogos cómicos en ambiente de bar', '2026-02-01 20:00:00', '2026-02-01 22:00:00', -34.5922, -58.3732, 'El Nacional', 600, 5500, 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800', 5, true, 4, 600, 0, 'SCHEDULED', 600);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Festival Internacional de Comedia', 'Comediantes de todo el mundo en Buenos Aires', '2026-02-12 19:00:00', '2026-02-13 00:00:00', -34.6037, -58.3816, 'Teatro Ópera', 2500, 15000, 'https://images.unsplash.com/photo-1504309092620-4d0ec726efa4?w=800', 5, true, 4, 2500, 0, 'SCHEDULED', 2500);

INSERT INTO events (name, description, start_time, end_time, lat, lng, location_name, max_capacity, base_price, image_url, category_id, active, created_by, available_passes, sold_passes, status, total_passes) VALUES 
('Roast Battle Argentina', 'Batalla de roasts entre comediantes argentinos', '2026-02-25 21:30:00', '2026-02-25 23:30:00', -34.5986, -58.3931, 'Teatro Vorterix', 1000, 8000, 'https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?w=800', 5, true, 4, 1000, 0, 'SCHEDULED', 1000);

-- Resumen
SELECT 
    COUNT(*) as total_eventos,
    COUNT(CASE WHEN category_id = 2 THEN 1 END) as electronica,
    COUNT(CASE WHEN category_id = 3 THEN 1 END) as rock,
    COUNT(CASE WHEN category_id = 4 THEN 1 END) as folklore,
    COUNT(CASE WHEN category_id = 5 THEN 1 END) as comedia
FROM events;
