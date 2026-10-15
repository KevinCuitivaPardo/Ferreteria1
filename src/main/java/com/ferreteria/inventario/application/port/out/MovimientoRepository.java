package com.ferreteria.inventario.application.port.out;

import com.ferreteria.inventario.domain.Movimiento;

import java.util.List;

/** Puerto de salida: historial de movimientos del inventario. */
public interface MovimientoRepository {

    void insertar(int productoId, int sucursalId, int cantidad, String motivo);

    /** Los más recientes primero. */
    List<Movimiento> historial(int productoId, int sucursalId, int limite);
}
