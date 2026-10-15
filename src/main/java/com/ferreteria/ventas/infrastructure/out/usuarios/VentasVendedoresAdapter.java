package com.ferreteria.ventas.infrastructure.out.usuarios;

import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import com.ferreteria.ventas.application.port.out.Vendedores;
import com.ferreteria.ventas.domain.Vendedor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Conecta las ventas con el módulo de usuarios, solo a través de su puerto público. */
@Component
class VentasVendedoresAdapter implements Vendedores {

    private final ConsultarUsuarios usuarios;

    VentasVendedoresAdapter(ConsultarUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<Vendedor> porNombre(String nombreUsuario) {
        return usuarios.porNombre(nombreUsuario)
                .map(u -> new Vendedor(u.id(), u.activo(), u.descuentoPct()));
    }
}
