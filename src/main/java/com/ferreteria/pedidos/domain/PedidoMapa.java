package com.ferreteria.pedidos.domain;

/** Un pedido ubicado en el mapa del administrador. */
public record PedidoMapa(
        int id,
        String numero,
        String estado,
        String cliente,
        String direccion,
        double latitud,
        double longitud,
        String sucursal,
        String domiciliario) {
}
