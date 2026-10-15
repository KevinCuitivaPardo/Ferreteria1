package com.ferreteria.ventas.domain;

import java.math.BigDecimal;

public record VentaItem(
        String codigo,
        String nombre,
        String medida,
        int cantidad,
        BigDecimal precioUnitario) {

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
