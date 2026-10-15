package com.ferreteria.pedidos.domain;

import com.ferreteria.shared.Totales;

import java.math.BigDecimal;
import java.util.List;

public record PedidoDetalle(PedidoCabecera pedido, List<PedidoItem> items, List<FotoPedido> fotos) {

    public BigDecimal subtotal() {
        return items.stream().map(PedidoItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal montoDescuento() {
        return Totales.montoDescuento(subtotal(), pedido.descuentoPct());
    }
}
