package com.ferreteria.usuarios.application.port.out;

/** Puerto de salida: cómo se guarda una contraseña (hash). */
public interface CodificadorClave {

    String codificar(String claveEnClaro);
}
