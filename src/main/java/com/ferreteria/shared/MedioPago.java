package com.ferreteria.shared;

import java.util.Arrays;

/** Cómo pagó el cliente. Lo usan las ventas en el local y los pedidos a domicilio. */
public enum MedioPago {
    EFECTIVO("efectivo", "Efectivo"),
    TRANSFERENCIA("transferencia", "Transferencia"),
    TARJETA("tarjeta", "Tarjeta"),
    NEQUI("nequi", "Nequi"),
    DAVIPLATA("daviplata", "Daviplata");

    private final String clave;
    private final String etiqueta;

    MedioPago(String clave, String etiqueta) {
        this.clave = clave;
        this.etiqueta = etiqueta;
    }

    public String getClave() {
        return clave;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    /** Solo en efectivo tiene sentido anotar con cuánto pagó el cliente para calcular el cambio. */
    public boolean admiteMontoRecibido() {
        return this == EFECTIVO;
    }

    /** Un valor desconocido o vacío se toma como efectivo, el pago más común en el mostrador. */
    public static MedioPago deClave(String clave) {
        return Arrays.stream(values()).filter(m -> m.clave.equals(clave)).findFirst().orElse(EFECTIVO);
    }

    public static String etiquetaDe(String clave) {
        return deClave(clave).etiqueta;
    }
}
