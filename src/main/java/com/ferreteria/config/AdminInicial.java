package com.ferreteria.config;

import com.ferreteria.usuarios.application.port.in.GestionarUsuarios;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar crea el primer administrador si todavía no hay usuarios. */
@Component
class AdminInicial implements ApplicationRunner {

    private final GestionarUsuarios usuarios;
    private final String usuario;
    private final String clave;

    AdminInicial(GestionarUsuarios usuarios,
                 @Value("${ferreteria.primer-admin.usuario:}") String usuario,
                 @Value("${ferreteria.primer-admin.clave:}") String clave) {
        this.usuarios = usuarios;
        this.usuario = usuario;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments args) {
        usuarios.crearAdminInicialSiHaceFalta(usuario, clave);
    }
}
