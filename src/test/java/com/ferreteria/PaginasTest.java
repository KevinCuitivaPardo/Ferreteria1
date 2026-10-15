package com.ferreteria;

import com.ferreteria.eventos.application.port.in.ConsultarEventos;
import com.ferreteria.eventos.domain.Evento;
import com.ferreteria.geolocalizacion.application.port.in.Geocodificar;
import com.ferreteria.geolocalizacion.domain.Coordenadas;
import com.ferreteria.inventario.application.port.in.ConsultarInventario;
import com.ferreteria.inventario.application.port.in.GestionarProductos;
import com.ferreteria.inventario.application.port.in.MoverStock;
import com.ferreteria.inventario.application.port.in.StockParaVentas;
import com.ferreteria.inventario.domain.ExistenciaSucursal;
import com.ferreteria.inventario.domain.Movimiento;
import com.ferreteria.inventario.domain.Producto;
import com.ferreteria.pedidos.application.port.in.AsignarDomiciliario;
import com.ferreteria.pedidos.application.port.in.CambiarEstadoPedido;
import com.ferreteria.pedidos.application.port.in.ConsultarPedidos;
import com.ferreteria.pedidos.application.port.in.CorregirUbicacion;
import com.ferreteria.pedidos.application.port.in.CrearPedido;
import com.ferreteria.pedidos.application.port.in.GestionarEntrega;
import com.ferreteria.pedidos.domain.ContenidoFoto;
import com.ferreteria.pedidos.domain.Domiciliario;
import com.ferreteria.pedidos.domain.FotoEntrega;
import com.ferreteria.pedidos.domain.FotoPedido;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.pedidos.domain.PedidoDetalle;
import com.ferreteria.pedidos.domain.PedidoItem;
import com.ferreteria.pedidos.domain.PedidoMapa;
import com.ferreteria.pedidos.domain.PedidoResumen;
import com.ferreteria.pedidos.domain.ProductoVendible;
import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.eventos.TipoEvento;
import com.ferreteria.sucursales.application.port.in.ConsultarSucursales;
import com.ferreteria.sucursales.application.port.in.GestionarSucursales;
import com.ferreteria.sucursales.domain.Sucursal;
import com.ferreteria.tiemporeal.application.port.in.ConsultarPosiciones;
import com.ferreteria.tiemporeal.application.port.in.PublicarAvisos;
import com.ferreteria.tiemporeal.application.port.in.ReportarPosicion;
import com.ferreteria.tiemporeal.domain.PosicionDomiciliario;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import com.ferreteria.usuarios.application.port.in.GestionarUsuarios;
import com.ferreteria.usuarios.domain.Rol;
import com.ferreteria.usuarios.domain.Usuario;
import com.ferreteria.ventas.application.port.in.ConsultarVentas;
import com.ferreteria.ventas.application.port.in.RegistrarVenta;
import com.ferreteria.ventas.domain.ProductoEnVenta;
import com.ferreteria.ventas.domain.VentaCabecera;
import com.ferreteria.ventas.domain.VentaDetalle;
import com.ferreteria.ventas.domain.VentaItem;
import com.ferreteria.ventas.domain.VentaResumen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.ferreteria.Sesiones.admin;
import static com.ferreteria.Sesiones.domiciliario;
import static com.ferreteria.Sesiones.vendedor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renderiza cada pantalla con los puertos de entrada simulados: prueba los adaptadores web
 * (rutas, permisos por rol, plantillas) sin base de datos.
 */
@AutoConfigureMockMvc
class PaginasTest extends SimuladorDeAdaptadores {

    @Autowired MockMvc mvc;

    @MockBean ConsultarPosiciones posiciones;
    @MockBean PublicarAvisos publicarAvisos;
    @MockBean ReportarPosicion reportarPosicion;

    private static final Sucursal SEDE = new Sucursal(1, "Sede principal", "Carrera 7 # 32-16", 4.6097, -74.0817, true);
    private static final Producto TORNILLO = new Producto(
            7, "DW-8", "Tornillo drywall", "8 x 2", new BigDecimal("1500"), new BigDecimal("3000"), 3, 5);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 3, 1, 8, 0);

    @BeforeEach
    void sucursalPorDefecto() {
        when(sucursales.porId(1)).thenReturn(Optional.of(SEDE));
        when(sucursales.activas()).thenReturn(List.of(SEDE));
        when(sucursales.listar()).thenReturn(List.of(SEDE));
    }

    // ---------- Acceso y permisos por rol ----------

    @Test
    void loginSeMuestraSinSesion() throws Exception {
        mvc.perform(get("/Login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresar")))
                .andExpect(content().string(containsString("/css/app.css")))
                .andExpect(content().string(containsString("Bienvenido")))
                .andExpect(content().string(containsString("id=\"ver-clave\"")))
                .andExpect(content().string(containsString("/js/login.js")))
                .andExpect(content().string(not(containsString("/js/tiempo-real.js"))))
                .andExpect(content().string(not(containsString("Usuario o contraseña incorrectos"))));
    }

    @Test
    void elLoginMuestraElErrorConElMensajeClaro() throws Exception {
        mvc.perform(get("/Login").param("error", "")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario o contraseña incorrectos")))
                .andExpect(content().string(containsString("con-error")));
    }

    // ---------- Cerrar sesión con doble verificación ----------

    @Test
    void cerrarSesionPideConfirmacionAntesDeSalir() throws Exception {
        mvc.perform(get("/").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dialogo-confirmar\"")))
                .andExpect(content().string(containsString("data-confirmar-titulo=\"¿Cerrar sesión?\"")))
                .andExpect(content().string(containsString("action=\"/Salir\"")))
                .andExpect(content().string(containsString("Tendrás que ingresar de nuevo")));

        // El domiciliario además es avisado de que dejará de compartir su ubicación
        mvc.perform(get("/").with(domiciliario())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Dejarás de compartir tu ubicación")));
    }

    @Test
    void unEnlaceASalirSoloMuestraLaConfirmacionYNoCierraLaSesion() throws Exception {
        mvc.perform(get("/Salir").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("¿Cerrar sesión?")))
                .andExpect(content().string(containsString("method=\"post\"")));
    }

    @Test
    void cerrarSesionRequiereUnPostConToken() throws Exception {
        // Sin token CSRF no se cierra
        mvc.perform(post("/Salir").with(vendedor())).andExpect(status().isForbidden());
        // Con token, sí, y vuelve al login
        mvc.perform(post("/Salir").with(vendedor()).with(csrf())).andExpect(redirectedUrl("/Login"));
    }

    @Test
    void sinSesionRedirigeAlLogin() throws Exception {
        mvc.perform(get("/Productos")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/Login"));
    }

    @Test
    void elAdminVeTodoEnElInicio() throws Exception {
        mvc.perform(get("/").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Bienvenido")))
                .andExpect(content().string(containsString("Registro de eventos")))
                .andExpect(content().string(containsString("Sucursales")))
                .andExpect(content().string(containsString("Mapa de domicilios")))
                .andExpect(content().string(containsString("Sede principal")))
                .andExpect(content().string(containsString("data-rol=\"admin\"")))
                .andExpect(content().string(containsString("/js/tiempo-real.js")))
                .andExpect(content().string(not(containsString("id=\"compartir-ubicacion\""))));
    }

    @Test
    void elVendedorSoloVeSusOpciones() throws Exception {
        mvc.perform(get("/").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Nueva venta")))
                .andExpect(content().string(containsString("Nuevo pedido")))
                .andExpect(content().string(not(containsString("/Descuentos"))))
                .andExpect(content().string(not(containsString("/Sucursales"))))
                .andExpect(content().string(not(containsString("Mis entregas"))));
    }

    @Test
    void elDomiciliarioSoloVeSusEntregasYComparteUbicacion() throws Exception {
        mvc.perform(get("/").with(domiciliario())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Mis entregas")))
                .andExpect(content().string(containsString("data-rol=\"domiciliario\"")))
                .andExpect(content().string(containsString("id=\"compartir-ubicacion\"")))
                .andExpect(content().string(containsString("/js/compartir-ubicacion.js")))
                .andExpect(content().string(not(containsString("Nueva venta"))))
                .andExpect(content().string(not(containsString("/Productos"))));
    }

    @Test
    void cadaRolSoloEntraDondeLeCorresponde() throws Exception {
        mvc.perform(get("/Sucursales").with(vendedor())).andExpect(status().isForbidden());
        mvc.perform(get("/Eventos").with(vendedor())).andExpect(status().isForbidden());
        mvc.perform(get("/Pedidos/Mapa").with(vendedor())).andExpect(status().isForbidden());
        mvc.perform(get("/Seguimiento/posiciones").with(vendedor())).andExpect(status().isForbidden());
        mvc.perform(get("/Seguimiento/posiciones").with(domiciliario())).andExpect(status().isForbidden());
        mvc.perform(get("/Entregas").with(vendedor())).andExpect(status().isForbidden());
        mvc.perform(get("/Editar/7").with(vendedor())).andExpect(status().isForbidden());
        mvc.perform(post("/Productos/7/eliminar").with(vendedor()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/Pedidos/Detalle/4/estado").with(vendedor()).with(csrf()).param("estado", "cancelado"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/Pedidos/Detalle/4/domiciliario").with(vendedor()).with(csrf()).param("domiciliarioId", "9"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/Pedidos/Detalle/4/ubicacion").with(vendedor()).with(csrf())
                        .param("latitud", "4.6").param("longitud", "-74.0"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/Productos").with(domiciliario())).andExpect(status().isForbidden());
        mvc.perform(get("/Ventas").with(domiciliario())).andExpect(status().isForbidden());
        mvc.perform(get("/Pedidos/Lista").with(domiciliario())).andExpect(status().isForbidden());
        mvc.perform(post("/Entregas/4/comenzar").with(admin()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/Entregas").with(admin())).andExpect(status().isForbidden());
    }

    // ---------- Inventario por sucursal ----------

    @Test
    void inventarioMuestraAlertaYAccionesDelAdmin() throws Exception {
        when(inventario.listar(1)).thenReturn(List.of(TORNILLO));
        mvc.perform(get("/Productos").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("fila-alerta")))
                .andExpect(content().string(containsString("$ 3.000")))
                .andExpect(content().string(containsString("/Editar/7")))
                .andExpect(content().string(containsString("/Productos/7/eliminar")))
                .andExpect(content().string(containsString("Sede principal")));
    }

    @Test
    void elVendedorVeElInventarioDeSuSucursalSinEditar() throws Exception {
        when(inventario.listar(1)).thenReturn(List.of(TORNILLO));
        mvc.perform(get("/Productos").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("/Movimientos/7")))
                .andExpect(content().string(not(containsString("/Editar/7"))));
        verify(inventario).listar(1);
    }

    @Test
    void busquedaDevuelveJsonDeLaSucursal() throws Exception {
        when(inventario.buscar("dw", 1)).thenReturn(List.of(TORNILLO));
        mvc.perform(get("/Productos/buscar").param("q", "dw").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"stockBajo\":true")))
                .andExpect(content().string(containsString("\"precioVenta\":3000")));
    }

    @Test
    void editarYNuevo() throws Exception {
        when(inventario.porId(7, 1)).thenReturn(Optional.of(TORNILLO));
        mvc.perform(get("/Editar/7").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("DW-8")))
                .andExpect(content().string(containsString("$ 3.000")));
        mvc.perform(get("/Nuevo").with(admin())).andExpect(status().isOk());
        mvc.perform(post("/Nuevo").with(admin()).with(csrf()).param("codigo", "X").param("nombre", "Y")
                        .param("precioCompra", "10").param("stock", "1").param("stockMinimo", "0"))
                .andExpect(redirectedUrl("/Productos"));
    }

    @Test
    void movimientosMuestraHistorialYExistenciasPorSucursal() throws Exception {
        when(inventario.porId(7, 1)).thenReturn(Optional.of(TORNILLO));
        when(inventario.historial(7, 1)).thenReturn(List.of(
                new Movimiento(10, "compra", LocalDateTime.of(2026, 1, 5, 9, 30)),
                new Movimiento(-2, "venta", LocalDateTime.of(2026, 1, 6, 10, 0))));
        when(inventario.existencias(7)).thenReturn(List.of(
                new ExistenciaSucursal(1, "Sede principal", 3), new ExistenciaSucursal(2, "Sucursal Norte", 80)));

        mvc.perform(get("/Movimientos/7").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("05/01/2026 09:30")))
                .andExpect(content().string(containsString("Salida")))
                .andExpect(content().string(containsString("Sucursal Norte")));
        mvc.perform(get("/Ajustar/7").with(admin())).andExpect(status().isOk());
    }

    // ---------- Ventas en el local ----------

    @Test
    void listaYDetalleDeVentasConMedioDePago() throws Exception {
        when(ventas.recientes(anyString(), anyBoolean())).thenReturn(List.of(new VentaResumen(
                3, "VEN-00003", LocalDateTime.of(2026, 3, 2, 10, 15), "Marta Ríos", "3115550000", "ana",
                "Sede principal", new BigDecimal("12500"), MedioPago.NEQUI)));
        mvc.perform(get("/Ventas").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("VEN-00003")))
                .andExpect(content().string(containsString("Marta Ríos")))
                .andExpect(content().string(containsString("3115550000")))
                .andExpect(content().string(containsString("Nequi")))
                .andExpect(content().string(containsString("$ 12.500")));

        var cabecera = new VentaCabecera(3, "VEN-00003", LocalDateTime.of(2026, 3, 2, 10, 15), "Marta Ríos",
                "3115550000", "ana", "Sede principal", new BigDecimal("5"), new BigDecimal("28500"),
                MedioPago.EFECTIVO, new BigDecimal("50000"));
        when(ventas.detalle(eq(3), anyString(), anyBoolean())).thenReturn(Optional.of(new VentaDetalle(cabecera,
                List.of(new VentaItem("DW-8", "Tornillo", "8 x 2", 10, new BigDecimal("3000"))))));
        mvc.perform(get("/Ventas/3").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Quién compró")))
                .andExpect(content().string(containsString("Efectivo")))
                .andExpect(content().string(containsString("$ 50.000")))
                .andExpect(content().string(containsString("$ 21.500")))
                .andExpect(content().string(containsString("$ 30.000")));

        when(ventas.detalle(eq(9), anyString(), anyBoolean())).thenReturn(Optional.empty());
        mvc.perform(get("/Ventas/9").with(vendedor())).andExpect(status().isNotFound());
    }

    @Test
    void nuevaVentaPideNombreNumeroYMedioDePagoYSeRegistra() throws Exception {
        when(ventas.descuentoMaximo("ana")).thenReturn(new BigDecimal("5"));
        when(ventas.productosDisponibles(1)).thenReturn(List.of(
                new ProductoEnVenta(7, "DW-8", "Tornillo", "8 x 2", new BigDecimal("3000"), 50)));
        mvc.perform(get("/Ventas/Nueva").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"clienteNombre\"")))
                .andExpect(content().string(containsString("name=\"clienteTelefono\"")))
                .andExpect(content().string(containsString("name=\"medioPago\"")))
                .andExpect(content().string(containsString("Transferencia")))
                .andExpect(content().string(containsString("Daviplata")))
                .andExpect(content().string(containsString("name=\"montoRecibido\"")))
                .andExpect(content().string(containsString("data-precio=\"3000\"")));

        when(registrarVenta.registrar(any())).thenReturn(12);
        mvc.perform(post("/Ventas/Nueva").with(vendedor()).with(csrf())
                        .param("clienteNombre", "Marta").param("clienteTelefono", "311")
                        .param("lineas[0].productoId", "7").param("lineas[0].cantidad", "2")
                        .param("descuentoElegido", "5").param("medioPago", "tarjeta"))
                .andExpect(redirectedUrl("/Ventas/12"));

        var comando = ArgumentCaptor.forClass(RegistrarVenta.Comando.class);
        verify(registrarVenta).registrar(comando.capture());
        assertEquals(MedioPago.TARJETA, comando.getValue().medioPago());
        assertNull(comando.getValue().montoRecibido());
    }

    // ---------- Pedidos (domicilios) ----------

    private PedidoCabecera pedido(String estado, Integer domiciliarioId, String domiciliario,
                                  LocalDateTime iniciado, LocalDateTime finalizado) {
        return new PedidoCabecera(4, "PED-00004", FECHA, estado,
                new BigDecimal("5"), new BigDecimal("28500"), "Ferro", "Luis", "Calle 45 # 12-30", null, null,
                "3001234567", null, "ana", 1, "Sede principal", 4.6097, -74.0817, 4.65, -74.06,
                domiciliarioId, domiciliario, MedioPago.EFECTIVO, new BigDecimal("50000"),
                domiciliarioId == null ? null : FECHA.plusMinutes(5), iniciado, finalizado, null);
    }

    private PedidoResumen resumen(int id, String numero, String cliente, String estado, String domiciliario,
                                  MedioPago medio) {
        return new PedidoResumen(id, numero, FECHA, "Ferro", cliente, "Calle 45 # 12-30", "ana", "Sede principal",
                domiciliario, new BigDecimal("28500"), estado, medio, FECHA.plusMinutes(5), 1);
    }

    private PedidoDetalle detallePedido(PedidoCabecera cabecera, List<FotoPedido> fotos) {
        return new PedidoDetalle(cabecera,
                List.of(new PedidoItem("DW-8", "Tornillo", "8 x 2", 10, new BigDecimal("3000"))), fotos);
    }

    @Test
    void listaDePedidosMuestraDireccionDomiciliarioYPago() throws Exception {
        when(pedidos.recientes(anyString(), anyBoolean())).thenReturn(List.of(
                resumen(4, "PED-00004", "Luis", "en_ruta", "pedro", MedioPago.DAVIPLATA)));
        mvc.perform(get("/Pedidos/Lista").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("estado-en_ruta")))
                .andExpect(content().string(containsString("Calle 45 # 12-30")))
                .andExpect(content().string(containsString("pedro")))
                .andExpect(content().string(containsString("Daviplata")))
                .andExpect(content().string(containsString("$ 28.500")));
    }

    @Test
    void detalleDelPedidoParaElAdminPermiteAsignarYTraeElMapa() throws Exception {
        when(pedidos.detalle(eq(4), anyString(), eq(true))).thenReturn(Optional.of(
                detallePedido(pedido("pendiente", null, null, null, null), List.of())));
        when(pedidos.domiciliariosDe(1)).thenReturn(List.of(new Domiciliario(9, "pedro", true, 1)));

        mvc.perform(get("/Pedidos/Detalle/4").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Gestionar pedido")))
                .andExpect(content().string(containsString("Ajustar el punto de entrega")))
                .andExpect(content().string(containsString("pedro")))
                .andExpect(content().string(containsString("Efectivo")))
                .andExpect(content().string(containsString("$ 50.000")))
                .andExpect(content().string(containsString("$ 21.500")))
                .andExpect(content().string(containsString("data-lat=\"4.65\"")))
                .andExpect(content().string(containsString("data-origen-lat=\"4.6097\"")))
                .andExpect(content().string(containsString("$ 30.000")));

        mvc.perform(post("/Pedidos/Detalle/4/domiciliario").with(admin()).with(csrf()).param("domiciliarioId", "9"))
                .andExpect(redirectedUrl("/Pedidos/Detalle/4"));
        verify(asignarDomiciliario).asignar(4, 9);

        mvc.perform(post("/Pedidos/Detalle/4/ubicacion").with(admin()).with(csrf())
                        .param("latitud", "4.66").param("longitud", "-74.05"))
                .andExpect(redirectedUrl("/Pedidos/Detalle/4"));
        verify(corregirUbicacion).corregir(4, 4.66, -74.05);
    }

    @Test
    void elAdminVeElAvanceDeLaEntregaYLaFotoParaRevisarla() throws Exception {
        var cabecera = pedido("entregado", 9, "pedro", FECHA.plusMinutes(10), FECHA.plusMinutes(55));
        var foto = new FotoPedido(31, 4, "image/jpeg", 120_000, "pedro", FECHA.plusMinutes(54));
        when(pedidos.detalle(eq(4), anyString(), eq(true))).thenReturn(Optional.of(detallePedido(cabecera, List.of(foto))));

        mvc.perform(get("/Pedidos/Detalle/4").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Avance de la entrega")))
                .andExpect(content().string(containsString("Tardó 45 min")))
                .andExpect(content().string(containsString("Foto de la entrega")))
                .andExpect(content().string(containsString("/Fotos/4/31")))
                .andExpect(content().string(containsString("Tomada por")));
    }

    @Test
    void conElPedidoEnRutaElAdminVeAlConductorEnVivo() throws Exception {
        var cabecera = pedido("en_ruta", 9, "pedro", FECHA.plusMinutes(10), null);
        when(pedidos.detalle(eq(4), anyString(), eq(true))).thenReturn(Optional.of(detallePedido(cabecera, List.of())));
        when(pedidos.domiciliariosDe(1)).thenReturn(List.of(new Domiciliario(9, "pedro", true, 1)));

        mvc.perform(get("/Pedidos/Detalle/4").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-conductor-id=\"9\"")))
                .andExpect(content().string(containsString("va en camino")))
                .andExpect(content().string(containsString("no se puede cambiar el domiciliario")));
    }

    @Test
    void elVendedorVeSuPedidoSinGestion() throws Exception {
        when(pedidos.detalle(eq(4), anyString(), eq(false))).thenReturn(Optional.of(
                detallePedido(pedido("en_ruta", 9, "pedro", FECHA.plusMinutes(10), null), List.of())));
        mvc.perform(get("/Pedidos/Detalle/4").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("En ruta")))
                .andExpect(content().string(not(containsString("Gestionar pedido"))))
                .andExpect(content().string(not(containsString("Ajustar el punto de entrega"))));

        when(pedidos.detalle(eq(9), anyString(), eq(false))).thenReturn(Optional.empty());
        mvc.perform(get("/Pedidos/Detalle/9").with(vendedor())).andExpect(status().isNotFound());
    }

    @Test
    void nuevoPedidoPideDireccionYMedioDePago() throws Exception {
        when(pedidos.descuentoMaximo("ana")).thenReturn(new BigDecimal("5"));
        when(pedidos.productosDisponibles(1)).thenReturn(List.of(
                new ProductoVendible(7, "DW-8", "Tornillo", "8 x 2", new BigDecimal("3000"), 50)));
        mvc.perform(get("/Pedidos/Nuevo").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Dirección de entrega")))
                .andExpect(content().string(containsString("name=\"medioPago\"")))
                .andExpect(content().string(containsString("Nequi")))
                .andExpect(content().string(containsString("lineas[9].productoId")))
                .andExpect(content().string(containsString("data-precio=\"3000\"")));

        when(crearPedido.crear(any())).thenReturn(12);
        mvc.perform(post("/Pedidos/Nuevo").with(vendedor()).with(csrf())
                        .param("cliente.celular1", "300").param("cliente.nombreCliente", "Luis")
                        .param("cliente.direccion", "Calle 1").param("lineas[0].productoId", "7")
                        .param("lineas[0].cantidad", "2").param("descuentoElegido", "5")
                        .param("medioPago", "efectivo").param("montoRecibido", "50000"))
                .andExpect(redirectedUrl("/Pedidos/Detalle/12"));

        var comando = ArgumentCaptor.forClass(CrearPedido.Comando.class);
        verify(crearPedido).crear(comando.capture());
        assertEquals(MedioPago.EFECTIVO, comando.getValue().medioPago());
        assertEquals(0, new BigDecimal("50000").compareTo(comando.getValue().montoRecibido()));
    }

    @Test
    void mapaDeDomiciliosMuestraLosPedidosPorAsignarConSusDomiciliarios() throws Exception {
        when(pedidos.paraElMapa()).thenReturn(List.of(new PedidoMapa(
                4, "PED-00004", "en_ruta", "Luis", "Calle 45", 4.65, -74.06, "Sede principal", "pedro")));
        when(pedidos.sinAsignar()).thenReturn(List.of(resumen(5, "PED-00005", "Sandra", "pendiente", null, MedioPago.EFECTIVO)));
        when(pedidos.domiciliariosDe(1)).thenReturn(List.of(new Domiciliario(9, "pedro", true, 1)));

        mvc.perform(get("/Pedidos/Mapa").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("mapa-pedidos")))
                .andExpect(content().string(containsString("lista-conductores")))
                .andExpect(content().string(containsString("Por asignar")))
                .andExpect(content().string(containsString("PED-00005")))
                .andExpect(content().string(containsString("name=\"volver\" value=\"mapa\"")))
                .andExpect(content().string(containsString("pedro")))
                .andExpect(content().string(containsString("/vendor/leaflet/leaflet.js")));
        mvc.perform(get("/Pedidos/Mapa/datos").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"latitud\":4.65")))
                .andExpect(content().string(containsString("\"estado\":\"en_ruta\"")));
        mvc.perform(get("/Sucursales/datos").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Sede principal")));

        mvc.perform(post("/Pedidos/Detalle/5/domiciliario").with(admin()).with(csrf())
                        .param("domiciliarioId", "9").param("volver", "mapa"))
                .andExpect(redirectedUrl("/Pedidos/Mapa"));
    }

    @Test
    void elAdminVeDondeEstanLosDomiciliarios() throws Exception {
        when(posiciones.actuales()).thenReturn(List.of(new PosicionDomiciliario(
                9, "pedro", 1, 4.65, -74.06, 12.0, Instant.parse("2026-03-01T13:00:00Z"))));

        mvc.perform(get("/Seguimiento/posiciones").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"nombre\":\"pedro\"")))
                .andExpect(content().string(containsString("\"latitud\":4.65")))
                .andExpect(content().string(containsString("2026-03-01T13:00:00Z")));
    }

    // ---------- Cola y entregas del domiciliario ----------

    @Test
    void laColaMuestraEnRutaEnEsperaEnOrdenYCerradas() throws Exception {
        when(pedidos.entregasDe(9)).thenReturn(List.of(
                resumen(4, "PED-00004", "Luis", "en_ruta", "pedro", MedioPago.EFECTIVO),
                resumen(5, "PED-00005", "Sandra", "pendiente", "pedro", MedioPago.NEQUI),
                resumen(6, "PED-00006", "Carlos", "alistado", "pedro", MedioPago.EFECTIVO),
                resumen(3, "PED-00003", "Marta", "entregado", "pedro", MedioPago.EFECTIVO)));

        mvc.perform(get("/Entregas").with(domiciliario())).andExpect(status().isOk())
                .andExpect(content().string(containsString("En ruta ahora")))
                .andExpect(content().string(containsString("Cobrar")))
                .andExpect(content().string(containsString("en efectivo")))
                .andExpect(content().string(containsString("$ 28.500")))
                .andExpect(content().string(containsString("En espera")))
                .andExpect(content().string(containsString("PED-00005")))
                .andExpect(content().string(containsString("PED-00006")))
                .andExpect(content().string(containsString("Terminadas")))
                .andExpect(content().string(containsString("/Entregas/5/comenzar")))
                // con una entrega en ruta, las demás no se pueden comenzar
                .andExpect(content().string(containsString("disabled")));
    }

    @Test
    void sinEntregaEnRutaSePuedeComenzarLaPrimeraDeLaCola() throws Exception {
        when(pedidos.entregasDe(9)).thenReturn(List.of(
                resumen(5, "PED-00005", "Sandra", "pendiente", "pedro", MedioPago.NEQUI)));

        mvc.perform(get("/Entregas").with(domiciliario())).andExpect(status().isOk())
                .andExpect(content().string(containsString("/Entregas/5/comenzar")))
                .andExpect(content().string(not(containsString("En ruta ahora"))))
                .andExpect(content().string(not(containsString("Termina la entrega que llevas"))));

        mvc.perform(post("/Entregas/5/comenzar").with(domiciliario()).with(csrf()))
                .andExpect(redirectedUrl("/Entregas/5"));
        verify(gestionarEntrega).comenzar(5, 9);
    }

    @Test
    void elDetalleDeLaEntregaTraeRutaCobroYCierre() throws Exception {
        var enRuta = pedido("en_ruta", 9, "pedro", FECHA.plusMinutes(10), null);
        when(pedidos.entrega(4, 9)).thenReturn(Optional.of(detallePedido(enRuta, List.of())));
        when(pedidos.entregasDe(9)).thenReturn(List.of(resumen(4, "PED-00004", "Luis", "en_ruta", "pedro", MedioPago.EFECTIVO)));

        mvc.perform(get("/Entregas/4").with(domiciliario())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ruta de entrega")))
                .andExpect(content().string(containsString("data-modo=\"conductor\"")))
                .andExpect(content().string(containsString("Hizo el pedido")))
                .andExpect(content().string(containsString("Cobra en efectivo")))
                .andExpect(content().string(containsString("$ 28.500")))
                .andExpect(content().string(containsString("$ 21.500")))
                .andExpect(content().string(containsString("de cambio")))
                .andExpect(content().string(containsString("Terminar la entrega")))
                .andExpect(content().string(containsString("type=\"file\"")))
                .andExpect(content().string(containsString("/js/foto-entrega.js")))
                .andExpect(content().string(containsString("_csrf=")))
                .andExpect(content().string(not(containsString("Comenzar entrega"))));
    }

    @Test
    void unPedidoPagadoPorTransferenciaNoSeCobra() throws Exception {
        var cabecera = new PedidoCabecera(4, "PED-00004", FECHA, "pendiente", BigDecimal.ZERO, new BigDecimal("28500"),
                null, "Luis", "Calle 45", null, null, "300", null, "ana", 1, "Sede principal", 4.6, -74.0,
                4.65, -74.06, 9, "pedro", MedioPago.TRANSFERENCIA, null, FECHA, null, null, null);
        when(pedidos.entrega(4, 9)).thenReturn(Optional.of(detallePedido(cabecera, List.of())));
        when(pedidos.entregasDe(9)).thenReturn(List.of());

        mvc.perform(get("/Entregas/4").with(domiciliario())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya está pagado: no cobres nada")))
                .andExpect(content().string(containsString("Transferencia")))
                .andExpect(content().string(containsString("Comenzar entrega")))
                .andExpect(content().string(not(containsString("Cobra en efectivo"))));
    }

    @Test
    void noSePuedeComenzarOtraEntregaMientrasHayUnaEnRuta() throws Exception {
        var pendiente = pedido("pendiente", 9, "pedro", null, null);
        when(pedidos.entrega(4, 9)).thenReturn(Optional.of(detallePedido(pendiente, List.of())));
        when(pedidos.entregasDe(9)).thenReturn(List.of(
                resumen(7, "PED-00007", "Otro", "en_ruta", "pedro", MedioPago.EFECTIVO),
                resumen(4, "PED-00004", "Luis", "pendiente", "pedro", MedioPago.EFECTIVO)));

        mvc.perform(get("/Entregas/4").with(domiciliario())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ya llevas otra entrega en ruta")))
                .andExpect(content().string(not(containsString("Comenzar entrega"))));
    }

    @Test
    void terminarConFotoLlegaAlCasoDeUso() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3, 4};
        when(gestionarEntrega.terminar(eq(4), eq(9), eq(GestionarEntrega.Resultado.ENTREGADO), anyString(), any()))
                .thenReturn("¡Entrega terminada!");

        mvc.perform(multipart("/Entregas/4/terminar").file(new MockMultipartFile("foto", "f.jpg", "image/jpeg", jpeg))
                        .param("resultado", "entregado").param("nota", "Lo recibió el encargado")
                        .with(domiciliario()).with(csrf()))
                .andExpect(redirectedUrl("/Entregas"));

        var foto = ArgumentCaptor.forClass(FotoEntrega.class);
        verify(gestionarEntrega).terminar(eq(4), eq(9), eq(GestionarEntrega.Resultado.ENTREGADO),
                eq("Lo recibió el encargado"), foto.capture());
        assertEquals("image/jpeg", foto.getValue().tipoContenido());
    }

    @Test
    void unArchivoQueNoEsImagenSeRechazaYNoLlegaAlCasoDeUso() throws Exception {
        mvc.perform(multipart("/Entregas/4/terminar")
                        .file(new MockMultipartFile("foto", "virus.jpg", "image/jpeg", "no soy una imagen".getBytes()))
                        .param("resultado", "entregado").with(domiciliario()).with(csrf()))
                .andExpect(redirectedUrl("/Entregas/4"));

        org.mockito.Mockito.verifyNoInteractions(gestionarEntrega);
    }

    @Test
    void noEntregadoSeEnviaSinFotoConSuMotivo() throws Exception {
        mvc.perform(multipart("/Entregas/4/terminar").param("resultado", "no_entregado")
                        .param("nota", "Nadie atendió").with(domiciliario()).with(csrf()))
                .andExpect(redirectedUrl("/Entregas"));

        verify(gestionarEntrega).terminar(eq(4), eq(9), eq(GestionarEntrega.Resultado.NO_ENTREGADO),
                eq("Nadie atendió"), isNull());
    }

    // ---------- Fotos de la entrega ----------

    @Test
    void lasFotosSoloSeSirvenAQuienTieneDerecho() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
        when(pedidos.foto(4, 31, 1, true)).thenReturn(Optional.of(new ContenidoFoto(jpeg, "image/jpeg")));
        when(pedidos.foto(4, 31, 2, false)).thenReturn(Optional.empty());

        mvc.perform(get("/Fotos/4/31").with(admin())).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(jpeg));
        mvc.perform(get("/Fotos/4/31").with(vendedor())).andExpect(status().isNotFound());
        mvc.perform(get("/Fotos/4/31")).andExpect(status().is3xxRedirection());
    }

    // ---------- Administración ----------

    @Test
    void sucursalesSeListanYSeCrean() throws Exception {
        mvc.perform(get("/Sucursales").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Sede principal")))
                .andExpect(content().string(containsString("Carrera 7 # 32-16")));
        mvc.perform(get("/Sucursales/Nueva").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("mapa-sucursal")))
                .andExpect(content().string(containsString("Buscar la dirección en el mapa")));
        mvc.perform(post("/Sucursales/Nueva").with(admin()).with(csrf()).param("nombre", "Norte")
                        .param("direccion", "Calle 100 # 15-20").param("latitud", "4.68").param("longitud", "-74.05"))
                .andExpect(redirectedUrl("/Sucursales"));
        verify(gestionSucursales).crear(any());
    }

    @Test
    void elAdminCambiaDeSucursalActiva() throws Exception {
        mvc.perform(post("/Sucursales/activa").with(admin()).with(csrf()).param("id", "1")
                        .header("Referer", "http://localhost:5107/Productos?x=1"))
                .andExpect(redirectedUrl("/Productos?x=1"));
    }

    @Test
    void registroDeEventos() throws Exception {
        when(eventos.recientes(any(), any())).thenReturn(List.of(new Evento(
                1, LocalDateTime.of(2026, 3, 2, 10, 15), TipoEvento.VENTA, "ana", "Sede principal",
                "Venta VEN-00003 a Marta Ríos por $28500")));
        mvc.perform(get("/Eventos").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Venta VEN-00003")))
                .andExpect(content().string(containsString("ana")));
    }

    @Test
    void usuariosPermiteElegirRolYSucursal() throws Exception {
        var pedro = new Usuario(9, "pedro", "x", Rol.DOMICILIARIO, true, BigDecimal.ZERO, 1);
        when(usuarios.listar()).thenReturn(List.of(
                new Usuario(1, "admin", "x", Rol.ADMIN, true, BigDecimal.ZERO, null), pedro));
        mvc.perform(get("/Usuarios").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("Domiciliario")))
                .andExpect(content().string(containsString("Sede principal")))
                .andExpect(content().string(containsString("Todas")));
    }

    @Test
    void descuentos() throws Exception {
        var ana = new Usuario(2, "ana", "x", Rol.VENDEDOR, true, new BigDecimal("5"), 1);
        when(usuarios.vendedores()).thenReturn(List.of(ana));
        mvc.perform(get("/Descuentos").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("filas[0].descuentoPct")));
    }

    @Test
    void sePuedeQuitarElDescuentoDeUnVendedorConConfirmacion() throws Exception {
        var ana = new Usuario(2, "ana", "x", Rol.VENDEDOR, true, new BigDecimal("5"), 1);
        var luis = new Usuario(3, "luis2", "x", Rol.VENDEDOR, true, BigDecimal.ZERO, 1);
        when(usuarios.vendedores()).thenReturn(List.of(ana, luis));

        mvc.perform(get("/Descuentos").with(admin())).andExpect(status().isOk())
                .andExpect(content().string(containsString("/Descuentos/2/quitar")))
                .andExpect(content().string(containsString("data-confirmar-titulo=\"¿Quitar el descuento?\"")))
                .andExpect(content().string(containsString("ana dejará de tener descuento")));

        mvc.perform(post("/Descuentos/2/quitar").with(admin()).with(csrf())).andExpect(redirectedUrl("/Descuentos"));
        verify(gestionUsuarios).quitarDescuento(2);

        mvc.perform(post("/Descuentos/2/quitar").with(vendedor()).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void geocodificarDevuelveCoordenadas() throws Exception {
        when(geocodificar.ubicar("Calle 45")).thenReturn(Optional.of(new Coordenadas(4.65, -74.06, "Calle 45, Bogotá")));
        mvc.perform(get("/api/geocodificar").param("q", "Calle 45").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"encontrado\":true")))
                .andExpect(content().string(containsString("\"latitud\":4.65")));
        mvc.perform(get("/api/geocodificar").param("q", "xyz").with(vendedor())).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"encontrado\":false")));
    }
}
