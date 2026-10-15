package com.ferreteria.pedidos.domain;

/** Una línea del formulario: producto elegido (0 = ninguno) y cantidad. */
public record LineaPedido(int productoId, Integer cantidad) {
}
