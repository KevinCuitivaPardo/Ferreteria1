package com.ferreteria.pedidos.domain;

import java.math.BigDecimal;

/** El usuario que hace el pedido, con el descuento máximo que tiene permitido. */
public record Vendedor(int id, boolean activo, BigDecimal descuentoMaximo) {
}
