package com.ferreteria.sucursales.domain;

import com.ferreteria.shared.NegocioException;

public class SucursalDuplicadaException extends NegocioException {

    public SucursalDuplicadaException() {
        super("Ya existe una sucursal con ese nombre.");
    }
}
