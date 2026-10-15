package com.ferreteria.tiemporeal.infrastructure.in.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferreteria.shared.web.Sesion;
import com.ferreteria.shared.web.UsuarioAutenticado;
import com.ferreteria.tiemporeal.application.port.in.ReportarPosicion;
import com.ferreteria.tiemporeal.infrastructure.out.websocket.RegistroSesiones;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * Adaptador de entrada del tiempo real. Cada pantalla abre una conexión {@code /ws} con su sesión iniciada.
 * El domiciliario manda su posición {@code {"tipo":"posicion","latitud":..,"longitud":..,"precision":..}};
 * los demás solo reciben avisos. Lo que llegue de alguien que no es domiciliario se ignora.
 */
@Component
public class ManejadorTiempoReal extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ManejadorTiempoReal.class);
    private static final String USUARIO = "usuario";
    /** Una posición ocupa unos 100 caracteres; lo que pase de esto no es legítimo y se ignora. */
    private static final int LARGO_MAXIMO = 2048;

    private final RegistroSesiones sesiones;
    private final ReportarPosicion posiciones;
    private final ObjectMapper json;

    ManejadorTiempoReal(RegistroSesiones sesiones, ReportarPosicion posiciones, ObjectMapper json) {
        this.sesiones = sesiones;
        this.posiciones = posiciones;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) throws IOException {
        Authentication auth = sesion.getPrincipal() instanceof Authentication a ? a : null;
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            sesion.close(CloseStatus.POLICY_VIOLATION.withReason("Sin sesión"));
            return;
        }

        sesion.getAttributes().put(USUARIO, new Conectado(usuario.getId(), usuario.getUsername(),
                usuario.getSucursalId(), Sesion.esDomiciliario(auth)));
        sesiones.agregar(sesion, usuario.getId(), Sesion.esAdmin(auth));
    }

    @Override
    protected void handleTextMessage(WebSocketSession sesion, TextMessage mensaje) {
        Object guardado = sesion.getAttributes().get(USUARIO);
        if (!(guardado instanceof Conectado quien) || !quien.esDomiciliario()
                || mensaje.getPayloadLength() > LARGO_MAXIMO) {
            return;
        }

        try {
            JsonNode datos = json.readTree(mensaje.getPayload());
            if (!"posicion".equals(datos.path("tipo").asText())
                    || !datos.path("latitud").isNumber() || !datos.path("longitud").isNumber()) {
                return;
            }
            Double precision = datos.path("precision").isNumber() ? datos.path("precision").asDouble() : null;
            posiciones.reportar(quien.id(), quien.nombre(), quien.sucursalId(),
                    datos.path("latitud").asDouble(), datos.path("longitud").asDouble(), precision);
        } catch (IOException e) {
            log.debug("Mensaje de tiempo real ilegible: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
        if (sesion.getAttributes().get(USUARIO) instanceof Conectado quien) {
            sesiones.quitar(sesion, quien.id());
        }
    }

    private record Conectado(int id, String nombre, Integer sucursalId, boolean esDomiciliario) {
    }
}
