package com.ferreteria.tiemporeal.infrastructure.in.web;

import com.ferreteria.tiemporeal.application.port.in.ConsultarPosiciones;
import com.ferreteria.tiemporeal.domain.PosicionDomiciliario;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Posiciones actuales de los domiciliarios, para que el mapa del administrador arranque con ellas. */
@RestController
class SeguimientoController {

    private final ConsultarPosiciones posiciones;

    SeguimientoController(ConsultarPosiciones posiciones) {
        this.posiciones = posiciones;
    }

    @GetMapping("/Seguimiento/posiciones")
    List<PosicionDomiciliario> posiciones() {
        return posiciones.actuales();
    }
}
