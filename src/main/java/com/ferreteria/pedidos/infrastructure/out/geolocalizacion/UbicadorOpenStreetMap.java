package com.ferreteria.pedidos.infrastructure.out.geolocalizacion;

import com.ferreteria.geolocalizacion.application.port.in.Geocodificar;
import com.ferreteria.pedidos.application.port.out.UbicadorDeDirecciones;
import com.ferreteria.pedidos.domain.Ubicacion;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Conecta los pedidos con el módulo de geolocalización (OpenStreetMap), solo a través de su puerto público. */
@Component
class UbicadorOpenStreetMap implements UbicadorDeDirecciones {

    private final Geocodificar geocodificar;

    UbicadorOpenStreetMap(Geocodificar geocodificar) {
        this.geocodificar = geocodificar;
    }

    @Override
    public Optional<Ubicacion> ubicar(String direccion) {
        return geocodificar.ubicar(direccion).map(c -> new Ubicacion(c.latitud(), c.longitud()));
    }
}
