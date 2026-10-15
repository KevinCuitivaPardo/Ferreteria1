package com.ferreteria.ventas.domain;

import com.ferreteria.shared.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una fila de la lista de ventas. */
public record VentaResumen(
        int id,
        String numero,
        LocalDateTime fecha,
        String cliente,
        String telefono,
        String vendedor,
        String sucursal,
        BigDecimal total,
        MedioPago medioPago) {
}
