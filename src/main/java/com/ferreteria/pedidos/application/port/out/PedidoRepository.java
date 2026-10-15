package com.ferreteria.pedidos.application.port.out;

import com.ferreteria.pedidos.domain.EstadoActual;
import com.ferreteria.pedidos.domain.FotoGuardada;
import com.ferreteria.pedidos.domain.FotoPedido;
import com.ferreteria.pedidos.domain.LineaStock;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.pedidos.domain.PedidoGuardado;
import com.ferreteria.pedidos.domain.PedidoItem;
import com.ferreteria.pedidos.domain.PedidoMapa;
import com.ferreteria.pedidos.domain.PedidoResumen;
import com.ferreteria.pedidos.domain.Ubicacion;
import com.ferreteria.shared.MedioPago;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de pedidos. */
public interface PedidoRepository {

    /** Pedidos de los últimos 3 meses. Con {@code vendedorId} nulo trae los de todos. */
    List<PedidoResumen> recientes(Integer vendedorId);

    /** Pedidos por entregar que no tienen domiciliario, los más viejos primero. */
    List<PedidoResumen> sinAsignar();

    /** Si no es admin, solo devuelve el pedido cuando es del vendedor indicado. */
    Optional<PedidoCabecera> cabecera(int pedidoId, boolean esAdmin, int vendedorId);

    /** Solo devuelve el pedido si está asignado a ese domiciliario. */
    Optional<PedidoCabecera> cabeceraDeEntrega(int pedidoId, int domiciliarioId);

    List<PedidoItem> items(int pedidoId);

    /** Lo que el pedido reserva de cada producto. */
    List<LineaStock> lineasDeStock(int pedidoId);

    List<PedidoMapa> ubicados();

    /** La cola de un domiciliario: en ruta primero, luego en espera por orden de asignación, luego las cerradas. */
    List<PedidoResumen> entregasDe(int domiciliarioId);

    /** El pedido que el domiciliario lleva en ruta ahora, si hay. */
    Optional<Integer> entregaEnCurso(int domiciliarioId);

    PedidoGuardado insertar(int clienteId, int vendedorId, int sucursalId, String direccionEntrega,
                            Ubicacion ubicacion, BigDecimal descuentoPct, BigDecimal total,
                            MedioPago medioPago, BigDecimal montoRecibido);

    void insertarItem(int pedidoId, int productoId, int cantidad, BigDecimal precioUnitario);

    /** Lee el estado y bloquea el pedido hasta que termine la transacción. */
    Optional<EstadoActual> estadoBloqueado(int pedidoId);

    /**
     * Cambia el estado y anota solo las horas de salida y de cierre.
     *
     * @throws com.ferreteria.pedidos.domain.EntregaEnCursoException si el domiciliario ya lleva otro pedido en ruta
     */
    void actualizarEstado(int pedidoId, String claveEstado);

    void guardarNotaDeEntrega(int pedidoId, String nota);

    /** Con {@code null} se quita la asignación. Al asignar queda anotada la hora, que ordena la cola. */
    void asignarDomiciliario(int pedidoId, Integer domiciliarioId);

    void actualizarUbicacion(int pedidoId, Ubicacion ubicacion);

    // ---------- Fotos de la entrega ----------

    int insertarFoto(int pedidoId, String archivo, String tipoContenido, int tamano, int subidaPor);

    List<FotoPedido> fotos(int pedidoId);

    Optional<FotoGuardada> foto(int pedidoId, int fotoId);
}
