package com.ferreteria.pedidos.domain;

import com.ferreteria.shared.MedioPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una fila de la lista de pedidos o de la cola de entregas. */
public record PedidoResumen(
        int id,
        String numero,
        LocalDateTime fecha,
        String ferreteria,
        String cliente,
        String direccion,
        String vendedor,
        String sucursal,
        String domiciliario,
        BigDecimal total,
        String estado,
        MedioPago medioPago,
        LocalDateTime asignadoEn,
        int sucursalId) {
}
