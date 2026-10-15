package com.ferreteria.ventas.application;

import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.Pagos;
import com.ferreteria.shared.Totales;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import com.ferreteria.ventas.application.port.in.RegistrarVenta;
import com.ferreteria.ventas.application.port.out.CatalogoProductos;
import com.ferreteria.ventas.application.port.out.Vendedores;
import com.ferreteria.ventas.application.port.out.VentaRepository;
import com.ferreteria.ventas.domain.LineaVenta;
import com.ferreteria.ventas.domain.ProductoEnVenta;
import com.ferreteria.ventas.domain.Vendedor;
import com.ferreteria.ventas.domain.VentaGuardada;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Venta en el local: se paga y se lleva en el momento, por eso descuenta el stock de la sucursal al guardar. */
@Service
public class VentaService implements RegistrarVenta {

    private record LineaValida(int productoId, int cantidad) {
    }

    private record Detalle(int productoId, int cantidad, BigDecimal precio) {
    }

    private final VentaRepository ventas;
    private final CatalogoProductos catalogo;
    private final Vendedores vendedores;
    private final RegistroEventos eventos;

    public VentaService(VentaRepository ventas, CatalogoProductos catalogo, Vendedores vendedores,
                        RegistroEventos eventos) {
        this.ventas = ventas;
        this.catalogo = catalogo;
        this.vendedores = vendedores;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public int registrar(Comando comando) {
        BigDecimal descuento = comando.descuentoElegido() == null ? BigDecimal.ZERO : comando.descuentoElegido();

        List<String> errores = new ArrayList<>();
        if (esVacio(comando.clienteNombre())) {
            errores.add("Escribe el nombre de la persona que compra.");
        }
        if (esVacio(comando.clienteTelefono())) {
            errores.add("Escribe el número de contacto de quien compra.");
        }
        List<LineaValida> validas = validarLineas(comando.lineas(), errores);
        if (errores.isEmpty() && validas.isEmpty()) {
            errores.add("Agrega al menos un producto con cantidad.");
        }
        if (!errores.isEmpty()) {
            throw new NegocioException(errores);
        }

        // El máximo sale de la base de datos, nunca del formulario.
        Vendedor vendedor = vendedores.porNombre(comando.nombreVendedor())
                .filter(Vendedor::activo)
                .orElseThrow(() -> new NegocioException(
                        "Tu usuario no está activo. Consulta con el administrador."));

        if (descuento.signum() < 0 || descuento.compareTo(vendedor.descuentoMaximo()) > 0) {
            throw new NegocioException("El descuento elegido supera tu máximo permitido ("
                    + vendedor.descuentoMaximo().stripTrailingZeros().toPlainString() + "%).");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<Detalle> detalles = new ArrayList<>();

        for (LineaValida linea : validas) {
            ProductoEnVenta producto = catalogo.bloquearParaVenta(linea.productoId(), comando.sucursalId())
                    .orElseThrow(() -> new NegocioException("Uno de los productos ya no existe."));

            if (producto.stock() < linea.cantidad()) {
                throw new NegocioException("No hay suficiente stock de " + producto.nombre()
                        + ". Disponible: " + producto.stock() + ".");
            }

            catalogo.descontar(producto.id(), comando.sucursalId(), linea.cantidad());
            subtotal = subtotal.add(producto.precioVenta().multiply(BigDecimal.valueOf(linea.cantidad())));
            detalles.add(new Detalle(producto.id(), linea.cantidad(), producto.precioVenta()));
        }

        BigDecimal total = Totales.totalConDescuento(subtotal, descuento);
        MedioPago medioPago = comando.medioPago() == null ? MedioPago.EFECTIVO : comando.medioPago();
        BigDecimal montoRecibido = Pagos.montoRecibido(medioPago, total, comando.montoRecibido());

        VentaGuardada guardada = ventas.insertar(comando.sucursalId(), vendedor.id(),
                comando.clienteNombre().trim(), comando.clienteTelefono().trim(), descuento, total,
                medioPago, montoRecibido);
        for (Detalle d : detalles) {
            ventas.insertarItem(guardada.id(), d.productoId(), d.cantidad(), d.precio());
        }

        eventos.registrar(TipoEvento.VENTA, comando.sucursalId(), "Venta " + guardada.numero() + " a "
                + comando.clienteNombre().trim() + " (" + comando.clienteTelefono().trim() + ") por $"
                + total.toPlainString() + " · " + validas.stream().mapToInt(LineaValida::cantidad).sum()
                + " unidades · pago en " + medioPago.getEtiqueta().toLowerCase());
        if (descuento.signum() > 0) {
            eventos.registrar(TipoEvento.DESCUENTO, comando.sucursalId(), "Descuento de "
                    + descuento.stripTrailingZeros().toPlainString() + "% (−$"
                    + Totales.montoDescuento(subtotal, descuento).setScale(0, java.math.RoundingMode.HALF_UP)
                            .toPlainString() + ") en la venta " + guardada.numero());
        }
        return guardada.id();
    }

    private List<LineaValida> validarLineas(List<LineaVenta> lineas, List<String> errores) {
        List<LineaValida> validas = new ArrayList<>();

        for (int i = 0; i < lineas.size(); i++) {
            LineaVenta l = lineas.get(i);
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

    private static boolean esVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
