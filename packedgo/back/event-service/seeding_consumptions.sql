-- Data Seeding: Consumos variados para pruebas de paginación y filtros
-- Categorías existentes: 3=Gaseosa, 4=Vino, 5=Sandwich
-- Usuario: created_by = 4

-- Limpiar datos anteriores (opcional, comentar si no quieres borrar)
-- DELETE FROM consumptions WHERE id > 5;

-- GASEOSAS (category_id = 3)
INSERT INTO consumptions (name, description, price, category_id, active, created_by, image_url) VALUES
('Coca Cola 500ml', 'Gaseosa cola en botella de 500ml', 1700, 3, true, 4, 'https://images.unsplash.com/photo-1554866585-cd94860890b7?w=400'),
('Sprite 500ml', 'Gaseosa lima-limón refrescante', 1700, 3, true, 4, 'https://images.unsplash.com/photo-1625772299848-391b6a87d7b3?w=400'),
('Fanta Naranja 500ml', 'Gaseosa sabor naranja', 1700, 3, true, 4, 'https://images.unsplash.com/photo-1624517452488-04869289c4ca?w=400'),
('Pepsi 500ml', 'Gaseosa cola alternativa', 1650, 3, true, 4, 'https://images.unsplash.com/photo-1629203851122-3726ecdf080e?w=400'),
('Seven Up 500ml', 'Gaseosa lima-limón sin cafeína', 1650, 3, true, 4, 'https://images.unsplash.com/photo-1581006852262-e4307cf6283a?w=400'),
('Agua Mineral con Gas 500ml', 'Agua mineral gasificada', 1200, 3, true, 4, 'https://images.unsplash.com/photo-1523362628745-0c100150b504?w=400'),
('Agua Mineral sin Gas 500ml', 'Agua mineral natural', 1000, 3, true, 4, 'https://images.unsplash.com/photo-1548839140-29a749e1cf4d?w=400'),
('Schweppes Pomelo 500ml', 'Gaseosa de pomelo rosado', 1800, 3, true, 4, 'https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=400'),

-- VINOS (category_id = 4)
INSERT INTO consumptions (name, description, price, category_id, active, created_by, image_url) VALUES
('Vino Malbec La Linda', 'Vino tinto Malbec de Mendoza', 14500, 4, true, 4, 'https://images.unsplash.com/photo-1510812431401-41d2bd2722f3?w=400'),
('Vino Cabernet Sauvignon', 'Vino tinto con cuerpo', 16000, 4, true, 4, 'https://images.unsplash.com/photo-1586370434639-0fe43b2d32d6?w=400'),
('Vino Blanco Chardonnay', 'Vino blanco fresco y afrutado', 13500, 4, true, 4, 'https://images.unsplash.com/photo-1547595628-c61a29f496f0?w=400'),
('Vino Rosé Pinot Noir', 'Vino rosado ligero y elegante', 12500, 4, true, 4, 'https://images.unsplash.com/photo-1584916201218-f4242ceb4809?w=400'),
('Champagne Extra Brut', 'Espumante seco premium', 25000, 4, true, 4, 'https://images.unsplash.com/photo-1558346490-a72e53ae2d4f?w=400'),
('Vino Merlot Reserva', 'Vino tinto suave y aterciopelado', 15500, 4, true, 4, 'https://images.unsplash.com/photo-1504279577054-acfeccf8fc52?w=400'),
('Vino Torrontés', 'Vino blanco aromático argentino', 11500, 4, true, 4, 'https://images.unsplash.com/photo-1571613316887-6f8d5cbf7ef7?w=400'),

-- SANDWICHES (category_id = 5)
INSERT INTO consumptions (name, description, price, category_id, active, created_by, image_url) VALUES
('Hamburguesa Clásica', 'Hamburguesa de carne con lechuga, tomate y queso', 4500, 5, true, 4, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400'),
('Hamburguesa Doble Cheddar', 'Doble medallón con doble queso cheddar', 6500, 5, true, 4, 'https://images.unsplash.com/photo-1550547660-d9450f859349?w=400'),
('Pancho Completo', 'Salchicha con papas, salsas y condimentos', 3500, 5, true, 4, 'https://images.unsplash.com/photo-1612392062422-ef19b42f74df?w=400'),
('Lomito Completo', 'Lomo con jamón, queso, lechuga, tomate y huevo', 7500, 5, true, 4, 'https://images.unsplash.com/photo-1619221882994-c3315c6e4f4f?w=400'),
('Sandwich de Pollo', 'Pollo grillé con vegetales frescos', 4800, 5, true, 4, 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?w=400'),
('Veggie Burger', 'Hamburguesa vegetariana con vegetales grillados', 4200, 5, true, 4, 'https://images.unsplash.com/photo-1520072959219-c595dc870360?w=400'),
('Choripán', 'Chorizo criollo en pan francés', 3200, 5, true, 4, 'https://images.unsplash.com/photo-1619740455993-9e303e4dea81?w=400'),
('Milanesa Napolitana', 'Milanesa con jamón, queso y salsa', 6800, 5, true, 4, 'https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=400'),
('Tostado Jamón y Queso', 'Clásico tostado mixto', 2800, 5, true, 4, 'https://images.unsplash.com/photo-1528736235302-52922df5c122?w=400'),
('Club Sandwich', 'Triple sandwich con pollo, panceta y vegetales', 5500, 5, true, 4, 'https://images.unsplash.com/photo-1562998653-e8e8c0f6f6e6?w=400');

-- Verificar inserción
SELECT 
    COUNT(*) as total_consumos,
    COUNT(CASE WHEN category_id = 3 THEN 1 END) as gaseosas,
    COUNT(CASE WHEN category_id = 4 THEN 1 END) as vinos,
    COUNT(CASE WHEN category_id = 5 THEN 1 END) as sandwiches
FROM consumptions;
