package com.ferreteria.pedidos.application.port.in;

import com.ferreteria.pedidos.domain.Cliente;
import com.ferreteria.pedidos.domain.ContenidoFoto;
import com.ferreteria.pedidos.domain.Domiciliario;
import com.ferreteria.pedidos.domain.PedidoDetalle;
import com.ferreteria.pedidos.domain.PedidoMapa;
import com.ferreteria.pedidos.domain.PedidoResumen;
import com.ferreteria.pedidos.domain.ProductoVendible;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ConsultarPedidos {

    /** Pedidos de los últimos 3 meses: todos para el admin, solo los propios para un vendedor. */
    List<PedidoResumen> recientes(String nombreUsuario, boolean esAdmin);

    /** Pedidos que todavía no tienen domiciliario y siguen por entregar, los más viejos primero. */
    List<PedidoResumen> sinAsignar();

    /** Un vendedor solo puede ver sus propios pedidos. */
    Optional<PedidoDetalle> detalle(int pedidoId, String nombreUsuario, boolean esAdmin);

    /** Pedidos ubicados en el mapa (últimos 3 meses), para el administrador. */
    List<PedidoMapa> paraElMapa();

    List<ProductoVendible> productosDisponibles(int sucursalId);

    BigDecimal descuentoMaximo(String nombreUsuario);

    Optional<Cliente> buscarCliente(String celular);

    /** Domiciliarios activos de una sucursal, para asignarles pedidos. */
    List<Domiciliario> domiciliariosDe(int sucursalId);

    /**
     * La cola de un domiciliario: primero la entrega en ruta, luego las que esperan (en el orden en que
     * se le asignaron) y al final las ya cerradas.
     */
    List<PedidoResumen> entregasDe(int domiciliarioId);

    /** El detalle de una entrega, solo si está asignada a ese domiciliario. */
    Optional<PedidoDetalle> entrega(int pedidoId, int domiciliarioId);

    /**
     * El archivo de una foto de entrega. La ven el administrador, el vendedor dueño del pedido y el
     * domiciliario asignado; para cualquier otra persona devuelve vacío.
     */
    Optional<ContenidoFoto> foto(int pedidoId, int fotoId, int usuarioId, boolean esAdmin);
}
