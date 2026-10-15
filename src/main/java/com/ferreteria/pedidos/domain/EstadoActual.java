package com.ferreteria.pedidos.domain;

/** Lo que hace falta saber de un pedido para cambiarle el estado o asignarlo. */
public record EstadoActual(String clave, int sucursalId, Integer domiciliarioId) {
}
