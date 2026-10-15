package com.ferreteria.tiemporeal.infrastructure.in.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Abre el canal {@code /ws}. Solo entra quien tenga sesión iniciada (la seguridad lo exige) y, por defecto,
 * solo desde páginas del mismo sitio.
 */
@Configuration
@EnableWebSocket
class ConfiguracionWebSocket implements WebSocketConfigurer {

    private final ManejadorTiempoReal manejador;

    ConfiguracionWebSocket(ManejadorTiempoReal manejador) {
        this.manejador = manejador;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
        registro.addHandler(manejador, "/ws");
    }
}
