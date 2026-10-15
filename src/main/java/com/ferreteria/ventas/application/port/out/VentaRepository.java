package com.ferreteria.ventas.application.port.out;

import com.ferreteria.shared.MedioPago;
import com.ferreteria.ventas.domain.VentaCabecera;
import com.ferreteria.ventas.domain.VentaGuardada;
import com.ferreteria.ventas.domain.VentaItem;
import com.ferreteria.ventas.domain.VentaResumen;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de ventas. */
public interface VentaRepository {

    /** Ventas de los últimos 3 meses. Con {@code vendedorId} nulo trae las de todos. */
    List<VentaResumen> recientes(Integer vendedorId);

    /** Si no es admin, solo devuelve la venta cuando es del vendedor indicado. */
    Optional<VentaCabecera> cabecera(int ventaId, boolean esAdmin, int vendedorId);

    List<VentaItem> items(int ventaId);

    VentaGuardada insertar(int sucursalId, int vendedorId, String clienteNombre, String clienteTelefono,
                           BigDecimal descuentoPct, BigDecimal total, MedioPago medioPago,
                           BigDecimal montoRecibido);

    void insertarItem(int ventaId, int productoId, int cantidad, BigDecimal precioUnitario);
}
