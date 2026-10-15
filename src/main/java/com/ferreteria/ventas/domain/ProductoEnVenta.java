package com.ferreteria.ventas.domain;

import java.math.BigDecimal;

/** Un producto tal como lo ve la venta: qué hay en la sucursal y a qué precio. */
public record ProductoEnVenta(
        int id,
        String codigo,
        String nombre,
        String medida,
        BigDecimal precioVenta,
        int stock) {
}
