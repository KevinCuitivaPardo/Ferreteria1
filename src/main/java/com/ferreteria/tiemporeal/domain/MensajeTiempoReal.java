package com.ferreteria.tiemporeal.domain;

/** Lo que viaja por el canal de tiempo real: un tipo ("posicion", "aviso"...) y sus datos. */
public record MensajeTiempoReal(String tipo, Object datos) {
}
