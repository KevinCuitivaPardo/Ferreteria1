package com.ferreteria.geolocalizacion.infrastructure.out.osm;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * En Colombia una dirección como "Carrera 7 # 72-41" significa "sobre la Carrera 7, cerca de la Calle 72".
 * OpenStreetMap casi nunca tiene los números de casa y, en las ciudades grandes, las carreras están partidas
 * en muchos tramos; en cambio las calles numeradas sí se encuentran. Por eso la dirección se busca por su
 * calle numerada ("Calle 72, Bogotá"): el resultado es aproximado (un punto sobre esa calle) y el
 * administrador puede ajustarlo a mano en el mapa.
 */
final class DireccionesColombianas {

    private static final Pattern DIRECCION = Pattern.compile(
            "^\\s*(calle|cl|cll|carrera|cra|cr|kr|kra|avenida|av|diagonal|diag|dg|transversal|trans|tv)\\.?\\s*"
                    + "(\\d+\\s*[a-z]?(?:\\s*bis)?(?:\\s+(?:sur|norte|este|oeste))?)\\s*"
                    + "(?:#|no\\.?|nro\\.?|n°)\\s*(\\d+\\s*[a-z]?)(?:\\s*-\\s*\\d+)?\\s*(.*)$",
            Pattern.CASE_INSENSITIVE);

    private DireccionesColombianas() {
    }

    /**
     * Consultas a probar, en orden. Si la dirección tiene formato de placa colombiana se busca por su calle
     * numerada: buscarla tal cual devuelve una carrera larga ubicada kilómetros más allá, y un punto
     * equivocado es peor que ninguno. Si no tiene ese formato se usa el texto original.
     */
    static List<String> consultas(String direccion) {
        Matcher m = DIRECCION.matcher(direccion);

        if (!m.matches()) {
            return List.of(direccion);
        }

        String via = nombreDeVia(m.group(1));
        String numeroVia = m.group(2).trim().replaceAll("\\s+", " ");
        String numeroCruce = m.group(3).trim().replaceAll("\\s+", " ");
        String resto = m.group(4).trim().replaceFirst("^,\\s*", "");
        String zona = resto.isEmpty() ? "" : ", " + resto;

        return switch (via) {
            case "Calle" -> List.of("Calle " + numeroVia + zona);
            case "Carrera", "Avenida", "Transversal" -> List.of("Calle " + numeroCruce + zona);
            default -> List.of(direccion);
        };
    }

    private static String nombreDeVia(String abreviatura) {
        return switch (abreviatura.toLowerCase()) {
            case "calle", "cl", "cll" -> "Calle";
            case "carrera", "cra", "cr", "kr", "kra" -> "Carrera";
            case "avenida", "av" -> "Avenida";
            case "diagonal", "diag", "dg" -> "Diagonal";
            default -> "Transversal";
        };
    }
}
