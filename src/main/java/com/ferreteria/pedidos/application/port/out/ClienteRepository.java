package com.ferreteria.pedidos.application.port.out;

import com.ferreteria.pedidos.domain.Cliente;
import com.ferreteria.pedidos.domain.DatosCliente;

import java.util.Optional;

/** Puerto de salida: almacenamiento de clientes. */
public interface ClienteRepository {

    Optional<Cliente> porCelular(String celular);

    Optional<Integer> idPorCelular(String celular);

    int crear(DatosCliente datos);
}
