package com.ferreteria.inventario.domain;

import com.ferreteria.shared.NegocioException;

public class ProductoConMovimientosException extends NegocioException {

    public ProductoConMovimientosException() {
        super("No se puede eliminar: el producto tiene movimientos de inventario.");
    }
}
