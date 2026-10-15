package com.ferreteria.ventas.domain;

import java.math.BigDecimal;

/** Quien vende, con el descuento máximo que tiene permitido. */
public record Vendedor(int id, boolean activo, BigDecimal descuentoMaximo) {
}
