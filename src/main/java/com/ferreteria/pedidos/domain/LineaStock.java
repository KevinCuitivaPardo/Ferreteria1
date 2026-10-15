package com.ferreteria.pedidos.domain;

/** Cuánto de un producto reserva un pedido. */
public record LineaStock(int productoId, int cantidad) {
}
