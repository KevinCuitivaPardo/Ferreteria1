package com.ferreteria.pedidos.application.port.out;

import com.ferreteria.pedidos.domain.ProductoVendible;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: los productos y existencias de una sucursal (los provee el inventario). */
public interface CatalogoProductos {

    List<ProductoVendible> conExistencias(int sucursalId);

    /** Devuelve el producto y bloquea su fila de stock hasta que termine la transacción. */
    Optional<ProductoVendible> bloquearParaVenta(int productoId, int sucursalId);

    void descontar(int productoId, int sucursalId, int cantidad);

    void reponer(int productoId, int sucursalId, int cantidad);
}
