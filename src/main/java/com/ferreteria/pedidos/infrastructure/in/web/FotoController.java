package com.ferreteria.pedidos.infrastructure.in.web;

import com.ferreteria.pedidos.application.port.in.ConsultarPedidos;
import com.ferreteria.pedidos.domain.ContenidoFoto;
import com.ferreteria.shared.web.Sesion;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Optional;

/** Muestra las fotos de entrega a quien tiene derecho a verlas (admin, vendedor dueño, domiciliario asignado). */
@RestController
class FotoController {

    private final ConsultarPedidos consulta;

    FotoController(ConsultarPedidos consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/Fotos/{pedidoId}/{fotoId}")
    ResponseEntity<byte[]> foto(@PathVariable int pedidoId, @PathVariable int fotoId, Authentication auth) {
        Optional<ContenidoFoto> foto = consulta.foto(pedidoId, fotoId, Sesion.usuarioId(auth), Sesion.esAdmin(auth));

        return foto.map(f -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(f.tipoContenido()))
                        .header("X-Content-Type-Options", "nosniff")
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                        .body(f.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
