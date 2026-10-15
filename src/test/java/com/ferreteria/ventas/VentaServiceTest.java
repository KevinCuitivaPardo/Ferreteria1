package com.ferreteria.ventas;

import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import com.ferreteria.ventas.application.VentaService;
import com.ferreteria.ventas.application.port.in.RegistrarVenta;
import com.ferreteria.ventas.application.port.out.CatalogoProductos;
import com.ferreteria.ventas.application.port.out.Vendedores;
import com.ferreteria.ventas.application.port.out.VentaRepository;
import com.ferreteria.ventas.domain.LineaVenta;
import com.ferreteria.ventas.domain.ProductoEnVenta;
import com.ferreteria.ventas.domain.Vendedor;
import com.ferreteria.ventas.domain.VentaGuardada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Venta en el local, probada con puertos simulados. */
class VentaServiceTest {

    static final int SUCURSAL = 2;

    VentaRepository ventas = mock(VentaRepository.class);
    CatalogoProductos catalogo = mock(CatalogoProductos.class);
    Vendedores vendedores = mock(Vendedores.class);
    RegistroEventos eventos = mock(RegistroEventos.class);
    VentaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new VentaService(ventas, catalogo, vendedores, eventos);
        when(vendedores.porNombre("ana")).thenReturn(Optional.of(new Vendedor(2, true, new BigDecimal("10"))));
        when(catalogo.bloquearParaVenta(7, SUCURSAL)).thenReturn(Optional.of(
                new ProductoEnVenta(7, "DW-8", "Tornillo", "8 x 2", new BigDecimal("3000"), 50)));
        when(ventas.insertar(anyInt(), anyInt(), anyString(), anyString(), any(), any(), any(), any()))
                .thenReturn(new VentaGuardada(41, "VEN-00041"));
    }

    private RegistrarVenta.Comando comando(String nombre, String telefono, BigDecimal descuento, LineaVenta... lineas) {
        return comando(nombre, telefono, descuento, MedioPago.EFECTIVO, null, lineas);
    }

    private RegistrarVenta.Comando comando(String nombre, String telefono, BigDecimal descuento, MedioPago medio,
                                           BigDecimal recibido, LineaVenta... lineas) {
        return new RegistrarVenta.Comando(SUCURSAL, "ana", nombre, telefono, descuento, medio, recibido, List.of(lineas));
    }

    @Test
    void registraLaVentaConNombreTelefonoYCantidadYDescuentaLaSucursal() {
        int id = servicio.registrar(comando("Marta Ríos", "3115550000", new BigDecimal("10"), new LineaVenta(7, 10)));

        assertEquals(41, id);
        verify(catalogo).descontar(7, SUCURSAL, 10);
        // 10 × 3000 = 30000, menos 10 % = 27000
        verify(ventas).insertar(SUCURSAL, 2, "Marta Ríos", "3115550000", new BigDecimal("10"), new BigDecimal("27000"),
                MedioPago.EFECTIVO, null);
        verify(ventas).insertarItem(41, 7, 10, new BigDecimal("3000"));
        verify(eventos).registrar(eq(TipoEvento.VENTA), eq(SUCURSAL), contains("VEN-00041"));
        verify(eventos).registrar(eq(TipoEvento.DESCUENTO), eq(SUCURSAL), contains("10%"));
    }

    @Test
    void sinDescuentoNoSeRegistraEventoDeDescuento() {
        servicio.registrar(comando("Marta", "311", BigDecimal.ZERO, new LineaVenta(7, 1)));

        verify(eventos, never()).registrar(eq(TipoEvento.DESCUENTO), anyInt(), anyString());
    }

    @Test
    void exigeNombreYNumeroDeContactoDeQuienCompra() {
        var e = assertThrows(NegocioException.class,
                () -> servicio.registrar(comando(" ", "", BigDecimal.ZERO, new LineaVenta(7, 1))));

        assertEquals(2, e.getMensajes().size());
        verify(catalogo, never()).descontar(anyInt(), anyInt(), anyInt());
    }

    @Test
    void exigeAlMenosUnProductoConCantidad() {
        var e = assertThrows(NegocioException.class,
                () -> servicio.registrar(comando("Marta", "311", BigDecimal.ZERO, new LineaVenta(0, null))));

        assertEquals("Agrega al menos un producto con cantidad.", e.getMessage());
    }

    @Test
    void noVendeMasDeLoQueHayNiPasaDelDescuentoPermitido() {
        assertEquals("No hay suficiente stock de Tornillo. Disponible: 50.",
                assertThrows(NegocioException.class,
                        () -> servicio.registrar(comando("M", "3", BigDecimal.ZERO, new LineaVenta(7, 51)))).getMessage());
        assertEquals("El descuento elegido supera tu máximo permitido (10%).",
                assertThrows(NegocioException.class,
                        () -> servicio.registrar(comando("M", "3", new BigDecimal("11"), new LineaVenta(7, 1)))).getMessage());
    }

    @Test
    void unVendedorDesactivadoNoPuedeVender() {
        when(vendedores.porNombre("ana")).thenReturn(Optional.of(new Vendedor(2, false, BigDecimal.TEN)));

        assertThrows(NegocioException.class,
                () -> servicio.registrar(comando("M", "3", BigDecimal.ZERO, new LineaVenta(7, 1))));
    }

    @Test
    void guardaElMedioDePagoYElMontoRecibidoEnEfectivo() {
        servicio.registrar(comando("Marta", "311", BigDecimal.ZERO, MedioPago.EFECTIVO, new BigDecimal("50000"),
                new LineaVenta(7, 10)));

        // 10 × 3000 = 30000; paga con 50000
        verify(ventas).insertar(SUCURSAL, 2, "Marta", "311", BigDecimal.ZERO, new BigDecimal("30000"),
                MedioPago.EFECTIVO, new BigDecimal("50000"));
        verify(eventos).registrar(eq(TipoEvento.VENTA), eq(SUCURSAL), contains("pago en efectivo"));
    }

    @Test
    void conTarjetaOTransferenciaNoSeGuardaMontoRecibido() {
        servicio.registrar(comando("Marta", "311", BigDecimal.ZERO, MedioPago.TARJETA, new BigDecimal("50000"),
                new LineaVenta(7, 10)));

        verify(ventas).insertar(SUCURSAL, 2, "Marta", "311", BigDecimal.ZERO, new BigDecimal("30000"),
                MedioPago.TARJETA, null);
    }

    @Test
    void sinIndicarMedioDePagoSeTomaEfectivo() {
        servicio.registrar(comando("Marta", "311", BigDecimal.ZERO, (MedioPago) null, null, new LineaVenta(7, 1)));

        verify(ventas).insertar(SUCURSAL, 2, "Marta", "311", BigDecimal.ZERO, new BigDecimal("3000"),
                MedioPago.EFECTIVO, null);
    }

    @Test
    void noSePuedePagarConMenosDelTotal() {
        var e = assertThrows(NegocioException.class, () -> servicio.registrar(
                comando("Marta", "311", BigDecimal.ZERO, MedioPago.EFECTIVO, new BigDecimal("1000"),
                        new LineaVenta(7, 10))));

        assertEquals("El monto recibido ($ 1.000) es menor al total ($ 30.000).", e.getMessage());
        verify(ventas, never()).insertarItem(anyInt(), anyInt(), anyInt(), any());
    }
}
