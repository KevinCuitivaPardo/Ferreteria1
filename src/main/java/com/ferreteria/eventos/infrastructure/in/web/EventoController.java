package com.ferreteria.eventos.infrastructure.in.web;

import com.ferreteria.eventos.application.port.in.ConsultarEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import com.ferreteria.sucursales.application.port.in.ConsultarSucursales;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Arrays;

/** Registro de eventos para el administrador. */
@Controller
class EventoController {

    private final ConsultarEventos consulta;
    private final ConsultarSucursales sucursales;

    EventoController(ConsultarEventos consulta, ConsultarSucursales sucursales) {
        this.consulta = consulta;
        this.sucursales = sucursales;
    }

    @GetMapping("/Eventos")
    String lista(@RequestParam(required = false) String tipo,
                 @RequestParam(required = false) Integer sucursal, Model model) {
        TipoEvento filtroTipo = Arrays.stream(TipoEvento.values())
                .filter(t -> t.name().equals(tipo)).findFirst().orElse(null);

        model.addAttribute("eventos", consulta.recientes(filtroTipo, sucursal));
        model.addAttribute("tipos", TipoEvento.values());
        model.addAttribute("listaSucursales", sucursales.listar());
        model.addAttribute("tipoElegido", filtroTipo == null ? "" : filtroTipo.name());
        model.addAttribute("sucursalElegida", sucursal);
        model.addAttribute("maximo", ConsultarEventos.MAXIMO);
        return "eventos/lista";
    }
}
