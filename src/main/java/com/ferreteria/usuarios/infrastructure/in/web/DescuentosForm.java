package com.ferreteria.usuarios.infrastructure.in.web;

import com.ferreteria.usuarios.application.port.in.GestionarUsuarios.DescuentoVendedor;
import com.ferreteria.usuarios.domain.Usuario;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Formulario de descuentos: una fila por vendedor. */
public class DescuentosForm {

    private List<Fila> filas = new ArrayList<>();

    static DescuentosForm desde(List<Usuario> vendedores) {
        DescuentosForm f = new DescuentosForm();
        for (Usuario u : vendedores) {
            f.filas.add(new Fila(u.id(), u.nombreUsuario(), u.activo(), u.descuentoPct()));
        }
        return f;
    }

    List<DescuentoVendedor> aDescuentos() {
        return filas.stream()
                .map(f -> new DescuentoVendedor(f.id, f.nombreUsuario, f.descuentoPct))
                .toList();
    }

    public List<Fila> getFilas() { return filas; }
    public void setFilas(List<Fila> filas) { this.filas = filas; }

    public static class Fila {
        private int id;
        private String nombreUsuario = "";
        private boolean activo;
        private BigDecimal descuentoPct = BigDecimal.ZERO;

        public Fila() {
        }

        Fila(int id, String nombreUsuario, boolean activo, BigDecimal descuentoPct) {
            this.id = id;
            this.nombreUsuario = nombreUsuario;
            this.activo = activo;
            this.descuentoPct = descuentoPct;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getNombreUsuario() { return nombreUsuario; }
        public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

        public boolean isActivo() { return activo; }
        public void setActivo(boolean activo) { this.activo = activo; }

        public BigDecimal getDescuentoPct() { return descuentoPct; }
        public void setDescuentoPct(BigDecimal descuentoPct) { this.descuentoPct = descuentoPct; }
    }
}
