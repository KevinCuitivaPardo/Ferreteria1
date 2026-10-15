package com.ferreteria.inventario.domain;

import java.time.LocalDateTime;

/** Entrada (cantidad positiva) o salida (negativa) del inventario de un producto. */
public record Movimiento(int cantidad, String motivo, LocalDateTime fecha) {

    public boolean esEntrada() {
        return cantidad >= 0;
    }

    public int cantidadAbsoluta() {
        return Math.abs(cantidad);
    }
}
