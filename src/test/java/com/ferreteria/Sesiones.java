package com.ferreteria;

import com.ferreteria.shared.web.UsuarioAutenticado;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/** Usuarios de prueba con la misma forma que los de verdad (con id y sucursal). */
final class Sesiones {

    static final int SUCURSAL = 1;

    private Sesiones() {
    }

    static RequestPostProcessor admin() {
        return con(1, "admin", "admin", null);
    }

    static RequestPostProcessor vendedor() {
        return con(2, "ana", "vendedor", SUCURSAL);
    }

    static RequestPostProcessor domiciliario() {
        return con(9, "pedro", "domiciliario", SUCURSAL);
    }

    private static RequestPostProcessor con(int id, String nombre, String rol, Integer sucursalId) {
        return user(new UsuarioAutenticado(id, nombre, "x", true,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol)), sucursalId));
    }
}
