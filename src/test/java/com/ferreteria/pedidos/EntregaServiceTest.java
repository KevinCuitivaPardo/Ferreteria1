package com.ferreteria.pedidos;

import com.ferreteria.pedidos.application.EntregaService;
import com.ferreteria.pedidos.application.TransicionesDeEstado;
import com.ferreteria.pedidos.application.port.in.GestionarEntrega.Resultado;
import com.ferreteria.pedidos.application.port.out.AlmacenFotos;
import com.ferreteria.pedidos.application.port.out.AvisosDeEntrega;
import com.ferreteria.pedidos.application.port.out.CatalogoProductos;
import com.ferreteria.pedidos.application.port.out.PedidoRepository;
import com.ferreteria.pedidos.domain.EstadoActual;
import com.ferreteria.pedidos.domain.FotoEntrega;
import com.ferreteria.pedidos.domain.LineaStock;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** La cola del domiciliario: comenzar uno por uno y terminar con foto. */
class EntregaServiceTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4, 5};
    static final int DOMICILIARIO = 9;

    PedidoRepository pedidos = mock(PedidoRepository.class);
    CatalogoProductos catalogo = mock(CatalogoProductos.class);
    AlmacenFotos fotos = mock(AlmacenFotos.class);
    AvisosDeEntrega avisos = mock(AvisosDeEntrega.class);
    RegistroEventos eventos = mock(RegistroEventos.class);
    EntregaService servicio;

    @BeforeEach
    void preparar() {
        var sinTransaccion = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> accion) {
                return accion.doInTransaction(null);
            }
        };
        servicio = new EntregaService(pedidos, fotos, avisos, eventos,
                new TransicionesDeEstado(pedidos, catalogo, eventos), sinTransaccion);

        when(fotos.guardar(any())).thenReturn("11111111-1111-1111-1111-111111111111.jpg");
        when(pedidos.entregaEnCurso(DOMICILIARIO)).thenReturn(Optional.empty());
        when(pedidos.cabeceraDeEntrega(99, DOMICILIARIO)).thenReturn(Optional.of(new PedidoCabecera(
                99, "PED-00099", LocalDateTime.now(), "entregado", BigDecimal.ZERO, new BigDecimal("30000"), null,
                "Luis", "Calle 45", null, null, "300", null, "ana", 1, "Sede principal", 4.6, -74.0, 4.65, -74.06,
                DOMICILIARIO, "pedro", MedioPago.EFECTIVO, null, null, null, null, null)));
    }

    private void pedido(String estado, Integer domiciliarioId) {
        when(pedidos.estadoBloqueado(99)).thenReturn(Optional.of(new EstadoActual(estado, 1, domiciliarioId)));
    }

    // ---------- Comenzar ----------

    @Test
    void comenzarPoneElPedidoEnRuta() {
        pedido("alistado", DOMICILIARIO);

        String mensaje = servicio.comenzar(99, DOMICILIARIO);

        assertEquals("Entrega en ruta. ¡Buen viaje!", mensaje);
        verify(pedidos).actualizarEstado(99, "en_ruta");
    }

    @Test
    void soloSePuedeLlevarUnaEntregaALaVez() {
        pedido("pendiente", DOMICILIARIO);
        when(pedidos.entregaEnCurso(DOMICILIARIO)).thenReturn(Optional.of(77));

        var e = assertThrows(NegocioException.class, () -> servicio.comenzar(99, DOMICILIARIO));

        assertEquals("Ya llevas una entrega en ruta. Termínala antes de comenzar otra.", e.getMessage());
        verify(pedidos, never()).actualizarEstado(anyInt(), anyString());
    }

    @Test
    void noSePuedeComenzarUnPedidoAjenoNiYaCerrado() {
        pedido("pendiente", 10);
        assertEquals("Este pedido no está asignado a ti.",
                assertThrows(NegocioException.class, () -> servicio.comenzar(99, DOMICILIARIO)).getMessage());

        pedido("pendiente", null);
        assertThrows(NegocioException.class, () -> servicio.comenzar(99, DOMICILIARIO));

        pedido("entregado", DOMICILIARIO);
        assertEquals("Este pedido ya está cerrado.",
                assertThrows(NegocioException.class, () -> servicio.comenzar(99, DOMICILIARIO)).getMessage());

        pedido("en_ruta", DOMICILIARIO);
        assertEquals("Esta entrega ya está en ruta.",
                assertThrows(NegocioException.class, () -> servicio.comenzar(99, DOMICILIARIO)).getMessage());
    }

    // ---------- Terminar ----------

    @Test
    void entregarGuardaLaFotoCierraElPedidoYAvisaAlAdmin() {
        pedido("en_ruta", DOMICILIARIO);
        FotoEntrega foto = FotoEntrega.deBytes(JPEG);

        String mensaje = servicio.terminar(99, DOMICILIARIO, Resultado.ENTREGADO, " Lo recibió el encargado ", foto);

        assertEquals("¡Entrega terminada! La foto quedó guardada.", mensaje);
        verify(pedidos).actualizarEstado(99, "entregado");
        verify(pedidos).guardarNotaDeEntrega(99, "Lo recibió el encargado");
        verify(pedidos).insertarFoto(99, "11111111-1111-1111-1111-111111111111.jpg", "image/jpeg", JPEG.length,
                DOMICILIARIO);
        verify(avisos).avisarAAdmins(eq("Pedido entregado"), contains("PED-00099"), eq("/Pedidos/Detalle/99"));
        verify(catalogo, never()).reponer(anyInt(), anyInt(), anyInt());
    }

    @Test
    void sinFotoNoSePuedeMarcarComoEntregado() {
        var e = assertThrows(NegocioException.class,
                () -> servicio.terminar(99, DOMICILIARIO, Resultado.ENTREGADO, "", null));

        assertEquals("Toma la foto de la entrega para poder terminarla.", e.getMessage());
        verifyNoInteractions(fotos, avisos);
        verify(pedidos, never()).actualizarEstado(anyInt(), anyString());
    }

    @Test
    void siNoSePudoEntregarHayQueDecirPorQueYElStockVuelveALaSucursal() {
        assertEquals("Cuenta por qué no se pudo entregar.",
                assertThrows(NegocioException.class,
                        () -> servicio.terminar(99, DOMICILIARIO, Resultado.NO_ENTREGADO, "  ", null)).getMessage());

        pedido("en_ruta", DOMICILIARIO);
        when(pedidos.lineasDeStock(99)).thenReturn(List.of(new LineaStock(7, 4)));

        servicio.terminar(99, DOMICILIARIO, Resultado.NO_ENTREGADO, "Nadie atendió", null);

        verify(catalogo).reponer(7, 1, 4);
        verify(pedidos).actualizarEstado(99, "no_entregado");
        verify(pedidos, never()).insertarFoto(anyInt(), anyString(), anyString(), anyInt(), anyInt());
        verify(avisos).avisarAAdmins(eq("Pedido no entregado"), contains("PED-00099"), anyString());
    }

    @Test
    void noSePuedeTerminarUnaEntregaQueNoSeComenzoOQueEsDeOtro() {
        pedido("pendiente", DOMICILIARIO);
        assertEquals("Primero comienza la entrega antes de terminarla.",
                assertThrows(NegocioException.class, () -> servicio.terminar(99, DOMICILIARIO, Resultado.ENTREGADO,
                        "", FotoEntrega.deBytes(JPEG))).getMessage());

        pedido("en_ruta", 10);
        assertEquals("Este pedido no está asignado a ti.",
                assertThrows(NegocioException.class, () -> servicio.terminar(99, DOMICILIARIO, Resultado.ENTREGADO,
                        "", FotoEntrega.deBytes(JPEG))).getMessage());
        verify(avisos, never()).avisarAAdmins(anyString(), anyString(), anyString());
    }

    @Test
    void siAlgoFallaDespuesDeGuardarLaFotoElArchivoSeBorra() {
        pedido("en_ruta", DOMICILIARIO);
        doThrow(new IllegalStateException("la base de datos se cayó")).when(pedidos)
                .insertarFoto(anyInt(), anyString(), anyString(), anyInt(), anyInt());

        assertThrows(IllegalStateException.class, () -> servicio.terminar(99, DOMICILIARIO, Resultado.ENTREGADO,
                "", FotoEntrega.deBytes(JPEG)));

        verify(fotos).borrar("11111111-1111-1111-1111-111111111111.jpg");
        verify(avisos, never()).avisarAAdmins(anyString(), anyString(), anyString());
    }
}
