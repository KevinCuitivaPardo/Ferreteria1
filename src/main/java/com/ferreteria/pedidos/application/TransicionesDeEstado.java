package com.ferreteria.pedidos.application;

import com.ferreteria.pedidos.application.port.out.CatalogoProductos;
import com.ferreteria.pedidos.application.port.out.PedidoRepository;
import com.ferreteria.pedidos.domain.EstadoActual;
import com.ferreteria.pedidos.domain.EstadoPedido;
import com.ferreteria.pedidos.domain.LineaStock;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * El cambio de estado de un pedido y lo que arrastra: devolver o volver a descontar el stock de la
 * sucursal y dejar el evento. Lo usan el administrador y el domiciliario, y debe llamarse dentro de una transacción.
 */
@Component
public class TransicionesDeEstado {

    private final PedidoRepository pedidos;
    private final CatalogoProductos catalogo;
    private final RegistroEventos eventos;

    public TransicionesDeEstado(PedidoRepository pedidos, CatalogoProductos catalogo, RegistroEventos eventos) {
        this.pedidos = pedidos;
        this.catalogo = catalogo;
        this.eventos = eventos;
    }

    /**
     * Si el pedido pasa a "No entregado" o "Cancelado" el stock vuelve al inventario de su sucursal;
     * si vuelve a un estado activo se descuenta otra vez.
     */
    public String aplicar(int pedidoId, EstadoPedido nuevo, EstadoActual actual) {
        if (actual.clave().equals(nuevo.getClave())) {
            return "El pedido ya está en ese estado.";
        }

        boolean eraDevuelto = EstadoPedido.deClave(actual.clave()).map(EstadoPedido::devuelveStock).orElse(false);

        if (!eraDevuelto && nuevo.devuelveStock()) {
            for (LineaStock linea : pedidos.lineasDeStock(pedidoId)) {
                catalogo.reponer(linea.productoId(), actual.sucursalId(), linea.cantidad());
            }
        } else if (eraDevuelto && !nuevo.devuelveStock()) {
            reservarStockOtraVez(pedidoId, actual.sucursalId());
        }

        pedidos.actualizarEstado(pedidoId, nuevo.getClave());
        eventos.registrar(TipoEvento.ESTADO_PEDIDO, actual.sucursalId(), "Pedido #" + pedidoId + ": de "
                + EstadoPedido.etiquetaDe(actual.clave()) + " a " + nuevo.getEtiqueta());
        return "Estado cambiado a " + nuevo.getEtiqueta() + ".";
    }

    private void reservarStockOtraVez(int pedidoId, int sucursalId) {
        List<LineaStock> lineas = pedidos.lineasDeStock(pedidoId);

        for (LineaStock linea : lineas) {
            boolean alcanza = catalogo.bloquearParaVenta(linea.productoId(), sucursalId)
                    .map(p -> p.stock() >= linea.cantidad())
                    .orElse(false);
            if (!alcanza) {
                throw new NegocioException("No hay stock suficiente para volver a activar este pedido.");
            }
        }
        for (LineaStock linea : lineas) {
            catalogo.descontar(linea.productoId(), sucursalId, linea.cantidad());
        }
    }
}
