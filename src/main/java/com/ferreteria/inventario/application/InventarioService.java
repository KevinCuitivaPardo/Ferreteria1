package com.ferreteria.inventario.application;

import com.ferreteria.inventario.application.port.in.ConsultarInventario;
import com.ferreteria.inventario.application.port.in.GestionarProductos;
import com.ferreteria.inventario.application.port.in.MoverStock;
import com.ferreteria.inventario.application.port.in.StockParaVentas;
import com.ferreteria.inventario.application.port.out.MovimientoRepository;
import com.ferreteria.inventario.application.port.out.ProductoRepository;
import com.ferreteria.inventario.domain.DatosProducto;
import com.ferreteria.inventario.domain.ExistenciaSucursal;
import com.ferreteria.inventario.domain.Movimiento;
import com.ferreteria.inventario.domain.Producto;
import com.ferreteria.inventario.domain.TipoMovimiento;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Casos de uso del inventario: consultar, administrar productos y mover stock por sucursal. */
@Service
public class InventarioService implements ConsultarInventario, GestionarProductos, MoverStock, StockParaVentas {

    private static final int MAX_SUGERENCIAS = 8;
    private static final int MAX_HISTORIAL = 50;

    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;
    private final RegistroEventos eventos;

    public InventarioService(ProductoRepository productos, MovimientoRepository movimientos,
                             RegistroEventos eventos) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.eventos = eventos;
    }

    // ---------- ConsultarInventario ----------

    @Override
    public List<Producto> listar(int sucursalId) {
        return productos.listar(sucursalId);
    }

    @Override
    public List<Producto> buscar(String texto, int sucursalId) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return productos.buscar(texto.trim(), sucursalId, MAX_SUGERENCIAS);
    }

    @Override
    public Optional<Producto> porId(int productoId, int sucursalId) {
        return productos.porId(productoId, sucursalId);
    }

    @Override
    public List<Movimiento> historial(int productoId, int sucursalId) {
        return movimientos.historial(productoId, sucursalId, MAX_HISTORIAL);
    }

    @Override
    public List<ExistenciaSucursal> existencias(int productoId) {
        return productos.existencias(productoId);
    }

    // ---------- GestionarProductos ----------

    @Override
    @Transactional
    public void crear(DatosProducto datos, int sucursalId) {
        if (esVacio(datos.codigo()) || esVacio(datos.nombre())) {
            throw new NegocioException("El código y el nombre son obligatorios.");
        }
        DatosProducto limpios = normalizar(datos);
        productos.insertar(limpios, sucursalId);
        eventos.registrar(TipoEvento.PRODUCTO, sucursalId, "Producto creado: " + limpios.codigo()
                + " · " + limpios.nombre() + " (stock inicial " + limpios.stock() + ")");
    }

    @Override
    @Transactional
    public void actualizar(int productoId, DatosProducto datos) {
        if (esVacio(datos.codigo())) {
            throw new NegocioException("El código es obligatorio.");
        }
        if (esVacio(datos.nombre())) {
            throw new NegocioException("El nombre es obligatorio.");
        }
        DatosProducto limpios = normalizar(datos);
        productos.actualizar(productoId, limpios);
        eventos.registrar(TipoEvento.PRODUCTO, null, "Producto actualizado: " + limpios.codigo()
                + " · " + limpios.nombre());
    }

    @Override
    @Transactional
    public void eliminar(int productoId) {
        productos.eliminar(productoId);
        eventos.registrar(TipoEvento.PRODUCTO, null, "Producto eliminado (id " + productoId + ")");
    }

    // ---------- MoverStock ----------

    @Override
    @Transactional
    public void registrarMovimiento(int productoId, int sucursalId, TipoMovimiento tipo, int cantidad,
                                    String motivo) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor a cero.");
        }

        int delta = tipo == TipoMovimiento.SALIDA ? -cantidad : cantidad;

        if (!productos.sumarStock(productoId, sucursalId, delta)) {
            throw new NegocioException("No hay stock suficiente para esa salida.");
        }
        String motivoLimpio = motivo == null ? "" : motivo.trim();
        movimientos.insertar(productoId, sucursalId, delta, motivoLimpio);
        eventos.registrar(TipoEvento.INVENTARIO, sucursalId, (delta > 0 ? "Entrada de " : "Salida de ")
                + cantidad + " del producto " + nombreDe(productoId, sucursalId)
                + (motivoLimpio.isEmpty() ? "" : " · " + motivoLimpio));
    }

    @Override
    @Transactional
    public void ajustar(int productoId, int sucursalId, int nuevoStock, String motivo) {
        if (nuevoStock < 0) {
            throw new NegocioException("El stock no puede ser negativo.");
        }
        if (esVacio(motivo)) {
            throw new NegocioException(
                    "Escribe el motivo del ajuste (por ejemplo: conteo físico, producto dañado).");
        }

        Producto producto = productos.porIdBloqueado(productoId, sucursalId)
                .orElseThrow(() -> new NegocioException("El producto no existe."));
        int delta = nuevoStock - producto.stock();

        productos.fijarStock(productoId, sucursalId, nuevoStock);
        if (delta != 0) {
            movimientos.insertar(productoId, sucursalId, delta, "Ajuste manual: " + motivo.trim());
            eventos.registrar(TipoEvento.INVENTARIO, sucursalId, "Ajuste de " + producto.codigo() + ": de "
                    + producto.stock() + " a " + nuevoStock + " · " + motivo.trim());
        }
    }

    // ---------- StockParaVentas ----------

    @Override
    public List<Producto> conExistencias(int sucursalId) {
        return productos.conExistencias(sucursalId);
    }

    @Override
    public Optional<Producto> bloquearParaVenta(int productoId, int sucursalId) {
        return productos.porIdBloqueado(productoId, sucursalId);
    }

    @Override
    public void descontar(int productoId, int sucursalId, int cantidad) {
        if (!productos.sumarStock(productoId, sucursalId, -cantidad)) {
            throw new NegocioException("No hay stock suficiente para completar la operación.");
        }
    }

    @Override
    public void reponer(int productoId, int sucursalId, int cantidad) {
        productos.sumarStock(productoId, sucursalId, cantidad);
    }

    // ---------- Ayudas ----------

    private String nombreDe(int productoId, int sucursalId) {
        return productos.porId(productoId, sucursalId)
                .map(p -> p.codigo() + " · " + p.nombre())
                .orElse("#" + productoId);
    }

    private static DatosProducto normalizar(DatosProducto d) {
        return new DatosProducto(
                d.codigo().trim(),
                d.nombre().trim(),
                esVacio(d.medida()) ? null : d.medida().trim(),
                d.precioCompra() == null ? BigDecimal.ZERO : d.precioCompra(),
                d.stock(),
                d.stockMinimo());
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
