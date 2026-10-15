package com.ferreteria.sucursales.infrastructure.in.web;

import com.ferreteria.sucursales.domain.DatosSucursal;
import com.ferreteria.sucursales.domain.Sucursal;

/** Formulario de sucursal (nueva y editar). Latitud y longitud vienen del punto marcado en el mapa. */
public class SucursalForm {

    private String nombre = "";
    private String direccion = "";
    private Double latitud;
    private Double longitud;

    static SucursalForm desde(Sucursal s) {
        SucursalForm f = new SucursalForm();
        f.nombre = s.nombre();
        f.direccion = s.direccion();
        f.latitud = s.latitud();
        f.longitud = s.longitud();
        return f;
    }

    DatosSucursal aDatos() {
        return new DatosSucursal(nombre, direccion, latitud, longitud);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
}
