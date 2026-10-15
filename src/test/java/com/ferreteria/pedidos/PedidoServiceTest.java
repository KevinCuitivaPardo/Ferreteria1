package com.ferreteria.pedidos;

import com.ferreteria.pedidos.application.PedidoService;
import com.ferreteria.pedidos.application.TransicionesDeEstado;
import com.ferreteria.pedidos.application.port.in.CrearPedido;
import com.ferreteria.pedidos.application.port.out.AvisosDeEntrega;
import com.ferreteria.pedidos.application.port.out.CatalogoProductos;
import com.ferreteria.pedidos.application.port.out.ClienteRepository;
import com.ferreteria.pedidos.application.port.out.Domiciliarios;
import com.ferreteria.pedidos.application.port.out.PedidoRepository;
import com.ferreteria.pedidos.application.port.out.UbicadorDeDirecciones;
import com.ferreteria.pedidos.application.port.out.Vendedores;
import com.ferreteria.pedidos.domain.DatosCliente;
import com.ferreteria.pedidos.domain.Domiciliario;
import com.ferreteria.pedidos.domain.EstadoActual;
import com.ferreteria.pedidos.domain.LineaPedido;
import com.ferreteria.pedidos.domain.LineaStock;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.pedidos.domain.PedidoGuardado;
import com.ferreteria.pedidos.domain.ProductoVendible;
import com.ferreteria.pedidos.domain.Ubicacion;
import com.ferreteria.pedidos.domain.Vendedor;
import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Los casos de uso se prueban con puertos simulados: sin Spring, sin base de datos, sin internet. */
class PedidoServiceTest {

    static final int SUCURSAL = 1;

    PedidoRepository pedidos = mock(PedidoRepository.class);
    ClienteRepository clientes = mock(ClienteRepository.class);
    CatalogoProductos catalogo = mock(CatalogoProductos.class);
    Vendedores vendedores = mock(Vendedores.class);
    Domiciliarios domiciliarios = mock(Domiciliarios.class);
    UbicadorDeDirecciones ubicador = mock(UbicadorDeDirecciones.class);
    RegistroEventos eventos = mock(RegistroEventos.class);
    AvisosDeEntrega avisos = mock(AvisosDeEntrega.class);
    PedidoService servicio;

    static final DatosCliente CLIENTE =
            new DatosCliente("300", null, null, "Luis", "Calle 45 # 12-30", null, null);

    @BeforeEach
    void preparar() {
        // Una "transacción" que solo ejecuta el código, para probar sin base de datos
        var sinTransaccion = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> accion) {
                return accion.doInTransaction(null);
            }
        };
        servicio = new PedidoService(pedidos, clientes, catalogo, vendedores, domiciliarios, ubicador, eventos,
                avisos, new TransicionesDeEstado(pedidos, catalogo, eventos), sinTransaccion);

        when(vendedores.porNombre("ana")).thenReturn(Optional.of(new Vendedor(2, true, new BigDecimal("10"))));
        when(clientes.idPorCelular("300")).thenReturn(Optional.of(5));
        when(catalogo.bloquearParaVenta(7, SUCURSAL)).thenReturn(Optional.of(
                new ProductoVendible(7, "DW-8", "Tornillo", "8 x 2", new BigDecimal("3000"), 50)));
        when(ubicador.ubicar("Calle 45 # 12-30")).thenReturn(Optional.of(new Ubicacion(4.65, -74.06)));
        when(pedidos.insertar(anyInt(), anyInt(), anyInt(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(new PedidoGuardado(99, "PED-00099"));
    }

    private CrearPedido.Comando comando(BigDecimal descuento, MedioPago medio, BigDecimal recibido,
                                        LineaPedido... lineas) {
        return new CrearPedido.Comando(SUCURSAL, "ana", CLIENTE, descuento, medio, recibido, List.of(lineas));
    }

    private CrearPedido.Comando comando(BigDecimal descuento, LineaPedido... lineas) {
        return comando(descuento, MedioPago.EFECTIVO, null, lineas);
    }

    // ---------- Crear ----------

    @Test
    void creaElDomicilioDescuentaStockDeLaSucursalLoUbicaYAvisaAlAdmin() {
        int id = servicio.crear(comando(new BigDecimal("10"), MedioPago.NEQUI, null, new LineaPedido(7, 10)));

        assertEquals(99, id);
        verify(catalogo).descontar(7, SUCURSAL, 10);
        // 10 × 3000 = 30000, menos 10 % = 27000
        verify(pedidos).insertar(eq(5), eq(2), eq(SUCURSAL), eq("Calle 45 # 12-30"), eq(new Ubicacion(4.65, -74.06)),
                eq(new BigDecimal("10")), eq(new BigDecimal("27000")), eq(MedioPago.NEQUI), isNull());
        verify(pedidos).insertarItem(99, 7, 10, new BigDecimal("3000"));
        verify(eventos).registrar(eq(TipoEvento.PEDIDO), eq(SUCURSAL), contains("PED-00099"));
        verify(eventos).registrar(eq(TipoEvento.DESCUENTO), eq(SUCURSAL), contains("10%"));
        // Al administrador le llega una alerta de que hay un pedido por asignar
        verify(avisos).avisarAAdmins(eq("Nuevo pedido por asignar"), contains("PED-00099"), eq("/Pedidos/Detalle/99"));
    }

    @Test
    void guardaElMedioDePagoYConCuantoPagaElCliente() {
        servicio.crear(comando(BigDecimal.ZERO, MedioPago.EFECTIVO, new BigDecimal("50000"), new LineaPedido(7, 10)));

        // total 30000, paga con 50000 → el domiciliario lleva 20000 de cambio
        verify(pedidos).insertar(anyInt(), anyInt(), anyInt(), anyString(), any(), any(),
                eq(new BigDecimal("30000")), eq(MedioPago.EFECTIVO), eq(new BigDecimal("50000")));
    }

    @Test
    void noSePuedePagarConMenosDelTotal() {
        var e = assertThrows(NegocioException.class, () -> servicio.crear(
                comando(BigDecimal.ZERO, MedioPago.EFECTIVO, new BigDecimal("20000"), new LineaPedido(7, 10))));

        assertEquals("El monto recibido ($ 20.000) es menor al total ($ 30.000).", e.getMessage());
        verify(pedidos, never()).insertarItem(anyInt(), anyInt(), anyInt(), any());
    }

    @Test
    void elMontoRecibidoSoloImportaEnEfectivo() {
        servicio.crear(comando(BigDecimal.ZERO, MedioPago.TRANSFERENCIA, new BigDecimal("50000"), new LineaPedido(7, 1)));

        verify(pedidos).insertar(anyInt(), anyInt(), anyInt(), anyString(), any(), any(), any(),
                eq(MedioPago.TRANSFERENCIA), isNull());
    }

    @Test
    void siLaDireccionNoSeEncuentraElPedidoSeGuardaSinUbicacion() {
        when(ubicador.ubicar("Calle 45 # 12-30")).thenReturn(Optional.empty());

        int id = servicio.crear(comando(BigDecimal.ZERO, new LineaPedido(7, 1)));

        assertEquals(99, id);
        verify(pedidos).insertar(anyInt(), anyInt(), anyInt(), anyString(), isNull(), any(), any(), any(), any());
        verify(eventos).registrar(eq(TipoEvento.PEDIDO), eq(SUCURSAL), contains("sin ubicar"));
    }

    @Test
    void noDejaPasarDelDescuentoMaximoDelVendedor() {
        var e = assertThrows(NegocioException.class,
                () -> servicio.crear(comando(new BigDecimal("15"), new LineaPedido(7, 1))));

        assertEquals("El descuento elegido supera tu máximo permitido (10%).", e.getMessage());
        verify(catalogo, never()).descontar(anyInt(), anyInt(), anyInt());
        verifyNoInteractions(avisos);
    }

    @Test
    void noVendeMasDeLoQueHayEnLaSucursal() {
        var e = assertThrows(NegocioException.class,
                () -> servicio.crear(comando(BigDecimal.ZERO, new LineaPedido(7, 51))));

        assertEquals("No hay suficiente stock de Tornillo. Disponible: 50.", e.getMessage());
    }

    @Test
    void juntaTodosLosErroresYNoConsultaElMapaSiHayErrores() {
        var comando = new CrearPedido.Comando(SUCURSAL, "ana",
                new DatosCliente("", null, null, "", "", null, null),
                BigDecimal.ZERO, MedioPago.EFECTIVO, null, List.of(new LineaPedido(0, 3)));

        var e = assertThrows(NegocioException.class, () -> servicio.crear(comando));

        assertEquals(4, e.getMensajes().size());
        verify(ubicador, never()).ubicar(anyString());
    }

    // ---------- Estado (administrador) ----------

    @Test
    void cancelarUnPedidoDevuelveElStockALaSucursalDelPedido() {
        when(pedidos.estadoBloqueado(99)).thenReturn(Optional.of(new EstadoActual("pendiente", 3, null)));
        when(pedidos.lineasDeStock(99)).thenReturn(List.of(new LineaStock(7, 4), new LineaStock(8, 1)));

        String mensaje = servicio.cambiarEstado(99, "cancelado");

        assertEquals("Estado cambiado a Cancelado.", mensaje);
        verify(catalogo).reponer(7, 3, 4);
        verify(catalogo).reponer(8, 3, 1);
        verify(pedidos).actualizarEstado(99, "cancelado");
        verify(eventos).registrar(eq(TipoEvento.ESTADO_PEDIDO), eq(3), contains("Cancelado"));
    }

    @Test
    void reactivarUnPedidoSinStockSuficienteFalla() {
        when(pedidos.estadoBloqueado(99)).thenReturn(Optional.of(new EstadoActual("cancelado", 1, null)));
        when(pedidos.lineasDeStock(99)).thenReturn(List.of(new LineaStock(7, 60)));

        assertThrows(NegocioException.class, () -> servicio.cambiarEstado(99, "pendiente"));

        verify(catalogo, never()).descontar(anyInt(), anyInt(), anyInt());
        verify(pedidos, never()).actualizarEstado(anyInt(), any());
    }

    @Test
    void pasarEntreEstadosActivosNoTocaElStock() {
        when(pedidos.estadoBloqueado(99)).thenReturn(Optional.of(new EstadoActual("pendiente", 1, null)));

        servicio.cambiarEstado(99, "en_ruta");

        verify(catalogo, never()).reponer(anyInt(), anyInt(), anyInt());
        verify(catalogo, never()).descontar(anyInt(), anyInt(), anyInt());
        verify(pedidos).actualizarEstado(99, "en_ruta");
    }

    // ---------- Asignación y alertas al domiciliario ----------

    private PedidoCabecera cabecera(int id, String estado, Integer domiciliarioId) {
        return new PedidoCabecera(id, "PED-00099", LocalDateTime.now(), estado, BigDecimal.ZERO,
                new BigDecimal("30000"), "Ferro", "Luis", "Calle 45 # 12-30", null, null, "300", null, "ana",
                SUCURSAL, "Sede principal", 4.6, -74.0, 4.65, -74.06, domiciliarioId, null,
                MedioPago.EFECTIVO, null, null, null, null, null);
    }

    private void pedidoEnEstado(String estado, Integer domiciliarioId) {
        when(pedidos.estadoBloqueado(99)).thenReturn(Optional.of(new EstadoActual(estado, SUCURSAL, domiciliarioId)));
        when(pedidos.cabecera(99, true, 0)).thenReturn(Optional.of(cabecera(99, estado, domiciliarioId)));
    }

    @Test
    void alAsignarElPedidoAlDomiciliarioLeLlegaUnaAlerta() {
        pedidoEnEstado("pendiente", null);
        when(domiciliarios.porId(9)).thenReturn(Optional.of(new Domiciliario(9, "pedro", true, SUCURSAL)));

        String mensaje = servicio.asignar(99, 9);

        assertEquals("Pedido asignado a pedro. Le llegó un aviso.", mensaje);
        verify(pedidos).asignarDomiciliario(99, 9);
        verify(eventos).registrar(eq(TipoEvento.ASIGNACION), eq(SUCURSAL), contains("pedro"));
        verify(avisos).pedidoAsignado(9, 99, "PED-00099", "Luis", "Calle 45 # 12-30");
    }

    @Test
    void alReasignarSeLeQuitaDeLaColaAlAnteriorYSeLeAvisaAlNuevo() {
        pedidoEnEstado("pendiente", 8);
        when(domiciliarios.porId(9)).thenReturn(Optional.of(new Domiciliario(9, "pedro", true, SUCURSAL)));

        servicio.asignar(99, 9);

        verify(avisos).pedidoRetirado(8, 99, "PED-00099");
        verify(avisos).pedidoAsignado(9, 99, "PED-00099", "Luis", "Calle 45 # 12-30");
    }

    @Test
    void asignarAlMismoDomiciliarioNoLoAvisaDeNuevo() {
        pedidoEnEstado("alistado", 9);
        when(domiciliarios.porId(9)).thenReturn(Optional.of(new Domiciliario(9, "pedro", true, SUCURSAL)));

        String mensaje = servicio.asignar(99, 9);

        assertEquals("El pedido ya estaba asignado a pedro.", mensaje);
        verify(pedidos, never()).asignarDomiciliario(anyInt(), any());
        verifyNoInteractions(avisos);
    }

    @Test
    void noAsignaADomiciliariosDeOtraSucursalNiDesactivados() {
        pedidoEnEstado("pendiente", null);
        when(domiciliarios.porId(9)).thenReturn(Optional.of(new Domiciliario(9, "pedro", true, 2)));
        when(domiciliarios.porId(10)).thenReturn(Optional.of(new Domiciliario(10, "juan", false, SUCURSAL)));

        assertEquals("El domiciliario pertenece a otra sucursal.",
                assertThrows(NegocioException.class, () -> servicio.asignar(99, 9)).getMessage());
        assertThrows(NegocioException.class, () -> servicio.asignar(99, 10));
        verify(pedidos, never()).asignarDomiciliario(anyInt(), any());
        verifyNoInteractions(avisos);
    }

    @Test
    void unPedidoQueYaVaEnRutaOEstaCerradoNoSeReasigna() {
        pedidoEnEstado("en_ruta", 8);
        assertEquals("El pedido ya va en ruta: no se puede cambiar su domiciliario.",
                assertThrows(NegocioException.class, () -> servicio.asignar(99, 9)).getMessage());

        pedidoEnEstado("entregado", 8);
        assertEquals("El pedido ya está cerrado: no se puede asignar.",
                assertThrows(NegocioException.class, () -> servicio.asignar(99, 9)).getMessage());
        verifyNoInteractions(avisos);
    }

    @Test
    void conCeroSeQuitaLaAsignacionYSeAvisaAlQueLoTenia() {
        pedidoEnEstado("pendiente", 9);

        servicio.asignar(99, 0);

        verify(pedidos).asignarDomiciliario(99, null);
        verify(avisos).pedidoRetirado(9, 99, "PED-00099");
        verify(avisos, never()).pedidoAsignado(anyInt(), anyInt(), anyString(), anyString(), anyString());
    }

    // ---------- Ubicación ----------

    @Test
    void elAdminFijaElPuntoDeEntregaAMano() {
        when(pedidos.estadoBloqueado(99)).thenReturn(Optional.of(new EstadoActual("pendiente", 1, null)));

        servicio.corregir(99, 4.66, -74.05);

        verify(pedidos).actualizarUbicacion(99, new Ubicacion(4.66, -74.05));
        verify(eventos).registrar(eq(TipoEvento.PEDIDO), eq(1), contains("ajustada a mano"));
    }

    @Test
    void rechazaPuntosQueNoExistenEnElMapa() {
        assertThrows(NegocioException.class, () -> servicio.corregir(99, 95, 0));
        assertThrows(NegocioException.class, () -> servicio.corregir(99, 4, 200));
        assertThrows(NegocioException.class, () -> servicio.corregir(99, Double.NaN, 0));
        verify(pedidos, never()).actualizarUbicacion(anyInt(), any());
    }
}
