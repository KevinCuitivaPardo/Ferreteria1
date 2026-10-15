package com.ferreteria.pedidos.domain;

import java.math.BigDecimal;

/** Un producto tal como lo ve el pedido: lo que se puede vender y a qué precio. */
public record ProductoVendible(
        int id,
        String codigo,
        String nombre,
        String medida,
        BigDecimal precioVenta,
        int stock) {
}
