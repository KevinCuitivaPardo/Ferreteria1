package com.ferreteria.ventas.infrastructure.out.inventario;

import com.ferreteria.inventario.application.port.in.StockParaVentas;
import com.ferreteria.inventario.domain.Producto;
import com.ferreteria.ventas.application.port.out.CatalogoProductos;
import com.ferreteria.ventas.domain.ProductoEnVenta;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Conecta las ventas con el módulo de inventario, solo a través de su puerto público. */
@Component
class VentasCatalogoAdapter implements CatalogoProductos {

    private final StockParaVentas stock;

    VentasCatalogoAdapter(StockParaVentas stock) {
        this.stock = stock;
    }

    @Override
    public List<ProductoEnVenta> conExistencias(int sucursalId) {
        return stock.conExistencias(sucursalId).stream().map(VentasCatalogoAdapter::aProducto).toList();
    }

    @Override
    public Optional<ProductoEnVenta> bloquearParaVenta(int productoId, int sucursalId) {
        return stock.bloquearParaVenta(productoId, sucursalId).map(VentasCatalogoAdapter::aProducto);
    }

    @Override
    public void descontar(int productoId, int sucursalId, int cantidad) {
        stock.descontar(productoId, sucursalId, cantidad);
    }

    private static ProductoEnVenta aProducto(Producto p) {
        return new ProductoEnVenta(p.id(), p.codigo(), p.nombre(), p.medida(), p.precioVenta(), p.stock());
    }
}
