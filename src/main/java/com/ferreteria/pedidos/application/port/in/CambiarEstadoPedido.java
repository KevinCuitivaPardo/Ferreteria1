package com.ferreteria.pedidos.application.port.in;

public interface CambiarEstadoPedido {

    /**
     * Cambio hecho por el administrador, a cualquier estado. Si el pedido pasa a "No entregado" o
     * "Cancelado" el stock vuelve al inventario; si vuelve a un estado activo se descuenta otra vez
     * (cuando hay existencias).
     *
     * @return el mensaje para mostrar al usuario
     */
    String cambiarEstado(int pedidoId, String claveEstado);
}
