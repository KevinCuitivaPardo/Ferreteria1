package com.ferreteria.shared.eventos;

/** Clases de hechos que quedan registrados para que el administrador los consulte. */
public enum TipoEvento {
    VENTA("Venta"),
    PEDIDO("Pedido"),
    ESTADO_PEDIDO("Estado de pedido"),
    ASIGNACION("Asignación de domicilio"),
    DESCUENTO("Descuento"),
    INVENTARIO("Inventario"),
    PRODUCTO("Producto"),
    USUARIO("Usuario"),
    SUCURSAL("Sucursal");

    private final String etiqueta;

    TipoEvento(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
