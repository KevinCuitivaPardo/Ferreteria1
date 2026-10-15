package com.ferreteria.sucursales.application.port.in;

import com.ferreteria.sucursales.domain.Sucursal;

import java.util.List;
import java.util.Optional;

public interface ConsultarSucursales {

    List<Sucursal> listar();

    List<Sucursal> activas();

    Optional<Sucursal> porId(int sucursalId);
}
