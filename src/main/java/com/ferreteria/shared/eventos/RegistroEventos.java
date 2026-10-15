package com.ferreteria.shared.eventos;

/**
 * Puerto compartido: cualquier módulo puede dejar constancia de lo que pasa (ventas, descuentos,
 * pedidos...). Quién lo hizo y cuándo lo completa el adaptador; si la operación falla y se revierte,
 * el evento también.
 */
public interface RegistroEventos {

    /** @param sucursalId sucursal donde ocurrió, o {@code null} si no aplica */
    void registrar(TipoEvento tipo, Integer sucursalId, String descripcion);
}
