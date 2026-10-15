-- Sistema centralizado de ferreterías: sucursales con inventario propio, ventas en el local,
-- domicilios con ubicación, rol de domiciliario y registro de eventos.

-- ---------- Sucursales ----------
CREATE TABLE sucursales (
    id        SERIAL PRIMARY KEY,
    nombre    TEXT             NOT NULL UNIQUE,
    direccion TEXT             NOT NULL,
    latitud   DOUBLE PRECISION,
    longitud  DOUBLE PRECISION,
    activa    BOOLEAN          NOT NULL DEFAULT TRUE
);

-- La ferretería que ya existía pasa a ser la sede principal.
INSERT INTO sucursales (nombre, direccion) VALUES ('Sede principal', 'Dirección por definir');

-- ---------- Usuarios: rol de domiciliario y sucursal ----------
DO $$
DECLARE restriccion TEXT;
BEGIN
    SELECT conname INTO restriccion
    FROM pg_constraint
    WHERE conrelid = 'usuarios'::regclass AND contype = 'c' AND pg_get_constraintdef(oid) LIKE '%rol%';
    IF restriccion IS NOT NULL THEN
        EXECUTE format('ALTER TABLE usuarios DROP CONSTRAINT %I', restriccion);
    END IF;
END $$;

ALTER TABLE usuarios
    ADD CONSTRAINT usuarios_rol_check CHECK (rol IN ('admin', 'vendedor', 'domiciliario'));

ALTER TABLE usuarios ADD COLUMN sucursal_id INTEGER REFERENCES sucursales (id);
UPDATE usuarios SET sucursal_id = (SELECT MIN(id) FROM sucursales) WHERE rol <> 'admin';

-- ---------- Inventario por sucursal ----------
CREATE TABLE inventario_sucursal (
    producto_id INTEGER NOT NULL REFERENCES productos (id) ON DELETE CASCADE,
    sucursal_id INTEGER NOT NULL REFERENCES sucursales (id),
    stock       INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    PRIMARY KEY (producto_id, sucursal_id)
);

INSERT INTO inventario_sucursal (producto_id, sucursal_id, stock)
SELECT id, (SELECT MIN(id) FROM sucursales), stock FROM productos;

ALTER TABLE productos DROP COLUMN stock;

ALTER TABLE movimientos_inventario ADD COLUMN sucursal_id INTEGER REFERENCES sucursales (id);
UPDATE movimientos_inventario SET sucursal_id = (SELECT MIN(id) FROM sucursales);
ALTER TABLE movimientos_inventario ALTER COLUMN sucursal_id SET NOT NULL;

-- ---------- Pedidos (domicilios): sucursal, dirección de entrega, ubicación y domiciliario ----------
ALTER TABLE pedidos
    ADD COLUMN sucursal_id       INTEGER REFERENCES sucursales (id),
    ADD COLUMN direccion_entrega TEXT,
    ADD COLUMN latitud           DOUBLE PRECISION,
    ADD COLUMN longitud          DOUBLE PRECISION,
    ADD COLUMN domiciliario_id   INTEGER REFERENCES usuarios (id);

UPDATE pedidos p
SET sucursal_id       = (SELECT MIN(id) FROM sucursales),
    direccion_entrega = c.direccion
FROM clientes c
WHERE c.id = p.cliente_id;

ALTER TABLE pedidos ALTER COLUMN sucursal_id SET NOT NULL;
ALTER TABLE pedidos ALTER COLUMN direccion_entrega SET NOT NULL;
CREATE INDEX ON pedidos (domiciliario_id);
CREATE INDEX ON pedidos (sucursal_id);

-- ---------- Ventas en el local ----------
CREATE SEQUENCE ventas_numero_seq;

CREATE TABLE ventas (
    id               SERIAL PRIMARY KEY,
    numero           TEXT           NOT NULL UNIQUE
                     DEFAULT ('VEN-' || lpad(nextval('ventas_numero_seq')::text, 5, '0')),
    sucursal_id      INTEGER        NOT NULL REFERENCES sucursales (id),
    vendedor_id      INTEGER        NOT NULL REFERENCES usuarios (id),
    cliente_nombre   TEXT           NOT NULL,
    cliente_telefono TEXT           NOT NULL,
    fecha            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    descuento_pct    NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    total            NUMERIC(14, 2) NOT NULL DEFAULT 0
);
CREATE INDEX ON ventas (fecha DESC);
CREATE INDEX ON ventas (sucursal_id);
CREATE INDEX ON ventas (vendedor_id);

CREATE TABLE venta_items (
    id              SERIAL PRIMARY KEY,
    venta_id        INTEGER        NOT NULL REFERENCES ventas (id) ON DELETE CASCADE,
    producto_id     INTEGER        NOT NULL REFERENCES productos (id),
    cantidad        INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(14, 2) NOT NULL
);
CREATE INDEX ON venta_items (venta_id);

-- ---------- Registro de eventos ----------
CREATE TABLE eventos (
    id          BIGSERIAL PRIMARY KEY,
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now(),
    tipo        TEXT        NOT NULL,
    actor       TEXT        NOT NULL,
    sucursal_id INTEGER REFERENCES sucursales (id),
    descripcion TEXT        NOT NULL
);
CREATE INDEX ON eventos (fecha DESC);
CREATE INDEX ON eventos (tipo);
