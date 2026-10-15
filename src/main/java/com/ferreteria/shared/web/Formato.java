package com.ferreteria.shared.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos para las vistas; en Thymeleaf se usa como {@code ${@formato.moneda(x)}}. */
@Component("formato")
public class Formato {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** $ 12.500 (pesos, sin centavos). */
    public String moneda(BigDecimal valor) {
        return "$ " + entero(valor);
    }

    /** 12.500 */
    public String entero(Number valor) {
        DecimalFormat formato = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(COLOMBIA));
        return formato.format(valor);
    }

    /** 12,5 (sin ceros sobrantes). */
    public String porcentaje(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    /** Número con punto decimal, para atributos como data-precio que lee JavaScript. */
    public String plano(BigDecimal valor) {
        return valor.toPlainString();
    }

    public String fecha(LocalDateTime fecha) {
        return FECHA_HORA.format(fecha);
    }

    /** "1 h 05 min", "12 min" o "menos de 1 min". */
    public String duracion(LocalDateTime desde, LocalDateTime hasta) {
        long minutos = Math.max(0, Duration.between(desde, hasta).toMinutes());
        if (minutos < 1) {
            return "menos de 1 min";
        }
        return minutos < 60 ? minutos + " min" : (minutos / 60) + " h " + String.format("%02d", minutos % 60) + " min";
    }
}
