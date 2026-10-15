package com.ferreteria.pedidos.domain;

public record Cliente(
        String nombreFerreteria,
        String nombreCliente,
        String direccion,
        String ubicacion,
        String horario,
        String celular2) {
}
