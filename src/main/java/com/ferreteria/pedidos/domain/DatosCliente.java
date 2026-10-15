package com.ferreteria.pedidos.domain;

/** Datos del cliente tal como se capturan en el pedido. */
public record DatosCliente(
        String celular1,
        String celular2,
        String nombreFerreteria,
        String nombreCliente,
        String direccion,
        String ubicacion,
        String horario) {
}
