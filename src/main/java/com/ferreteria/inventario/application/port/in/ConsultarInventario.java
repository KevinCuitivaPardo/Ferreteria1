package com.ferreteria.inventario.application.port.in;

import com.ferreteria.inventario.domain.ExistenciaSucursal;
import com.ferreteria.inventario.domain.Movimiento;
import com.ferreteria.inventario.domain.Producto;

import java.util.List;
import java.util.Optional;

/** El catálogo de productos es de toda la empresa; el stock que se ve es el de la sucursal indicada. */
public interface ConsultarInventario {

    List<Producto> listar(int sucursalId);

    /** Sugerencias para el buscador; un texto vacío devuelve una lista vacía. */
    List<Producto> buscar(String texto, int sucursalId);

    Optional<Producto> porId(int productoId, int sucursalId);

    List<Movimiento> historial(int productoId, int sucursalId);

    /** Existencias del producto en cada sucursal. */
    List<ExistenciaSucursal> existencias(int productoId);
}
