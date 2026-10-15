package com.ferreteria.tiemporeal;

import com.ferreteria.tiemporeal.application.TiempoRealService;
import com.ferreteria.tiemporeal.application.port.out.CanalTiempoReal;
import com.ferreteria.tiemporeal.application.port.out.PosicionesActuales;
import com.ferreteria.tiemporeal.domain.Aviso;
import com.ferreteria.tiemporeal.domain.MensajeTiempoReal;
import com.ferreteria.tiemporeal.domain.PosicionDomiciliario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TiempoRealServiceTest {

    PosicionesActuales posiciones = mock(PosicionesActuales.class);
    CanalTiempoReal canal = mock(CanalTiempoReal.class);
    TiempoRealService servicio;

    @BeforeEach
    void preparar() {
        servicio = new TiempoRealService(posiciones, canal);
        when(posiciones.de(9)).thenReturn(Optional.empty());
    }

    @Test
    void laPosicionDelDomiciliarioSeGuardaYSeEmpujaALosAdmins() {
        servicio.reportar(9, "pedro", 1, 4.65, -74.06, 12.0);

        var guardada = ArgumentCaptor.forClass(PosicionDomiciliario.class);
        verify(posiciones).guardar(guardada.capture());
        assertEquals("pedro", guardada.getValue().nombre());
        assertEquals(4.65, guardada.getValue().latitud());

        var mensaje = ArgumentCaptor.forClass(MensajeTiempoReal.class);
        verify(canal).enviarAAdmins(mensaje.capture());
        assertEquals("posicion", mensaje.getValue().tipo());
        assertEquals(guardada.getValue(), mensaje.getValue().datos());
    }

    @Test
    void ignoraPuntosQueNoExistenEnElMapa() {
        servicio.reportar(9, "pedro", 1, 91, 0, null);
        servicio.reportar(9, "pedro", 1, 0, -181, null);
        servicio.reportar(9, "pedro", 1, Double.NaN, 0, null);

        verify(posiciones, never()).guardar(any());
        verify(canal, never()).enviarAAdmins(any());
    }

    @Test
    void noReenviaPosicionesMuyDeSeguidoPeroSiLasQueYaPasoElIntervalo() {
        when(posiciones.de(9)).thenReturn(Optional.of(new PosicionDomiciliario(
                9, "pedro", 1, 4.6, -74.0, null, Instant.now().minusMillis(300))));
        servicio.reportar(9, "pedro", 1, 4.65, -74.06, null);
        verify(posiciones, never()).guardar(any());

        when(posiciones.de(9)).thenReturn(Optional.of(new PosicionDomiciliario(
                9, "pedro", 1, 4.6, -74.0, null, Instant.now().minusSeconds(5))));
        servicio.reportar(9, "pedro", 1, 4.65, -74.06, null);
        verify(posiciones).guardar(any());
    }

    @Test
    void lasPosicionesActualesSoloIncluyenLasRecientes() {
        var reciente = new PosicionDomiciliario(9, "pedro", 1, 4.6, -74.0, null, Instant.now());
        when(posiciones.desde(any())).thenReturn(List.of(reciente));

        assertEquals(List.of(reciente), servicio.actuales());

        var desde = ArgumentCaptor.forClass(Instant.class);
        verify(posiciones).desde(desde.capture());
        // la ventana de vigencia es de 30 minutos
        assertTrue(Duration.between(desde.getValue(), Instant.now()).toMinutes() >= 29);
    }

    @Test
    void losAvisosVanAlUsuarioOAlosAdmins() {
        var aviso = new Aviso("asignacion", "Nuevo pedido asignado", "PED-1 · Luis", "/Entregas");

        servicio.avisarAUsuario(9, aviso);
        servicio.avisarAAdmins(aviso);

        verify(canal).enviarAUsuario(9, new MensajeTiempoReal("aviso", aviso));
        verify(canal).enviarAAdmins(new MensajeTiempoReal("aviso", aviso));
    }
}
