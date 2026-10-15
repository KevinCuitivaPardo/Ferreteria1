package com.ferreteria.pedidos.domain;

import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.Totales;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Datos de un domicilio. {@code direccion} es la dirección de entrega; {@code latitud}/{@code longitud}
 * son su ubicación en el mapa (nulas si no se pudo ubicar). Las de la sucursal sirven de punto de partida
 * de la ruta.
 *
 * @param montoRecibido   con cuánto pagará el cliente (solo efectivo), para llevar el cambio; puede ser nulo
 * @param asignadoEn      cuándo se le asignó al domiciliario (define el orden de su cola)
 * @param iniciadoEn      cuándo el domiciliario salió a entregar
 * @param finalizadoEn    cuándo se entregó o se cerró
 * @param notaEntrega     lo que anotó el domiciliario al terminar
 */
public record PedidoCabecera(
        int id,
        String numero,
        LocalDateTime fecha,
        String estado,
        BigDecimal descuentoPct,
        BigDecimal total,
        String ferreteria,
        String cliente,
        String direccion,
        String ubicacion,
        String horario,
        String celular1,
        String celular2,
        String vendedor,
        int sucursalId,
        String sucursal,
        Double sucursalLatitud,
        Double sucursalLongitud,
        Double latitud,
        Double longitud,
        Integer domiciliarioId,
        String domiciliario,
        MedioPago medioPago,
        BigDecimal montoRecibido,
        LocalDateTime asignadoEn,
        LocalDateTime iniciadoEn,
        LocalDateTime finalizadoEn,
        String notaEntrega) {

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }

    public boolean sucursalTieneUbicacion() {
        return sucursalLatitud != null && sucursalLongitud != null;
    }

    /** El vuelto que el domiciliario debe llevar; cero si no se anotó con cuánto pagará. */
    public BigDecimal cambio() {
        return Totales.cambio(total, montoRecibido);
    }

    /** El domiciliario debe cobrar en la puerta cuando el pago es en efectivo. */
    public boolean seCobraAlEntregar() {
        return medioPago == MedioPago.EFECTIVO;
    }
}
