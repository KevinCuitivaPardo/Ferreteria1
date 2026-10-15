package com.ferreteria.pedidos.infrastructure.out.inventario;

import com.ferreteria.inventario.application.port.in.StockParaVentas;
import com.ferreteria.inventario.domain.Producto;
import com.ferreteria.pedidos.application.port.out.CatalogoProductos;
import com.ferreteria.pedidos.domain.ProductoVendible;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Conecta los pedidos con el módulo de inventario, solo a través de su puerto público. */
@Component
class InventarioCatalogoAdapter implements CatalogoProductos {

    private final StockParaVentas stock;

    InventarioCatalogoAdapter(StockParaVentas stock) {
        this.stock = stock;
    }

    @Override
    public List<ProductoVendible> conExistencias(int sucursalId) {
        return stock.conExistencias(sucursalId).stream().map(InventarioCatalogoAdapter::aVendible).toList();
    }

    @Override
    public Optional<ProductoVendible> bloquearParaVenta(int productoId, int sucursalId) {
        return stock.bloquearParaVenta(productoId, sucursalId).map(InventarioCatalogoAdapter::aVendible);
    }

    @Override
    public void descontar(int productoId, int sucursalId, int cantidad) {
        stock.descontar(productoId, sucursalId, cantidad);
    }

    @Override
    public void reponer(int productoId, int sucursalId, int cantidad) {
        stock.reponer(productoId, sucursalId, cantidad);
    }

    private static ProductoVendible aVendible(Producto p) {
        return new ProductoVendible(p.id(), p.codigo(), p.nombre(), p.medida(), p.precioVenta(), p.stock());
    }
}
