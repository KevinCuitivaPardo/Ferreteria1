package com.ferreteria.geolocalizacion.infrastructure.in.web;

import com.ferreteria.geolocalizacion.application.port.in.Geocodificar;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Lo usan los formularios para mostrar en el mapa dónde queda una dirección. */
@RestController
class GeocodificarController {

    private final Geocodificar geocodificar;

    GeocodificarController(Geocodificar geocodificar) {
        this.geocodificar = geocodificar;
    }

    @GetMapping("/api/geocodificar")
    Map<String, Object> buscar(@RequestParam(defaultValue = "") String q) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        var encontrada = geocodificar.ubicar(q);

        respuesta.put("encontrado", encontrada.isPresent());
        encontrada.ifPresent(c -> {
            respuesta.put("latitud", c.latitud());
            respuesta.put("longitud", c.longitud());
            respuesta.put("descripcion", c.descripcion());
        });
        return respuesta;
    }
}
