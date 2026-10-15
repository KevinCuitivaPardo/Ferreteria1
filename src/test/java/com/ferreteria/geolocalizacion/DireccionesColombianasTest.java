package com.ferreteria.geolocalizacion;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La clase es privada del adaptador de OpenStreetMap, así que se prueba por reflexión. */
class DireccionesColombianasTest {

    @SuppressWarnings("unchecked")
    private static List<String> consultas(String direccion) throws Exception {
        Class<?> clase = Class.forName("com.ferreteria.geolocalizacion.infrastructure.out.osm.DireccionesColombianas");
        Method metodo = clase.getDeclaredMethod("consultas", String.class);
        metodo.setAccessible(true);
        return (List<String>) metodo.invoke(null, direccion);
    }

    @Test
    void unaCarreraConPlacaSeBuscaPorSuCalleNumerada() throws Exception {
        // "Carrera 7 # 72-41" queda sobre la Carrera 7, cerca de la Calle 72
        assertEquals(List.of("Calle 72, Bogota"), consultas("Carrera 7 # 72-41, Bogota"));
    }

    @Test
    void unaCalleConPlacaSeBuscaPorSuPropiaCalle() throws Exception {
        assertEquals(List.of("Calle 85, Bogota"), consultas("Calle 85 # 15-20, Bogota"));
    }

    @Test
    void entiendeAbreviaturasLetrasYNumeroDeLaPlaca() throws Exception {
        assertEquals("Calle 85, Bogota", consultas("Cl 85 # 15-20, Bogota").get(0));
        assertEquals("Calle 45A, Bogota", consultas("Calle 45A No. 13-30, Bogota").get(0));
        assertEquals("Calle 30, Medellín", consultas("Kra 80 #30-15, Medellín").get(0));
    }

    @Test
    void sinCiudadTampocoFalla() throws Exception {
        assertEquals(List.of("Calle 100"), consultas("Carrera 15 # 100-20"));
    }

    @Test
    void siNoEsUnaPlacaUsaElTextoTalCual() throws Exception {
        assertEquals(List.of("Parque de la 93, Bogotá"), consultas("Parque de la 93, Bogotá"));
        assertEquals(List.of("Diagonal 40 # 12-3, Cali"), consultas("Diagonal 40 # 12-3, Cali"));
    }
}
