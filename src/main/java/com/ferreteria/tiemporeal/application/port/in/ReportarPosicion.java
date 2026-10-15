package com.ferreteria.tiemporeal.application.port.in;

public interface ReportarPosicion {

    /**
     * Registra dónde está un domiciliario y se lo muestra en vivo a los administradores. Si el punto
     * no es válido o llega demasiado seguido se ignora, sin error.
     */
    void reportar(int usuarioId, String nombre, Integer sucursalId, double latitud, double longitud,
                  Double precisionMetros);
}
