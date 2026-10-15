package com.ferreteria.pedidos.application;

import com.ferreteria.pedidos.application.port.in.AsignarDomiciliario;
import com.ferreteria.pedidos.application.port.in.CambiarEstadoPedido;
import com.ferreteria.pedidos.application.port.in.CorregirUbicacion;
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
import com.ferreteria.pedidos.domain.EstadoPedido;
import com.ferreteria.pedidos.domain.LineaPedido;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.pedidos.domain.PedidoGuardado;
import com.ferreteria.pedidos.domain.ProductoVendible;
import com.ferreteria.pedidos.domain.Ubicacion;
import com.ferreteria.pedidos.domain.Vendedor;
import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.Pagos;
import com.ferreteria.shared.Totales;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso que modifican domicilios: crear, cambiar de estado (administrador), asignar a un
 * domiciliario y corregir la ubicación. Los avisos en tiempo real salen después de guardar.
 */
@Service
public class PedidoService implements CrearPedido, CambiarEstadoPedido, AsignarDomiciliario, CorregirUbicacion {

    private record LineaValida(int productoId, int cantidad) {
    }

    private record Detalle(int productoId, int cantidad, BigDecimal precio) {
    }

    /** Lo que hay que avisar una vez que la asignación ya quedó guardada. */
    private record Asignacion(String mensaje, Integer anterior, Integer nuevo, PedidoCabecera pedido) {
    }

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final CatalogoProductos catalogo;
    private final Vendedores vendedores;
    private final Domiciliarios domiciliarios;
    private final UbicadorDeDirecciones ubicador;
    private final RegistroEventos eventos;
    private final AvisosDeEntrega avisos;
    private final TransicionesDeEstado transiciones;
    private final TransactionTemplate transaccion;

    public PedidoService(PedidoRepository pedidos, ClienteRepository clientes, CatalogoProductos catalogo,
                         Vendedores vendedores, Domiciliarios domiciliarios, UbicadorDeDirecciones ubicador,
                         RegistroEventos eventos, AvisosDeEntrega avisos, TransicionesDeEstado transiciones,
                         TransactionTemplate transaccion) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.catalogo = catalogo;
        this.vendedores = vendedores;
        this.domiciliarios = domiciliarios;
        this.ubicador = ubicador;
        this.eventos = eventos;
        this.avisos = avisos;
        this.transiciones = transiciones;
        this.transaccion = transaccion;
    }

    // ---------- CrearPedido ----------

    /**
     * La dirección se ubica en el mapa antes de abrir la transacción: así una consulta lenta al servicio
     * de mapas no mantiene filas de stock bloqueadas. Si no se encuentra, el pedido se guarda igual.
     */
    @Override
    public int crear(Comando comando) {
        List<String> errores = new ArrayList<>();
        validarCliente(comando.cliente(), errores);
        List<LineaValida> validas = validarLineas(comando.lineas(), errores);
        if (errores.isEmpty() && validas.isEmpty()) {
            errores.add("Agrega al menos un producto con cantidad.");
        }
        if (!errores.isEmpty()) {
            throw new NegocioException(errores);
        }

        Optional<Ubicacion> ubicacion = ubicador.ubicar(comando.cliente().direccion().trim());
        PedidoGuardado guardado = transaccion.execute(estado -> guardar(comando, validas, ubicacion.orElse(null)));

        avisos.avisarAAdmins("Nuevo pedido por asignar", guardado.numero() + " · "
                + comando.cliente().nombreCliente().trim() + " · " + comando.cliente().direccion().trim(),
                "/Pedidos/Detalle/" + guardado.id());
        return guardado.id();
    }

    private PedidoGuardado guardar(Comando comando, List<LineaValida> validas, Ubicacion ubicacion) {
        BigDecimal descuento = comando.descuentoElegido() == null ? BigDecimal.ZERO : comando.descuentoElegido();

        // El máximo sale de la base de datos, nunca del formulario.
        Vendedor vendedor = vendedores.porNombre(comando.nombreVendedor())
                .filter(Vendedor::activo)
                .orElseThrow(() -> new NegocioException(
                        "Tu usuario no está activo. Consulta con el administrador."));

        if (descuento.signum() < 0 || descuento.compareTo(vendedor.descuentoMaximo()) > 0) {
            throw new NegocioException("El descuento elegido supera tu máximo permitido ("
                    + vendedor.descuentoMaximo().stripTrailingZeros().toPlainString() + "%).");
        }

        int clienteId = obtenerOCrearCliente(comando.cliente());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<Detalle> detalles = new ArrayList<>();

        for (LineaValida linea : validas) {
            ProductoVendible producto = catalogo.bloquearParaVenta(linea.productoId(), comando.sucursalId())
                    .orElseThrow(() -> new NegocioException("Uno de los productos ya no existe."));

            if (producto.stock() < linea.cantidad()) {
                throw new NegocioException("No hay suficiente stock de " + producto.nombre()
                        + ". Disponible: " + producto.stock() + ".");
            }

            catalogo.descontar(producto.id(), comando.sucursalId(), linea.cantidad());
            subtotal = subtotal.add(producto.precioVenta().multiply(BigDecimal.valueOf(linea.cantidad())));
            detalles.add(new Detalle(producto.id(), linea.cantidad(), producto.precioVenta()));
        }

        String direccion = comando.cliente().direccion().trim();
        BigDecimal total = Totales.totalConDescuento(subtotal, descuento);
        MedioPago medioPago = comando.medioPago() == null ? MedioPago.EFECTIVO : comando.medioPago();
        BigDecimal montoRecibido = Pagos.montoRecibido(medioPago, total, comando.montoRecibido());

        PedidoGuardado guardado = pedidos.insertar(clienteId, vendedor.id(), comando.sucursalId(), direccion,
                ubicacion, descuento, total, medioPago, montoRecibido);
        for (Detalle d : detalles) {
            pedidos.insertarItem(guardado.id(), d.productoId(), d.cantidad(), d.precio());
        }

        eventos.registrar(TipoEvento.PEDIDO, comando.sucursalId(), "Pedido " + guardado.numero() + " para "
                + comando.cliente().nombreCliente().trim() + " en " + direccion + " por $"
                + total.toPlainString() + " · pago en " + medioPago.getEtiqueta().toLowerCase()
                + (ubicacion == null ? " · dirección sin ubicar en el mapa" : ""));
        if (descuento.signum() > 0) {
            eventos.registrar(TipoEvento.DESCUENTO, comando.sucursalId(), "Descuento de "
                    + descuento.stripTrailingZeros().toPlainString() + "% (−$"
                    + Totales.montoDescuento(subtotal, descuento).setScale(0, RoundingMode.HALF_UP).toPlainString()
                    + ") en el pedido " + guardado.numero());
        }
        return guardado;
    }

    // ---------- CambiarEstadoPedido (administrador) ----------

    @Override
    @Transactional
    public String cambiarEstado(int pedidoId, String claveEstado) {
        EstadoPedido nuevo = EstadoPedido.deClave(claveEstado)
                .orElseThrow(() -> new NegocioException("El estado elegido no es válido."));
        EstadoActual actual = pedidos.estadoBloqueado(pedidoId)
                .orElseThrow(() -> new NegocioException("El pedido no existe."));
        return transiciones.aplicar(pedidoId, nuevo, actual);
    }

    // ---------- AsignarDomiciliario ----------

    @Override
    public String asignar(int pedidoId, int domiciliarioId) {
        Asignacion resultado = transaccion.execute(estado -> guardarAsignacion(pedidoId, domiciliarioId));

        // Los avisos salen cuando la asignación ya está guardada, para que la cola del domiciliario ya la tenga
        if (resultado.anterior() != null && !resultado.anterior().equals(resultado.nuevo())) {
            avisos.pedidoRetirado(resultado.anterior(), pedidoId, resultado.pedido().numero());
        }
        if (resultado.nuevo() != null && !resultado.nuevo().equals(resultado.anterior())) {
            PedidoCabecera p = resultado.pedido();
            avisos.pedidoAsignado(resultado.nuevo(), pedidoId, p.numero(), p.cliente(), p.direccion());
        }
        return resultado.mensaje();
    }

    private Asignacion guardarAsignacion(int pedidoId, int domiciliarioId) {
        EstadoActual actual = pedidos.estadoBloqueado(pedidoId)
                .orElseThrow(() -> new NegocioException("El pedido no existe."));
        PedidoCabecera pedido = pedidos.cabecera(pedidoId, true, 0)
                .orElseThrow(() -> new NegocioException("El pedido no existe."));

        EstadoPedido estado = EstadoPedido.deClave(actual.clave()).orElse(EstadoPedido.PENDIENTE);
        if (estado == EstadoPedido.EN_RUTA) {
            throw new NegocioException("El pedido ya va en ruta: no se puede cambiar su domiciliario.");
        }
        if (!estado.estaPorEntregar()) {
            throw new NegocioException("El pedido ya está cerrado: no se puede asignar.");
        }

        Integer anterior = actual.domiciliarioId();

        if (domiciliarioId == 0) {
            pedidos.asignarDomiciliario(pedidoId, null);
            eventos.registrar(TipoEvento.ASIGNACION, actual.sucursalId(),
                    "Pedido " + pedido.numero() + ": se quitó el domiciliario");
            return new Asignacion("Se quitó la asignación del domiciliario.", anterior, null, pedido);
        }

        Domiciliario domiciliario = domiciliarios.porId(domiciliarioId)
                .filter(Domiciliario::activo)
                .orElseThrow(() -> new NegocioException("El domiciliario elegido no existe o está desactivado."));
        if (domiciliario.sucursalId() == null || domiciliario.sucursalId() != actual.sucursalId()) {
            throw new NegocioException("El domiciliario pertenece a otra sucursal.");
        }
        if (anterior != null && anterior == domiciliarioId) {
            return new Asignacion("El pedido ya estaba asignado a " + domiciliario.nombre() + ".",
                    anterior, domiciliarioId, pedido);
        }

        pedidos.asignarDomiciliario(pedidoId, domiciliarioId);
        eventos.registrar(TipoEvento.ASIGNACION, actual.sucursalId(),
                "Pedido " + pedido.numero() + " asignado a " + domiciliario.nombre());
        return new Asignacion("Pedido asignado a " + domiciliario.nombre() + ". Le llegó un aviso.",
                anterior, domiciliarioId, pedido);
    }

    // ---------- CorregirUbicacion ----------

    @Override
    @Transactional
    public String corregir(int pedidoId, double latitud, double longitud) {
        boolean valida = Math.abs(latitud) <= 90 && Math.abs(longitud) <= 180
                && !Double.isNaN(latitud) && !Double.isNaN(longitud);
        if (!valida) {
            throw new NegocioException("El punto marcado en el mapa no es válido.");
        }

        EstadoActual actual = pedidos.estadoBloqueado(pedidoId)
                .orElseThrow(() -> new NegocioException("El pedido no existe."));
        pedidos.actualizarUbicacion(pedidoId, new Ubicacion(latitud, longitud));
        eventos.registrar(TipoEvento.PEDIDO, actual.sucursalId(),
                "Ubicación de entrega del pedido #" + pedidoId + " ajustada a mano");
        return "Ubicación de entrega actualizada.";
    }

    // ---------- Validaciones y ayudas ----------

    private void validarCliente(DatosCliente c, List<String> errores) {
        if (esVacio(c.celular1())) {
            errores.add("Escribe el celular del cliente.");
        }
        if (esVacio(c.nombreCliente())) {
            errores.add("Escribe el nombre del cliente.");
        }
        if (esVacio(c.direccion())) {
            errores.add("Escribe la dirección de entrega.");
        }
    }

    private List<LineaValida> validarLineas(List<LineaPedido> lineas, List<String> errores) {
        List<LineaValida> validas = new ArrayList<>();

        for (int i = 0; i < lineas.size(); i++) {
            LineaPedido l = lineas.get(i);
            int cantidad = l.cantidad() == null ? 0 : l.cantidad();

            if (l.productoId() == 0 && cantidad == 0) {
                continue; // fila vacía
            }
            if (l.productoId() == 0) {
                errores.add("La línea " + (i + 1) + " tiene cantidad, pero no tiene producto.");
                continue;
            }
            if (cantidad < 1 || cantidad > MAX_CANTIDAD_POR_LINEA) {
                errores.add("La línea " + (i + 1) + " debe tener una cantidad entre 1 y 100.000.");
                continue;
            }
            validas.add(new LineaValida(l.productoId(), cantidad));
        }
        return validas;
    }

    /** Si el cliente ya existe se usa su registro sin sobrescribir sus datos. */
    private int obtenerOCrearCliente(DatosCliente c) {
        String celular = c.celular1().trim();

        return clientes.idPorCelular(celular).orElseGet(() -> clientes.crear(new DatosCliente(
                celular,
                nulo(c.celular2()),
                nulo(c.nombreFerreteria()),
                c.nombreCliente().trim(),
                c.direccion().trim(),
                nulo(c.ubicacion()),
                nulo(c.horario()))));
    }

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String nulo(String texto) {
        return esVacio(texto) ? null : texto.trim();
    }
}
