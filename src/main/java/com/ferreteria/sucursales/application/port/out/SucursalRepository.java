package com.ferreteria.sucursales.application.port.out;

import com.ferreteria.sucursales.domain.DatosSucursal;
import com.ferreteria.sucursales.domain.Sucursal;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de sucursales. */
public interface SucursalRepository {

    List<Sucursal> listar();

    Optional<Sucursal> porId(int sucursalId);

    /** @throws com.ferreteria.sucursales.domain.SucursalDuplicadaException si el nombre ya existe */
    void insertar(DatosSucursal datos);

    /** @throws com.ferreteria.sucursales.domain.SucursalDuplicadaException si el nombre ya existe */
    void actualizar(int sucursalId, DatosSucursal datos);

    void alternarActiva(int sucursalId);
}
