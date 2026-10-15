package com.ferreteria.pedidos.domain;

import java.time.LocalDateTime;

/** Datos de una foto de la entrega (el archivo se pide aparte). */
public record FotoPedido(int id, int pedidoId, String tipoContenido, int tamano, String subidaPor,
                         LocalDateTime fecha) {
}
