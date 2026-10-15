package com.ferreteria.sucursales.infrastructure.in.web;

import com.ferreteria.shared.web.Sesion;
import com.ferreteria.shared.web.UsuarioAutenticado;
import com.ferreteria.sucursales.application.port.in.ConsultarSucursales;
import com.ferreteria.sucursales.domain.Sucursal;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Decide en qué sucursal se está trabajando. El vendedor y el domiciliario trabajan en la suya;
 * el administrador elige una en la barra superior (queda guardada en su sesión).
 * Las vistas lo usan como {@code ${@contextoSucursal.activa()}}.
 */
@Component("contextoSucursal")
public class ContextoSucursal {

    static final String CLAVE_SESION = "sucursalActiva";

    private final ConsultarSucursales sucursales;

    ContextoSucursal(ConsultarSucursales sucursales) {
        this.sucursales = sucursales;
    }

    Optional<Sucursal> resolver(Authentication auth, HttpSession session) {
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            return Optional.empty();
        }

        if (!Sesion.esAdmin(auth)) {
            return usuario.getSucursalId() == null
                    ? Optional.empty()
                    : sucursales.porId(usuario.getSucursalId());
        }

        Object elegida = session == null ? null : session.getAttribute(CLAVE_SESION);
        if (elegida instanceof Integer id) {
            Optional<Sucursal> sucursal = sucursales.porId(id).filter(Sucursal::activa);
            if (sucursal.isPresent()) {
                return sucursal;
            }
        }
        return sucursales.activas().stream().findFirst();
    }

    // ---------- Para las plantillas ----------

    public Sucursal activa() {
        return resolver(autenticacion(), sesionActual()).orElse(null);
    }

    /** Las sucursales entre las que puede elegir el administrador. */
    public List<Sucursal> activas() {
        return sucursales.activas();
    }

    private static Authentication autenticacion() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static HttpSession sesionActual() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos
                ? atributos.getRequest().getSession(false)
                : null;
    }
}
