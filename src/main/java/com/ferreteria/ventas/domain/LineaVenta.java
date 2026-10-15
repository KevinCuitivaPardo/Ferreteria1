package com.ferreteria.ventas.domain;

/** Una línea del formulario: producto elegido (0 = ninguno) y cantidad. */
public record LineaVenta(int productoId, Integer cantidad) {
}
