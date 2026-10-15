package com.ferreteria.tiemporeal.domain;

import java.time.Instant;

/** Dónde está un domiciliario en este momento, según el GPS de su teléfono. */
public record PosicionDomiciliario(
        int usuarioId,
        String nombre,
        Integer sucursalId,
        double latitud,
        double longitud,
        Double precisionMetros,
        Instant actualizada) {
}
