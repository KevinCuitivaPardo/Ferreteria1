package com.ferreteria.pedidos.application.port.in;

public interface CorregirUbicacion {

    /**
     * Fija a mano el punto de entrega de un pedido. Sirve cuando el mapa no encontró la dirección
     * o la ubicó solo de forma aproximada.
     *
     * @return el mensaje para mostrar al usuario
     * @throws com.ferreteria.shared.NegocioException si el pedido no existe o el punto no es válido
     */
    String corregir(int pedidoId, double latitud, double longitud);
}
