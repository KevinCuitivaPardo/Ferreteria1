package com.ferreteria.tiemporeal.application.port.out;

import com.ferreteria.tiemporeal.domain.MensajeTiempoReal;

/** Puerto de salida: el canal por el que se empuja un mensaje a quien está conectado (hoy, WebSocket). */
public interface CanalTiempoReal {

    void enviarAUsuario(int usuarioId, MensajeTiempoReal mensaje);

    void enviarAAdmins(MensajeTiempoReal mensaje);
}
