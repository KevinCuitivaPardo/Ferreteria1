-- Esquema de la ferretería (PostgreSQL 16).
-- Reconstruido a partir de las consultas de la aplicación original.

CREATE TABLE usuarios (
    id             SERIAL PRIMARY KEY,
    nombre_usuario TEXT          NOT NULL UNIQUE,
    clave_hash     TEXT          NOT NULL,
    rol            TEXT          NOT NULL CHECK (rol IN ('admin', 'vendedor')),
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    descuento_pct  NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (descuento_pct BETWEEN 0 AND 100)
);

CREATE TABLE productos (
    id            SERIAL PRIMARY KEY,
    codigo        TEXT           NOT NULL UNIQUE,
    nombre        TEXT           NOT NULL,
    medida        TEXT,
    precio_compra NUMERIC(14, 2) NOT NULL DEFAULT 0 CHECK (precio_compra >= 0),
    -- El precio de venta siempre es el doble del de compra.
    precio_venta  NUMERIC(14, 2) GENERATED ALWAYS AS (precio_compra * 2) STORED,
    stock         INTEGER        NOT NULL DEFAULT 0 CHECK (stock >= 0),
    stock_minimo  INTEGER        NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0)
);

CREATE TABLE movimientos_inventario (
    id          SERIAL PRIMARY KEY,
    producto_id INTEGER     NOT NULL REFERENCES productos (id),
    cantidad    INTEGER     NOT NULL,           -- positivo = entrada, negativo = salida
    motivo      TEXT,
    fecha       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ON movimientos_inventario (producto_id, fecha DESC);

CREATE TABLE clientes (
    id                SERIAL PRIMARY KEY,
    nombre_ferreteria TEXT,
    nombre_cliente    TEXT NOT NULL,
    direccion         TEXT NOT NULL,
    ubicacion         TEXT,
    horario           TEXT,
    celular_1         TEXT NOT NULL,
    celular_2         TEXT
);
CREATE INDEX ON clientes (celular_1);
CREATE INDEX ON clientes (celular_2);

CREATE SEQUENCE pedidos_numero_seq;

CREATE TABLE pedidos (
    id            SERIAL PRIMARY KEY,
    numero        TEXT          NOT NULL UNIQUE
                  DEFAULT ('PED-' || lpad(nextval('pedidos_numero_seq')::text, 5, '0')),
    cliente_id    INTEGER       NOT NULL REFERENCES clientes (id),
    vendedor_id   INTEGER       NOT NULL REFERENCES usuarios (id),
    fecha         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    estado        TEXT          NOT NULL DEFAULT 'pendiente'
                  CHECK (estado IN ('pendiente', 'alistado', 'en_ruta', 'entregado', 'no_entregado', 'cancelado')),
    descuento_pct NUMERIC(5, 2) NOT NULL DEFAULT 0,
    total         NUMERIC(14, 2) NOT NULL DEFAULT 0
);
CREATE INDEX ON pedidos (fecha DESC);
CREATE INDEX ON pedidos (vendedor_id);

CREATE TABLE pedido_items (
    id              SERIAL PRIMARY KEY,
    pedido_id       INTEGER        NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    producto_id     INTEGER        NOT NULL REFERENCES productos (id),
    cantidad        INTEGER        NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(14, 2) NOT NULL
);
CREATE INDEX ON pedido_items (pedido_id);
