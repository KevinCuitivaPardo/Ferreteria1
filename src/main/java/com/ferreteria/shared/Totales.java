package com.ferreteria.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculo de totales con descuento y de cambio, igual para ventas y pedidos. */
public final class Totales {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Totales() {
    }

    /** Pesos colombianos: el total no lleva centavos. */
    public static BigDecimal totalConDescuento(BigDecimal subtotal, BigDecimal descuentoPct) {
        BigDecimal factor = BigDecimal.ONE.subtract(descuentoPct.divide(CIEN));
        return subtotal.multiply(factor).setScale(0, RoundingMode.HALF_UP);
    }

    public static BigDecimal montoDescuento(BigDecimal subtotal, BigDecimal descuentoPct) {
        return subtotal.multiply(descuentoPct).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    /**
     * Lo que se devuelve al cliente (vuelto). Es cero si no se anotó con cuánto pagó o si pagó justo;
     * nunca es negativo.
     */
    public static BigDecimal cambio(BigDecimal total, BigDecimal montoRecibido) {
        if (montoRecibido == null || montoRecibido.compareTo(total) <= 0) {
            return BigDecimal.ZERO;
        }
        return montoRecibido.subtract(total);
    }
}
