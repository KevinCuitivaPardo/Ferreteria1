package com.ferreteria.usuarios.application.port.in;

import com.ferreteria.usuarios.domain.Usuario;

import java.util.List;
import java.util.Optional;

public interface ConsultarUsuarios {

    List<Usuario> listar();

    List<Usuario> vendedores();

    Optional<Usuario> porNombre(String nombreUsuario);

    Optional<Usuario> porId(int usuarioId);

    /** Domiciliarios activos de una sucursal, para asignarles pedidos. */
    List<Usuario> domiciliarios(int sucursalId);
}
