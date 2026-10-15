package com.ferreteria.ventas.infrastructure.in.web;

import com.ferreteria.ventas.domain.LineaVenta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Formulario de nueva venta: quién compra, líneas de productos y descuento elegido. */
public class VentaForm {

    private String clienteNombre = "";
    private String clienteTelefono = "";
    private List<LineaForm> lineas = new ArrayList<>();
    private BigDecimal descuentoElegido = BigDecimal.ZERO;
    private String medioPago = "efectivo";
    private BigDecimal montoRecibido;

    static VentaForm vacio(int cantidadLineas) {
        VentaForm f = new VentaForm();
        for (int i = 0; i < cantidadLineas; i++) {
            f.lineas.add(new LineaForm());
        }
        return f;
    }

    List<LineaVenta> lineasVenta() {
        return lineas.stream().map(l -> new LineaVenta(l.productoId, l.cantidad)).toList();
    }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }

    public List<LineaForm> getLineas() { return lineas; }
    public void setLineas(List<LineaForm> lineas) { this.lineas = lineas; }

    public BigDecimal getDescuentoElegido() { return descuentoElegido; }
    public void setDescuentoElegido(BigDecimal descuentoElegido) {
        this.descuentoElegido = descuentoElegido == null ? BigDecimal.ZERO : descuentoElegido;
    }

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago; }

    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal montoRecibido) { this.montoRecibido = montoRecibido; }

    public static class LineaForm {
        private int productoId;
        private Integer cantidad;

        public int getProductoId() { return productoId; }
        public void setProductoId(int productoId) { this.productoId = productoId; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }
}
