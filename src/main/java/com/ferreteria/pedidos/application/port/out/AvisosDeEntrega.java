package com.ferreteria.pedidos.application.port.out;

/**
 * Puerto de salida: avisos en tiempo real sobre los domicilios. Nunca deben impedir la operación:
 * si no hay nadie conectado simplemente no pasa nada.
 */
public interface AvisosDeEntrega {

    /** Le llega al domiciliario una alerta de que le asignaron un pedido. */
    void pedidoAsignado(int domiciliarioId, int pedidoId, String numero, String cliente, String direccion);

    /** A un domiciliario se le quitó un pedido de la cola (reasignado o desasignado). */
    void pedidoRetirado(int domiciliarioId, int pedidoId, String numero);

    /** A los administradores les llega un aviso (pedido nuevo por asignar, entrega terminada...). */
    void avisarAAdmins(String titulo, String mensaje, String enlace);
}
