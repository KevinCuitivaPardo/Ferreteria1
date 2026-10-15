package com.ferreteria.inventario.infrastructure.in.web;

import com.ferreteria.inventario.domain.DatosProducto;
import com.ferreteria.inventario.domain.Producto;

import java.math.BigDecimal;

/** Datos del formulario de producto (nuevo y editar). */
public class ProductoForm {

    private Integer id;
    private String codigo = "";
    private String nombre = "";
    private String medida;
    private BigDecimal precioCompra = BigDecimal.ZERO;
    private Integer stock = 0;
    private Integer stockMinimo = 0;

    static ProductoForm desde(Producto p) {
        ProductoForm f = new ProductoForm();
        f.id = p.id();
        f.codigo = p.codigo();
        f.nombre = p.nombre();
        f.medida = p.medida();
        f.precioCompra = p.precioCompra();
        f.stock = p.stock();
        f.stockMinimo = p.stockMinimo();
        return f;
    }

    DatosProducto aDatos() {
        return new DatosProducto(codigo, nombre, medida, precioCompra,
                stock == null ? 0 : stock, stockMinimo == null ? 0 : stockMinimo);
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getMedida() { return medida; }
    public void setMedida(String medida) { this.medida = medida; }

    public BigDecimal getPrecioCompra() { return precioCompra; }
    public void setPrecioCompra(BigDecimal precioCompra) { this.precioCompra = precioCompra; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    /** Precio de venta automático: compra × 2. */
    public BigDecimal getPrecioVentaAutomatico() {
        return precioCompra == null ? BigDecimal.ZERO : precioCompra.multiply(BigDecimal.valueOf(2));
    }
}
