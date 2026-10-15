package com.ferreteria.ventas.application.port.out;

import com.ferreteria.ventas.domain.ProductoEnVenta;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: los productos y existencias de una sucursal (los provee el inventario). */
public interface CatalogoProductos {

    List<ProductoEnVenta> conExistencias(int sucursalId);

    /** Devuelve el producto y bloquea su fila de stock hasta que termine la transacción. */
    Optional<ProductoEnVenta> bloquearParaVenta(int productoId, int sucursalId);

    void descontar(int productoId, int sucursalId, int cantidad);
}
