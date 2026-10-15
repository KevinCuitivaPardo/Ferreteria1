package com.ferreteria.inventario.domain;

import java.math.BigDecimal;

/** Lo que se captura al crear o editar un producto. El precio de venta lo calcula la base (compra × 2). */
public record DatosProducto(
        String codigo,
        String nombre,
        String medida,
        BigDecimal precioCompra,
        int stock,
        int stockMinimo) {
}
