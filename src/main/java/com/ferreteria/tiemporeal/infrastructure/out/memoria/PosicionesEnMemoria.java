package com.ferreteria.tiemporeal.infrastructure.out.memoria;

import com.ferreteria.tiemporeal.application.port.out.PosicionesActuales;
import com.ferreteria.tiemporeal.domain.PosicionDomiciliario;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda la última posición de cada domiciliario en memoria. Se pierde al reiniciar la aplicación
 * (los teléfonos vuelven a reportar solos en segundos); con varios servidores habría que cambiarla por
 * una base de datos o Redis, y solo se toca este adaptador.
 */
@Component
class PosicionesEnMemoria implements PosicionesActuales {

    private final ConcurrentHashMap<Integer, PosicionDomiciliario> ultimas = new ConcurrentHashMap<>();

    @Override
    public Optional<PosicionDomiciliario> de(int usuarioId) {
        return Optional.ofNullable(ultimas.get(usuarioId));
    }

    @Override
    public void guardar(PosicionDomiciliario posicion) {
        ultimas.put(posicion.usuarioId(), posicion);
    }

    @Override
    public List<PosicionDomiciliario> desde(Instant desde) {
        // De paso se limpian las que ya caducaron
        ultimas.values().removeIf(p -> p.actualizada().isBefore(desde));
        return ultimas.values().stream().toList();
    }
}
