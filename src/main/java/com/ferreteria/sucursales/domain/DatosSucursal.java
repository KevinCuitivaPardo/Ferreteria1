package com.ferreteria.sucursales.domain;

/** Lo que se captura al crear o editar una sucursal. La ubicación es el punto marcado en el mapa. */
public record DatosSucursal(String nombre, String direccion, Double latitud, Double longitud) {
}
