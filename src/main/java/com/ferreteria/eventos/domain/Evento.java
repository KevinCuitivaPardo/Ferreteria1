package com.ferreteria.eventos.domain;

import com.ferreteria.shared.eventos.TipoEvento;

import java.time.LocalDateTime;

/** Un hecho registrado: quién hizo qué, cuándo y en qué sucursal. */
public record Evento(
        long id,
        LocalDateTime fecha,
        TipoEvento tipo,
        String actor,
        String sucursal,
        String descripcion) {
}
