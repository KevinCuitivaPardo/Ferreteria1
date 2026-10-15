package com.ferreteria.inventario.domain;

/** Cuánto hay de un producto en una sucursal. */
public record ExistenciaSucursal(int sucursalId, String sucursal, int stock) {
}
