package com.ferreteria.ventas.domain;

import com.ferreteria.shared.Totales;

import java.math.BigDecimal;
import java.util.List;

public record VentaDetalle(VentaCabecera venta, List<VentaItem> items) {

    public BigDecimal subtotal() {
        return items.stream().map(VentaItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal montoDescuento() {
        return Totales.montoDescuento(subtotal(), venta.descuentoPct());
    }

    /** El vuelto que se le dio al cliente; cero si no se anotó con cuánto pagó. */
    public BigDecimal cambio() {
        return Totales.cambio(venta.total(), venta.montoRecibido());
    }
}
