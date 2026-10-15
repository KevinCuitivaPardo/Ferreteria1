package com.ferreteria.geolocalizacion.application;

import com.ferreteria.geolocalizacion.application.port.in.Geocodificar;
import com.ferreteria.geolocalizacion.application.port.out.BuscadorDirecciones;
import com.ferreteria.geolocalizacion.domain.Coordenadas;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GeolocalizacionService implements Geocodificar {

    private final BuscadorDirecciones buscador;

    public GeolocalizacionService(BuscadorDirecciones buscador) {
        this.buscador = buscador;
    }

    @Override
    public Optional<Coordenadas> ubicar(String direccion) {
        if (direccion == null || direccion.isBlank()) {
            return Optional.empty();
        }
        try {
            return buscador.buscar(direccion.trim());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
