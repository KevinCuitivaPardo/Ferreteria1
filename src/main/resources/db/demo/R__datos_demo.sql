-- Datos de ejemplo para desarrollo. Se pueden repetir sin duplicar nada.
-- Para no cargarlos (producción): FLYWAY_LOCATIONS=classpath:db/migration

INSERT INTO productos (codigo, nombre, medida, precio_compra, stock_minimo) VALUES
    ('DW-6',  'Tornillo drywall',    '6 x 1',      900, 100),
    ('DW-8',  'Tornillo drywall',    '8 x 2',     1500,  50),
    ('HX-10', 'Tornillo hexagonal',  '1/4 x 2',   4200,  30),
    ('CH-3',  'Chazo plástico',      '3/8',        600, 200),
    ('AR-12', 'Arandela plana',      '1/2',        350,  60),
    ('PT-25', 'Puntilla con cabeza', '2 1/2"',    2800,  40)
ON CONFLICT (codigo) DO NOTHING;

-- Existencias de la sede principal (solo si todavía no tiene registro de ese producto)
INSERT INTO inventario_sucursal (producto_id, sucursal_id, stock)
SELECT p.id, (SELECT MIN(id) FROM sucursales), v.stock
FROM (VALUES ('DW-6', 500), ('DW-8', 40), ('HX-10', 120), ('CH-3', 800), ('AR-12', 0), ('PT-25', 220))
         AS v(codigo, stock)
JOIN productos p ON p.codigo = v.codigo
ON CONFLICT (producto_id, sucursal_id) DO NOTHING;

-- Ubicación de ejemplo de la sede principal (Bogotá), solo si aún no tiene una
UPDATE sucursales
SET direccion = 'Carrera 7 # 32-16, Bogotá', latitud = 4.6097, longitud = -74.0817
WHERE nombre = 'Sede principal' AND latitud IS NULL;

INSERT INTO clientes (nombre_ferreteria, nombre_cliente, direccion, ubicacion, horario, celular_1, celular_2)
SELECT 'Ferretería El Tornillo', 'Luis Ramírez', 'Calle 45 # 12-30', 'Barrio Centro', 'Lun-Sáb 8am-6pm',
       '3001234567', '3109876543'
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE celular_1 = '3001234567');
