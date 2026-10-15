package com.ferreteria.tiemporeal.domain;

/**
 * Una alerta para una persona conectada.
 *
 * @param tipo    para qué sirve en pantalla: "asignacion" (le asignaron un pedido), "cola" (cambió su lista),
 *                "admin" (aviso para el administrador)
 * @param enlace  a dónde lleva al tocarla (una ruta de la aplicación), o nulo
 */
public record Aviso(String tipo, String titulo, String mensaje, String enlace) {
}
