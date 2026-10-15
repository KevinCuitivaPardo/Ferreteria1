package com.ferreteria.inventario.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Producto(
        int id,
        String codigo,
        String nombre,
        String medida,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        int stock,
        int stockMinimo) {

    /** Alerta: solo si hay un mínimo definido y el stock llegó a ese mínimo o está por debajo. */
    @JsonProperty("stockBajo")
    public boolean stockBajo() {
        return stockMinimo > 0 && stock <= stockMinimo;
    }
}
