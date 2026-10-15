package com.ferreteria.pedidos.infrastructure.out.usuarios;

import com.ferreteria.pedidos.application.port.out.Vendedores;
import com.ferreteria.pedidos.domain.Vendedor;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Conecta los pedidos con el módulo de usuarios, solo a través de su puerto público. */
@Component
class UsuariosVendedoresAdapter implements Vendedores {

    private final ConsultarUsuarios usuarios;

    UsuariosVendedoresAdapter(ConsultarUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<Vendedor> porNombre(String nombreUsuario) {
        return usuarios.porNombre(nombreUsuario)
                .map(u -> new Vendedor(u.id(), u.activo(), u.descuentoPct()));
    }
}
