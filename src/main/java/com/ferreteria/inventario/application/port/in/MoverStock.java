package com.ferreteria.inventario.application.port.in;

import com.ferreteria.inventario.domain.TipoMovimiento;

public interface MoverStock {

    /** Entrada (suma) o salida (resta) de stock en la sucursal, con su registro en el historial. */
    void registrarMovimiento(int productoId, int sucursalId, TipoMovimiento tipo, int cantidad, String motivo);

    /** Deja el stock de la sucursal en el valor contado y guarda la diferencia como movimiento. */
    void ajustar(int productoId, int sucursalId, int nuevoStock, String motivo);
}
