package com.ferreteria.pedidos.domain;

import com.ferreteria.shared.NegocioException;

/** El domiciliario ya tiene otra entrega en ruta: solo puede llevar una a la vez. */
public class EntregaEnCursoException extends NegocioException {

    public EntregaEnCursoException() {
        super("Ese domiciliario ya tiene una entrega en curso. Debe terminarla antes de comenzar otra.");
    }
}
