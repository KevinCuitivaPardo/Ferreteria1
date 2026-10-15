package com.ferreteria.inventario.application.port.in;

import com.ferreteria.inventario.domain.DatosProducto;

public interface GestionarProductos {

    /** Crea el producto; el stock inicial queda en la sucursal indicada. */
    void crear(DatosProducto datos, int sucursalId);

    /** Cambia los datos del producto; el stock no se toca (se mueve con {@link MoverStock}). */
    void actualizar(int productoId, DatosProducto datos);

    void eliminar(int productoId);
}
