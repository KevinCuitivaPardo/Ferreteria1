package com.ferreteria.pedidos.domain;

import java.util.Arrays;
import java.util.Optional;

public enum EstadoPedido {
    PENDIENTE("pendiente", "Pendiente"),
    ALISTADO("alistado", "Alistado"),
    EN_RUTA("en_ruta", "En ruta"),
    ENTREGADO("entregado", "Entregado"),
    NO_ENTREGADO("no_entregado", "No entregado"),
    CANCELADO("cancelado", "Cancelado");

    private final String clave;
    private final String etiqueta;

    EstadoPedido(String clave, String etiqueta) {
        this.clave = clave;
        this.etiqueta = etiqueta;
    }

    public String getClave() {
        return clave;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Estados en los que el stock vuelve al inventario. */
    public boolean devuelveStock() {
        return this == NO_ENTREGADO || this == CANCELADO;
    }

    /** Estados que el domiciliario puede poner a sus entregas. */
    public boolean loPuedePonerElDomiciliario() {
        return this == EN_RUTA || this == ENTREGADO || this == NO_ENTREGADO;
    }

    /** Estados en los que el pedido todavía está por entregar. */
    public boolean estaPorEntregar() {
        return this == PENDIENTE || this == ALISTADO || this == EN_RUTA;
    }

    public static Optional<EstadoPedido> deClave(String clave) {
        return Arrays.stream(values()).filter(e -> e.clave.equals(clave)).findFirst();
    }

    /** Etiqueta legible; si la clave no existe devuelve la clave tal cual. */
    public static String etiquetaDe(String clave) {
        return deClave(clave).map(EstadoPedido::getEtiqueta).orElse(clave);
    }
}
