package com.ferreteria.inventario.domain;

public enum TipoMovimiento {
    ENTRADA, SALIDA;

    public static TipoMovimiento deClave(String clave) {
        return "salida".equals(clave) ? SALIDA : ENTRADA;
    }

    public String getClave() {
        return name().toLowerCase();
    }
}
