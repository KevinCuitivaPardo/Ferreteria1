package com.ferreteria.shared.web;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/** La persona que inició sesión, con su id y la sucursal a la que pertenece (nula para el admin). */
public class UsuarioAutenticado extends User {

    private final int id;
    private final Integer sucursalId;

    public UsuarioAutenticado(int id, String nombreUsuario, String claveHash, boolean activo,
                              Collection<? extends GrantedAuthority> permisos, Integer sucursalId) {
        super(nombreUsuario, claveHash, activo, true, true, true, permisos);
        this.id = id;
        this.sucursalId = sucursalId;
    }

    public int getId() {
        return id;
    }

    public Integer getSucursalId() {
        return sucursalId;
    }
}
