package com.ferreteria.sucursales.application.port.in;

import com.ferreteria.sucursales.domain.DatosSucursal;

public interface GestionarSucursales {

    void crear(DatosSucursal datos);

    void actualizar(int sucursalId, DatosSucursal datos);

    /** Activa o desactiva una sucursal (una sucursal desactivada no aparece para trabajar en ella). */
    void alternarActiva(int sucursalId);
}
