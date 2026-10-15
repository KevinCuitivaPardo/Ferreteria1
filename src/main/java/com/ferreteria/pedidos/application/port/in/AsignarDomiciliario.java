package com.ferreteria.pedidos.application.port.in;

public interface AsignarDomiciliario {

    /**
     * Asigna el pedido a un domiciliario de su misma sucursal. Con {@code domiciliarioId} en 0
     * se quita la asignación.
     *
     * @return el mensaje para mostrar al usuario
     */
    String asignar(int pedidoId, int domiciliarioId);
}
