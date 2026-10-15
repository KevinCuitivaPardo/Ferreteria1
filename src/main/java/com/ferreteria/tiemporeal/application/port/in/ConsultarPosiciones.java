package com.ferreteria.tiemporeal.application.port.in;

import com.ferreteria.tiemporeal.domain.PosicionDomiciliario;

import java.util.List;

public interface ConsultarPosiciones {

    /** La última posición conocida de cada domiciliario que la compartió hace poco. */
    List<PosicionDomiciliario> actuales();
}
