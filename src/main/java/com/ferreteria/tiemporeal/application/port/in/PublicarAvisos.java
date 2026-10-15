package com.ferreteria.tiemporeal.application.port.in;

import com.ferreteria.tiemporeal.domain.Aviso;

/** Lo que ofrece el módulo a los demás para avisar en el momento a quien esté conectado. */
public interface PublicarAvisos {

    /** Se envía a todas las pantallas abiertas de esa persona; si no está conectada, no pasa nada. */
    void avisarAUsuario(int usuarioId, Aviso aviso);

    /** Se envía a todos los administradores conectados. */
    void avisarAAdmins(Aviso aviso);
}
