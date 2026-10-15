package com.ferreteria.shared;

import java.util.List;

/**
 * Error de una regla de negocio. Es la única excepción que cruza los límites entre capas:
 * el mensaje está pensado para mostrarse tal cual al usuario.
 */
public class NegocioException extends RuntimeException {

    private final List<String> mensajes;

    public NegocioException(String mensaje) {
        super(mensaje);
        this.mensajes = List.of(mensaje);
    }

    public NegocioException(List<String> mensajes) {
        super(String.join(" ", mensajes));
        this.mensajes = List.copyOf(mensajes);
    }

    public List<String> getMensajes() {
        return mensajes;
    }
}
