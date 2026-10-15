package com.ferreteria.ventas.domain;

import com.ferreteria.shared.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @param montoRecibido con cuánto pagó el cliente (solo en efectivo); nulo si no se anotó
 */
public record VentaCabecera(
        int id,
        String numero,
        LocalDateTime fecha,
        String cliente,
        String telefono,
        String vendedor,
        String sucursal,
        BigDecimal descuentoPct,
        BigDecimal total,
        MedioPago medioPago,
        BigDecimal montoRecibido) {
}
