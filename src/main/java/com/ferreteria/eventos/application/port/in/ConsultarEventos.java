package com.ferreteria.eventos.application.port.in;

import com.ferreteria.eventos.domain.Evento;
import com.ferreteria.shared.eventos.TipoEvento;

import java.util.List;

public interface ConsultarEventos {

    int MAXIMO = 300;

    /** Los más recientes primero; los filtros nulos significan "todos". */
    List<Evento> recientes(TipoEvento tipo, Integer sucursalId);
}
