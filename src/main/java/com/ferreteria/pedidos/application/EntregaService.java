package com.ferreteria.pedidos.application;

import com.ferreteria.pedidos.application.port.in.GestionarEntrega;
import com.ferreteria.pedidos.application.port.out.AlmacenFotos;
import com.ferreteria.pedidos.application.port.out.AvisosDeEntrega;
import com.ferreteria.pedidos.application.port.out.PedidoRepository;
import com.ferreteria.pedidos.domain.EstadoActual;
import com.ferreteria.pedidos.domain.EstadoPedido;
import com.ferreteria.pedidos.domain.FotoEntrega;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/** La cola del domiciliario: comenzar una entrega, llevarla y terminarla con su foto. */
@Service
public class EntregaService implements GestionarEntrega {

    private record Cierre(String mensaje, PedidoCabecera pedido) {
    }

    private final PedidoRepository pedidos;
    private final AlmacenFotos fotos;
    private final AvisosDeEntrega avisos;
    private final RegistroEventos eventos;
    private final TransicionesDeEstado transiciones;
    private final TransactionTemplate transaccion;

    public EntregaService(PedidoRepository pedidos, AlmacenFotos fotos, AvisosDeEntrega avisos,
                          RegistroEventos eventos, TransicionesDeEstado transiciones,
                          TransactionTemplate transaccion) {
        this.pedidos = pedidos;
        this.fotos = fotos;
        this.avisos = avisos;
        this.eventos = eventos;
        this.transiciones = transiciones;
        this.transaccion = transaccion;
    }

    // ---------- Comenzar ----------

    @Override
    public String comenzar(int pedidoId, int domiciliarioId) {
        return transaccion.execute(estado -> {
            EstadoActual actual = propio(pedidoId, domiciliarioId);
            EstadoPedido estadoActual = EstadoPedido.deClave(actual.clave()).orElse(EstadoPedido.PENDIENTE);

            if (estadoActual == EstadoPedido.EN_RUTA) {
                throw new NegocioException("Esta entrega ya está en ruta.");
            }
            if (!estadoActual.estaPorEntregar()) {
                throw new NegocioException("Este pedido ya está cerrado.");
            }
            if (pedidos.entregaEnCurso(domiciliarioId).isPresent()) {
                throw new NegocioException("Ya llevas una entrega en ruta. Termínala antes de comenzar otra.");
            }

            transiciones.aplicar(pedidoId, EstadoPedido.EN_RUTA, actual);
            return "Entrega en ruta. ¡Buen viaje!";
        });
    }

    // ---------- Terminar ----------

    @Override
    public String terminar(int pedidoId, int domiciliarioId, Resultado resultado, String nota, FotoEntrega foto) {
        if (resultado == Resultado.ENTREGADO && foto == null) {
            throw new NegocioException("Toma la foto de la entrega para poder terminarla.");
        }
        if (resultado == Resultado.NO_ENTREGADO && (nota == null || nota.isBlank())) {
            throw new NegocioException("Cuenta por qué no se pudo entregar.");
        }

        // El archivo se guarda antes de abrir la transacción; si algo falla después, se borra.
        String archivo = foto == null ? null : fotos.guardar(foto);
        Cierre cierre;
        try {
            cierre = transaccion.execute(estado -> cerrar(pedidoId, domiciliarioId, resultado, nota, foto, archivo));
        } catch (RuntimeException e) {
            if (archivo != null) {
                fotos.borrar(archivo);
            }
            throw e;
        }

        PedidoCabecera p = cierre.pedido();
        avisos.avisarAAdmins(
                resultado == Resultado.ENTREGADO ? "Pedido entregado" : "Pedido no entregado",
                p.numero() + " · " + p.cliente() + (p.domiciliario() == null ? "" : " · " + p.domiciliario()),
                "/Pedidos/Detalle/" + pedidoId);
        return cierre.mensaje();
    }

    private Cierre cerrar(int pedidoId, int domiciliarioId, Resultado resultado, String nota, FotoEntrega foto,
                          String archivo) {
        EstadoActual actual = propio(pedidoId, domiciliarioId);
        if (!EstadoPedido.EN_RUTA.getClave().equals(actual.clave())) {
            throw new NegocioException("Primero comienza la entrega antes de terminarla.");
        }

        EstadoPedido nuevo = resultado == Resultado.ENTREGADO ? EstadoPedido.ENTREGADO : EstadoPedido.NO_ENTREGADO;
        transiciones.aplicar(pedidoId, nuevo, actual);

        if (nota != null && !nota.isBlank()) {
            pedidos.guardarNotaDeEntrega(pedidoId, nota.trim());
        }
        if (foto != null) {
            pedidos.insertarFoto(pedidoId, archivo, foto.tipoContenido(), foto.contenido().length, domiciliarioId);
        }

        PedidoCabecera pedido = pedidos.cabeceraDeEntrega(pedidoId, domiciliarioId).orElseThrow();
        eventos.registrar(TipoEvento.PEDIDO, actual.sucursalId(), "Entrega del pedido " + pedido.numero()
                + (resultado == Resultado.ENTREGADO ? " terminada con foto" : " no realizada: " + nota.trim()));
        return new Cierre(resultado == Resultado.ENTREGADO
                ? "¡Entrega terminada! La foto quedó guardada."
                : "Quedó registrado que no se pudo entregar. El stock volvió a la sucursal.", pedido);
    }

    /** El pedido debe existir y estar asignado a este domiciliario. */
    private EstadoActual propio(int pedidoId, int domiciliarioId) {
        EstadoActual actual = pedidos.estadoBloqueado(pedidoId)
                .orElseThrow(() -> new NegocioException("El pedido no existe."));
        if (!Optional.ofNullable(actual.domiciliarioId()).map(id -> id == domiciliarioId).orElse(false)) {
            throw new NegocioException("Este pedido no está asignado a ti.");
        }
        return actual;
    }
}
