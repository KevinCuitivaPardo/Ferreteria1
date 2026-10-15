package com.ferreteria.pedidos.application.port.out;

import com.ferreteria.pedidos.domain.Ubicacion;

import java.util.Optional;

/** Puerto de salida: convierte la dirección de entrega en un punto del mapa. */
public interface UbicadorDeDirecciones {

    /** Vacío si no se encuentra o el servicio de mapas no responde. */
    Optional<Ubicacion> ubicar(String direccion);
}
