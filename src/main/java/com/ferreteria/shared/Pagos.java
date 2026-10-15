package com.ferreteria.shared;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Reglas del pago, iguales para ventas y pedidos. */
public final class Pagos {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private Pagos() {
    }

    /**
     * Valida con cuánto pagó el cliente y devuelve el valor a guardar.
     *
     * @return el monto, o {@code null} si no se anotó o el medio de pago no lo admite
     * @throws NegocioException si el monto es menor al total
     */
    public static BigDecimal montoRecibido(MedioPago medio, BigDecimal total, BigDecimal recibido) {
        if (recibido == null || !medio.admiteMontoRecibido()) {
            return null;
        }
        if (recibido.compareTo(total) < 0) {
            throw new NegocioException("El monto recibido (" + pesos(recibido)
                    + ") es menor al total (" + pesos(total) + ").");
        }
        return recibido;
    }

    public static String pesos(BigDecimal valor) {
        return "$ " + new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(COLOMBIA)).format(valor);
    }
}
