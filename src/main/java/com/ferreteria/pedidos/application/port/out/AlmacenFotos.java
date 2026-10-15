package com.ferreteria.pedidos.application.port.out;

import com.ferreteria.pedidos.domain.FotoEntrega;

import java.util.Optional;

/** Puerto de salida: dónde se guardan los archivos de las fotos (hoy el disco; mañana podría ser la nube). */
public interface AlmacenFotos {

    /** Guarda la foto y devuelve el nombre con el que quedó. */
    String guardar(FotoEntrega foto);

    Optional<byte[]> leer(String archivo);

    /** Borra el archivo; se usa para no dejar fotos huérfanas si algo falla después de guardarlas. */
    void borrar(String archivo);
}
