package com.ferreteria.sucursales.domain;

public record Sucursal(
        int id,
        String nombre,
        String direccion,
        Double latitud,
        Double longitud,
        boolean activa) {

    public boolean tieneUbicacion() {
        return latitud != null && longitud != null;
    }
}
