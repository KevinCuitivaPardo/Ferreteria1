package com.ferreteria.usuarios.domain;

public enum Rol {
    ADMIN("admin", "Administrador"),
    VENDEDOR("vendedor", "Vendedor"),
    DOMICILIARIO("domiciliario", "Domiciliario");

    private final String clave;
    private final String etiqueta;

    Rol(String clave, String etiqueta) {
        this.clave = clave;
        this.etiqueta = etiqueta;
    }

    public String getClave() {
        return clave;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Un valor desconocido se trata como vendedor, el rol con menos poder administrativo. */
    public static Rol deClave(String clave) {
        for (Rol rol : values()) {
            if (rol.clave.equals(clave)) {
                return rol;
            }
        }
        return VENDEDOR;
    }
}
