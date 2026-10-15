package com.ferreteria.inventario.application.port.out;

import com.ferreteria.inventario.domain.DatosProducto;
import com.ferreteria.inventario.domain.ExistenciaSucursal;
import com.ferreteria.inventario.domain.Producto;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de productos y de su stock por sucursal. */
public interface ProductoRepository {

    List<Producto> listar(int sucursalId);

    List<Producto> buscar(String texto, int sucursalId, int limite);

    List<Producto> conExistencias(int sucursalId);

    Optional<Producto> porId(int productoId, int sucursalId);

    List<ExistenciaSucursal> existencias(int productoId);

    /**
     * Crea el producto y deja su stock inicial en la sucursal.
     *
     * @throws com.ferreteria.inventario.domain.CodigoProductoDuplicadoException si el código ya existe
     */
    void insertar(DatosProducto datos, int sucursalId);

    /** @throws com.ferreteria.inventario.domain.CodigoProductoDuplicadoException si el código ya existe */
    void actualizar(int productoId, DatosProducto datos);

    /** @throws com.ferreteria.inventario.domain.ProductoConMovimientosException si tiene historial */
    void eliminar(int productoId);

    /** Lee el producto con el stock de la sucursal y bloquea esa fila hasta que termine la transacción. */
    Optional<Producto> porIdBloqueado(int productoId, int sucursalId);

    void fijarStock(int productoId, int sucursalId, int stock);

    /** Suma (o resta, si es negativo) al stock sin dejarlo por debajo de cero. Devuelve si se pudo. */
    boolean sumarStock(int productoId, int sucursalId, int delta);
}
