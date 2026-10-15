package com.ferreteria.usuarios.infrastructure.out.security;

import com.ferreteria.usuarios.application.port.out.CodificadorClave;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Guarda las contraseñas con BCrypt (compatible con los hashes de la versión anterior). */
@Component
class BCryptCodificadorClave implements CodificadorClave {

    private final PasswordEncoder encoder;

    BCryptCodificadorClave(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String codificar(String claveEnClaro) {
        return encoder.encode(claveEnClaro);
    }
}
