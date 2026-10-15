package com.ferreteria.ventas.application.port.in;

import com.ferreteria.ventas.domain.ProductoEnVenta;
import com.ferreteria.ventas.domain.VentaDetalle;
import com.ferreteria.ventas.domain.VentaResumen;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ConsultarVentas {

    /** Ventas de los últimos 3 meses: todas para el admin, solo las propias para un vendedor. */
    List<VentaResumen> recientes(String nombreUsuario, boolean esAdmin);

    /** Un vendedor solo puede ver sus propias ventas. */
    Optional<VentaDetalle> detalle(int ventaId, String nombreUsuario, boolean esAdmin);

    List<ProductoEnVenta> productosDisponibles(int sucursalId);

    BigDecimal descuentoMaximo(String nombreUsuario);
}
