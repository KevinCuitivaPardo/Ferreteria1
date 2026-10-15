package com.ferreteria.eventos.application.port.out;

import com.ferreteria.eventos.domain.Evento;
import com.ferreteria.shared.eventos.TipoEvento;

import java.util.List;

/** Puerto de salida: lectura del registro de eventos. */
public interface EventoRepository {

    List<Evento> recientes(TipoEvento tipo, Integer sucursalId, int limite);
}
