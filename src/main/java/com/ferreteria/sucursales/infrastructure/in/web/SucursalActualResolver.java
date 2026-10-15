package com.ferreteria.sucursales.infrastructure.in.web;

import com.ferreteria.shared.web.SucursalActual;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/** Rellena los parámetros {@code @SucursalActual int sucursalId} de cualquier controlador. */
@Component
class SucursalActualResolver implements HandlerMethodArgumentResolver {

    private final ContextoSucursal contexto;

    SucursalActualResolver(ContextoSucursal contexto) {
        this.contexto = contexto;
    }

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.hasParameterAnnotation(SucursalActual.class)
                && (parametro.getParameterType() == int.class || parametro.getParameterType() == Integer.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer vista,
                                  NativeWebRequest peticion, WebDataBinderFactory fabrica) {
        HttpServletRequest http = peticion.getNativeRequest(HttpServletRequest.class);
        var sesion = http == null ? null : http.getSession(false);

        return contexto.resolver(SecurityContextHolder.getContext().getAuthentication(), sesion)
                .map(s -> s.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Tu usuario no tiene una sucursal asignada."));
    }
}
