package com.ferreteria.tiemporeal.application.port.out;

import com.ferreteria.tiemporeal.domain.PosicionDomiciliario;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: dónde se guardan las últimas posiciones (hoy en memoria; podría ser una base de datos o Redis). */
public interface PosicionesActuales {

    Optional<PosicionDomiciliario> de(int usuarioId);

    void guardar(PosicionDomiciliario posicion);

    /** Las posiciones actualizadas desde {@code desde}. */
    List<PosicionDomiciliario> desde(Instant desde);
}
