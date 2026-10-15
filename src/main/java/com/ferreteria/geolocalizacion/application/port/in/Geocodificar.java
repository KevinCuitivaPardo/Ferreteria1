package com.ferreteria.geolocalizacion.application.port.in;

import com.ferreteria.geolocalizacion.domain.Coordenadas;

import java.util.Optional;

public interface Geocodificar {

    /**
     * Busca una dirección en el mapa. Devuelve vacío si no se encuentra o si el servicio no responde:
     * nunca debe impedir que se guarde un pedido.
     */
    Optional<Coordenadas> ubicar(String direccion);
}
