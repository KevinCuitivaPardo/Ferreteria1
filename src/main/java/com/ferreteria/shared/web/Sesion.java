package com.ferreteria.shared.web;

import org.springframework.security.core.Authentication;

/** Ayudas para leer la sesión desde los controladores. */
public final class Sesion {

    private Sesion() {
    }

    public static boolean esAdmin(Authentication auth) {
        return tieneRol(auth, "admin");
    }

    public static boolean esVendedor(Authentication auth) {
        return tieneRol(auth, "vendedor");
    }

    public static boolean esDomiciliario(Authentication auth) {
        return tieneRol(auth, "domiciliario");
    }

    /** Id del usuario con sesión iniciada. */
    public static int usuarioId(Authentication auth) {
        return ((UsuarioAutenticado) auth.getPrincipal()).getId();
    }

    private static boolean tieneRol(Authentication auth, String rol) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
    }
}
