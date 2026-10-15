package com.ferreteria.pedidos.infrastructure.out.usuarios;

import com.ferreteria.pedidos.application.port.out.Domiciliarios;
import com.ferreteria.pedidos.domain.Domiciliario;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import com.ferreteria.usuarios.domain.Rol;
import com.ferreteria.usuarios.domain.Usuario;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Conecta los pedidos con el módulo de usuarios para saber quiénes son los domiciliarios. */
@Component
class UsuariosDomiciliariosAdapter implements Domiciliarios {

    private final ConsultarUsuarios usuarios;

    UsuariosDomiciliariosAdapter(ConsultarUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public Optional<Domiciliario> porId(int usuarioId) {
        return usuarios.porId(usuarioId)
                .filter(u -> u.rol() == Rol.DOMICILIARIO)
                .map(UsuariosDomiciliariosAdapter::aDomiciliario);
    }

    @Override
    public List<Domiciliario> activosDe(int sucursalId) {
        return usuarios.domiciliarios(sucursalId).stream()
                .map(UsuariosDomiciliariosAdapter::aDomiciliario).toList();
    }

    private static Domiciliario aDomiciliario(Usuario u) {
        return new Domiciliario(u.id(), u.nombreUsuario(), u.activo(), u.sucursalId());
    }
}
