package com.ferreteria.ventas.application.port.out;

import com.ferreteria.ventas.domain.Vendedor;

import java.util.Optional;

/** Puerto de salida: quién vende y cuánto descuento puede dar (lo provee el módulo de usuarios). */
public interface Vendedores {

    Optional<Vendedor> porNombre(String nombreUsuario);
}
