package com.ferreteria.inventario.domain;

import com.ferreteria.shared.NegocioException;

public class CodigoProductoDuplicadoException extends NegocioException {

    public CodigoProductoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
