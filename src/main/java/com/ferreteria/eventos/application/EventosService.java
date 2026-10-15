package com.ferreteria.eventos.application;

import com.ferreteria.eventos.application.port.in.ConsultarEventos;
import com.ferreteria.eventos.application.port.out.EventoRepository;
import com.ferreteria.eventos.domain.Evento;
import com.ferreteria.shared.eventos.TipoEvento;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventosService implements ConsultarEventos {

    private final EventoRepository eventos;

    public EventosService(EventoRepository eventos) {
        this.eventos = eventos;
    }

    @Override
    public List<Evento> recientes(TipoEvento tipo, Integer sucursalId) {
        return eventos.recientes(tipo, sucursalId, MAXIMO);
    }
}
