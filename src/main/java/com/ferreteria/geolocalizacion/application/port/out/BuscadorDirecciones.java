package com.ferreteria.geolocalizacion.application.port.out;

import com.ferreteria.geolocalizacion.domain.Coordenadas;

import java.util.Optional;

/** Puerto de salida: el servicio de mapas que sabe convertir una dirección en coordenadas. */
public interface BuscadorDirecciones {

    Optional<Coordenadas> buscar(String direccion);
}
