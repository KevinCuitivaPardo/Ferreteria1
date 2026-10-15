package com.ferreteria.tiemporeal.infrastructure.out.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ferreteria.tiemporeal.application.port.out.CanalTiempoReal;
import com.ferreteria.tiemporeal.domain.MensajeTiempoReal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Lleva la cuenta de quién está conectado por WebSocket (una persona puede tener varias pantallas abiertas)
 * y es el adaptador de salida: empuja los mensajes a esas conexiones.
 */
@Component
public class RegistroSesiones implements CanalTiempoReal {

    private static final Logger log = LoggerFactory.getLogger(RegistroSesiones.class);
    private static final int TIEMPO_MAXIMO_ENVIO_MS = 5_000;
    private static final int BUFFER_MAXIMO_BYTES = 128 * 1024;

    private final ObjectMapper json;
    private final Map<Integer, Set<WebSocketSession>> porUsuario = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> administradores = new CopyOnWriteArraySet<>();

    RegistroSesiones(ObjectMapper json) {
        this.json = json;
    }

    /** Registra una conexión nueva y devuelve la versión segura para enviar desde varios hilos. */
    public WebSocketSession agregar(WebSocketSession sesion, int usuarioId, boolean esAdmin) {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sesion, TIEMPO_MAXIMO_ENVIO_MS,
                BUFFER_MAXIMO_BYTES);
        porUsuario.computeIfAbsent(usuarioId, id -> new CopyOnWriteArraySet<>()).add(segura);
        if (esAdmin) {
            administradores.add(segura);
        }
        return segura;
    }

    public void quitar(WebSocketSession sesion, int usuarioId) {
        Set<WebSocketSession> sesiones = porUsuario.get(usuarioId);
        if (sesiones != null) {
            sesiones.removeIf(s -> mismaConexion(s, sesion));
            if (sesiones.isEmpty()) {
                porUsuario.remove(usuarioId);
            }
        }
        administradores.removeIf(s -> mismaConexion(s, sesion));
    }

    @Override
    public void enviarAUsuario(int usuarioId, MensajeTiempoReal mensaje) {
        enviar(porUsuario.getOrDefault(usuarioId, Set.of()), mensaje);
    }

    @Override
    public void enviarAAdmins(MensajeTiempoReal mensaje) {
        enviar(administradores, mensaje);
    }

    /** Cuántas conexiones hay abiertas (para pruebas y diagnóstico). */
    public int conexiones() {
        return porUsuario.values().stream().mapToInt(Set::size).sum();
    }

    private void enviar(Set<WebSocketSession> destinos, MensajeTiempoReal mensaje) {
        if (destinos.isEmpty()) {
            return;
        }
        TextMessage texto;
        try {
            texto = new TextMessage(json.writeValueAsString(mensaje));
        } catch (JsonProcessingException e) {
            log.warn("No se pudo preparar el mensaje de tiempo real: {}", e.getMessage());
            return;
        }

        for (WebSocketSession sesion : destinos) {
            try {
                if (sesion.isOpen()) {
                    sesion.sendMessage(texto);
                }
            } catch (IOException | RuntimeException e) {
                // Una conexión rota no debe afectar a las demás ni a la operación que originó el aviso
                log.debug("No se pudo enviar a una conexión: {}", e.getMessage());
                try {
                    sesion.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (IOException ignorado) {
                    // ya estaba cerrada
                }
            }
        }
    }

    private static boolean mismaConexion(WebSocketSession guardada, WebSocketSession buscada) {
        return guardada.getId().equals(buscada.getId());
    }
}
