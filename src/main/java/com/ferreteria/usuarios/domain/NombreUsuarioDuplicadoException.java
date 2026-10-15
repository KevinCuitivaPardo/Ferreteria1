package com.ferreteria.usuarios.domain;

import com.ferreteria.shared.NegocioException;

public class NombreUsuarioDuplicadoException extends NegocioException {

    public NombreUsuarioDuplicadoException() {
        super("Ya existe un usuario con ese nombre.");
    }
}
