-- Medio de pago en ventas y pedidos, cola de entregas del domiciliario y fotos de entrega.

-- ---------- Medio de pago y monto recibido ----------
ALTER TABLE ventas
    ADD COLUMN medio_pago     TEXT NOT NULL DEFAULT 'efectivo'
        CHECK (medio_pago IN ('efectivo', 'transferencia', 'tarjeta', 'nequi', 'daviplata')),
    ADD COLUMN monto_recibido NUMERIC(14, 2) CHECK (monto_recibido IS NULL OR monto_recibido >= 0);

ALTER TABLE pedidos
    ADD COLUMN medio_pago     TEXT NOT NULL DEFAULT 'efectivo'
        CHECK (medio_pago IN ('efectivo', 'transferencia', 'tarjeta', 'nequi', 'daviplata')),
    ADD COLUMN monto_recibido NUMERIC(14, 2) CHECK (monto_recibido IS NULL OR monto_recibido >= 0);

-- ---------- Seguimiento de la entrega ----------
ALTER TABLE pedidos
    ADD COLUMN asignado_en   TIMESTAMPTZ,   -- cuándo se le asignó al domiciliario (orden de la cola)
    ADD COLUMN iniciado_en   TIMESTAMPTZ,   -- cuándo salió a entregar
    ADD COLUMN finalizado_en TIMESTAMPTZ,   -- cuándo se entregó o se cerró
    ADD COLUMN nota_entrega  TEXT;          -- lo que anotó el domiciliario al terminar

UPDATE pedidos SET asignado_en = fecha WHERE domiciliario_id IS NOT NULL;
UPDATE pedidos SET finalizado_en = fecha WHERE estado IN ('entregado', 'no_entregado', 'cancelado');

-- ---------- Fotos de la entrega (el archivo vive en disco; aquí solo sus datos) ----------
CREATE TABLE pedido_fotos (
    id             SERIAL PRIMARY KEY,
    pedido_id      INTEGER     NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    archivo        TEXT        NOT NULL,
    tipo_contenido TEXT        NOT NULL,
    tamano         INTEGER     NOT NULL,
    subida_por     INTEGER     NOT NULL REFERENCES usuarios (id),
    fecha          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ON pedido_fotos (pedido_id);

-- Un domiciliario solo puede tener una entrega en curso a la vez
CREATE UNIQUE INDEX pedidos_una_entrega_en_curso
    ON pedidos (domiciliario_id) WHERE estado = 'en_ruta' AND domiciliario_id IS NOT NULL;
