package com.ferreteria.usuarios.domain;

import java.math.BigDecimal;

/**
 * @param sucursalId sucursal a la que pertenece; es nula para el administrador, que trabaja en todas
 */
public record Usuario(
        int id,
        String nombreUsuario,
        String claveHash,
        Rol rol,
        boolean activo,
        BigDecimal descuentoPct,
        Integer sucursalId) {

    /** El administrador principal no se puede desactivar. */
    public static final String NOMBRE_ADMIN_PRINCIPAL = "admin";

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    public boolean esAdminPrincipal() {
        return NOMBRE_ADMIN_PRINCIPAL.equals(nombreUsuario);
    }
}
