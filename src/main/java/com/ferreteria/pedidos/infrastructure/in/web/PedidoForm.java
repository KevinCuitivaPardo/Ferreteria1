package com.ferreteria.pedidos.infrastructure.in.web;

import com.ferreteria.pedidos.domain.DatosCliente;
import com.ferreteria.pedidos.domain.LineaPedido;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Formulario de nuevo pedido: cliente, líneas de productos y descuento elegido. */
public class PedidoForm {

    private ClienteForm cliente = new ClienteForm();
    private List<LineaForm> lineas = new ArrayList<>();
    private BigDecimal descuentoElegido = BigDecimal.ZERO;
    private String medioPago = "efectivo";
    private BigDecimal montoRecibido;

    static PedidoForm vacio(int cantidadLineas) {
        PedidoForm f = new PedidoForm();
        for (int i = 0; i < cantidadLineas; i++) {
            f.lineas.add(new LineaForm());
        }
        return f;
    }

    DatosCliente datosCliente() {
        return new DatosCliente(cliente.celular1, cliente.celular2, cliente.nombreFerreteria,
                cliente.nombreCliente, cliente.direccion, cliente.ubicacion, cliente.horario);
    }

    List<LineaPedido> lineasPedido() {
        return lineas.stream().map(l -> new LineaPedido(l.productoId, l.cantidad)).toList();
    }

    public ClienteForm getCliente() { return cliente; }
    public void setCliente(ClienteForm cliente) { this.cliente = cliente; }

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

    public static class ClienteForm {
        private String celular1 = "";
        private String celular2;
        private String nombreFerreteria;
        private String nombreCliente = "";
        private String direccion = "";
        private String ubicacion;
        private String horario;

        public String getCelular1() { return celular1; }
        public void setCelular1(String celular1) { this.celular1 = celular1; }

        public String getCelular2() { return celular2; }
        public void setCelular2(String celular2) { this.celular2 = celular2; }

        public String getNombreFerreteria() { return nombreFerreteria; }
        public void setNombreFerreteria(String nombreFerreteria) { this.nombreFerreteria = nombreFerreteria; }

        public String getNombreCliente() { return nombreCliente; }
        public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

        public String getDireccion() { return direccion; }
        public void setDireccion(String direccion) { this.direccion = direccion; }

        public String getUbicacion() { return ubicacion; }
        public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

        public String getHorario() { return horario; }
        public void setHorario(String horario) { this.horario = horario; }
    }

    public static class LineaForm {
        private int productoId;
        private Integer cantidad;

        public int getProductoId() { return productoId; }
        public void setProductoId(int productoId) { this.productoId = productoId; }

        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }
}
