package com.ferreteria.sucursales.infrastructure.in.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
class SucursalWebConfig implements WebMvcConfigurer {

    private final SucursalActualResolver resolver;

    SucursalWebConfig(SucursalActualResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}
