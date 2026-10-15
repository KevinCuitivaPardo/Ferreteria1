package com.ferreteria.geolocalizacion.infrastructure.out.osm;

import com.fasterxml.jackson.databind.JsonNode;
import com.ferreteria.geolocalizacion.application.port.out.BuscadorDirecciones;
import com.ferreteria.geolocalizacion.domain.Coordenadas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Busca direcciones en OpenStreetMap con Nominatim (gratis). Su política de uso pide identificarse con
 * un User-Agent y no pasar de una consulta por segundo, por eso se espacian y se guardan las respuestas.
 */
@Component
class NominatimBuscador implements BuscadorDirecciones {

    private static final Logger log = LoggerFactory.getLogger(NominatimBuscador.class);
    private static final long ESPERA_ENTRE_CONSULTAS_MS = 1_100;

    private final RestClient cliente;
    private final String pais;
    private final ConcurrentHashMap<String, Optional<Coordenadas>> recordadas = new ConcurrentHashMap<>();
    private long ultimaConsulta;

    NominatimBuscador(@Value("${ferreteria.geocodificador.url}") String url,
                      @Value("${ferreteria.geocodificador.pais:}") String pais,
                      @Value("${ferreteria.geocodificador.contacto:ferreteria-app}") String contacto) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3_000);
        fabrica.setReadTimeout(5_000);

        this.pais = pais;
        this.cliente = RestClient.builder()
                .baseUrl(url)
                .requestFactory(fabrica)
                .defaultHeader("User-Agent", "FerreteriaApp/1.0 (" + contacto + ")")
                .defaultHeader("Accept-Language", "es")
                .build();
    }

    @Override
    public Optional<Coordenadas> buscar(String direccion) {
        String clave = direccion.toLowerCase(Locale.ROOT);
        Optional<Coordenadas> guardada = recordadas.get(clave);
        if (guardada != null) {
            return guardada;
        }

        Optional<Coordenadas> encontrada = Optional.empty();
        for (String consulta : DireccionesColombianas.consultas(direccion)) {
            encontrada = consultar(consulta);
            if (encontrada.isPresent()) {
                break;
            }
        }
        // Solo se recuerdan los aciertos: un fallo puede ser pasajero.
        encontrada.ifPresent(c -> recordadas.put(clave, Optional.of(c)));
        return encontrada;
    }

    private Optional<Coordenadas> consultar(String direccion) {
        esperarTurno();
        try {
            JsonNode respuesta = cliente.get()
                    .uri(uri -> {
                        var constructor = uri.path("/search")
                                .queryParam("format", "jsonv2")
                                .queryParam("limit", 1)
                                .queryParam("q", direccion);
                        if (!pais.isBlank()) {
                            constructor.queryParam("countrycodes", pais);
                        }
                        return constructor.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);

            if (respuesta == null || !respuesta.isArray() || respuesta.isEmpty()) {
                return Optional.empty();
            }
            JsonNode primero = respuesta.get(0);
            return Optional.of(new Coordenadas(
                    primero.path("lat").asDouble(),
                    primero.path("lon").asDouble(),
                    primero.path("display_name").asText("")));
        } catch (RestClientException e) {
            log.warn("No se pudo consultar Nominatim: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized void esperarTurno() {
        long falta = ESPERA_ENTRE_CONSULTAS_MS - (System.currentTimeMillis() - ultimaConsulta);
        if (falta > 0) {
            try {
                Thread.sleep(falta);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        ultimaConsulta = System.currentTimeMillis();
    }
}
