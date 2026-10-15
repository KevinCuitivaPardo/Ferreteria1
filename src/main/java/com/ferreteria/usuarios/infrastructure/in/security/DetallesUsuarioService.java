package com.ferreteria.usuarios.infrastructure.in.security;

import com.ferreteria.shared.web.UsuarioAutenticado;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Le dice a Spring Security quién es cada usuario, leyéndolo a través del caso de uso de consulta. */
@Service
class DetallesUsuarioService implements UserDetailsService {

    private final ConsultarUsuarios usuarios;

    DetallesUsuarioService(ConsultarUsuarios usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String nombre) {
        return usuarios.porNombre(nombre.trim())
                .map(u -> new UsuarioAutenticado(u.id(), u.nombreUsuario(), u.claveHash(), u.activo(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.rol().getClave())), u.sucursalId()))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
