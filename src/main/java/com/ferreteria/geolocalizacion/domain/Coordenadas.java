package com.ferreteria.geolocalizacion.domain;

/** Un punto en el mapa y el nombre con el que se encontró. */
public record Coordenadas(double latitud, double longitud, String descripcion) {
}
