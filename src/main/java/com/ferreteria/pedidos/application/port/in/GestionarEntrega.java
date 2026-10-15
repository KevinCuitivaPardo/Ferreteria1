package com.ferreteria.pedidos.application.port.in;

import com.ferreteria.pedidos.domain.FotoEntrega;

/** Lo que hace el domiciliario con las entregas de su cola, una por una. */
public interface GestionarEntrega {

    enum Resultado { ENTREGADO, NO_ENTREGADO }

    /**
     * Sale a entregar el pedido. Solo puede llevar una entrega a la vez, y el pedido debe estar
     * asignado a él y todavía por entregar.
     *
     * @return el mensaje para mostrar al usuario
     */
    String comenzar(int pedidoId, int domiciliarioId);

    /**
     * Cierra la entrega en curso. Para "Entregado" la foto es obligatoria; para "No entregado"
     * hay que anotar el motivo y el stock vuelve a la sucursal.
     *
     * @param foto la foto de la entrega; puede ser {@code null} solo si no se pudo entregar
     * @return el mensaje para mostrar al usuario
     */
    String terminar(int pedidoId, int domiciliarioId, Resultado resultado, String nota, FotoEntrega foto);
}
