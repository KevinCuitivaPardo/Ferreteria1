package com.ferreteria.inventario.application.port.in;

import com.ferreteria.inventario.domain.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Lo que el inventario ofrece a otros módulos (ventas y pedidos) para vender y devolver existencias
 * de una sucursal. Las operaciones de escritura deben llamarse dentro de la transacción del caso de
 * uso que las usa.
 */
public interface StockParaVentas {

    List<Producto> conExistencias(int sucursalId);

    /** Devuelve el producto con el stock de la sucursal y bloquea esa fila hasta que termine la transacción. */
    Optional<Producto> bloquearParaVenta(int productoId, int sucursalId);

    /** @throws com.ferreteria.shared.NegocioException si no hay stock suficiente */
    void descontar(int productoId, int sucursalId, int cantidad);

    void reponer(int productoId, int sucursalId, int cantidad);
}
