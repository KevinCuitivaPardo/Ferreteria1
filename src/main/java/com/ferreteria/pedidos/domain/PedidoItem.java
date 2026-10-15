package com.ferreteria.pedidos.domain;

import java.math.BigDecimal;

public record PedidoItem(
        String codigo,
        String nombre,
        String medida,
        int cantidad,
        BigDecimal precioUnitario) {

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
