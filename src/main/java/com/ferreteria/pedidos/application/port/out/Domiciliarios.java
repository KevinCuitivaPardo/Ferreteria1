package com.ferreteria.pedidos.application.port.out;

import com.ferreteria.pedidos.domain.Domiciliario;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: quién entrega (lo provee el módulo de usuarios). Solo devuelve usuarios con rol de domiciliario. */
public interface Domiciliarios {

    Optional<Domiciliario> porId(int usuarioId);

    List<Domiciliario> activosDe(int sucursalId);
}
