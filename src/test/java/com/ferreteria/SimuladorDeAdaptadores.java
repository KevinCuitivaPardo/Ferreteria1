package com.ferreteria;

import com.ferreteria.eventos.application.port.in.ConsultarEventos;
import com.ferreteria.geolocalizacion.application.port.in.Geocodificar;
import com.ferreteria.inventario.application.port.in.ConsultarInventario;
import com.ferreteria.inventario.application.port.in.GestionarProductos;
import com.ferreteria.inventario.application.port.in.MoverStock;
import com.ferreteria.inventario.application.port.in.StockParaVentas;
import com.ferreteria.pedidos.application.port.in.AsignarDomiciliario;
import com.ferreteria.pedidos.application.port.in.CambiarEstadoPedido;
import com.ferreteria.pedidos.application.port.in.ConsultarPedidos;
import com.ferreteria.pedidos.application.port.in.CorregirUbicacion;
import com.ferreteria.pedidos.application.port.in.CrearPedido;
import com.ferreteria.pedidos.application.port.in.GestionarEntrega;
import com.ferreteria.sucursales.application.port.in.ConsultarSucursales;
import com.ferreteria.sucursales.application.port.in.GestionarSucursales;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import com.ferreteria.usuarios.application.port.in.GestionarUsuarios;
import com.ferreteria.ventas.application.port.in.ConsultarVentas;
import com.ferreteria.ventas.application.port.in.RegistrarVenta;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sustituye por simulacros todos los puertos de entrada y la base de datos, para probar la parte web
 * (rutas, permisos, plantillas, WebSocket) sin PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class SimuladorDeAdaptadores {

    @MockBean ConsultarInventario inventario;
    @MockBean GestionarProductos productos;
    @MockBean MoverStock stock;
    @MockBean StockParaVentas stockParaVentas;
    @MockBean ConsultarPedidos pedidos;
    @MockBean CrearPedido crearPedido;
    @MockBean CambiarEstadoPedido cambiarEstado;
    @MockBean AsignarDomiciliario asignarDomiciliario;
    @MockBean CorregirUbicacion corregirUbicacion;
    @MockBean GestionarEntrega gestionarEntrega;
    @MockBean ConsultarVentas ventas;
    @MockBean RegistrarVenta registrarVenta;
    @MockBean ConsultarUsuarios usuarios;
    @MockBean GestionarUsuarios gestionUsuarios;
    @MockBean ConsultarSucursales sucursales;
    @MockBean GestionarSucursales gestionSucursales;
    @MockBean ConsultarEventos eventos;
    @MockBean Geocodificar geocodificar;
    @MockBean org.springframework.jdbc.core.JdbcTemplate jdbc;
    @MockBean javax.sql.DataSource dataSource;
}
