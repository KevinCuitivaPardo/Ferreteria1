package com.ferreteria.pedidos.domain;

/** Quien entrega los pedidos de una sucursal. */
public record Domiciliario(int id, String nombre, boolean activo, Integer sucursalId) {
}
