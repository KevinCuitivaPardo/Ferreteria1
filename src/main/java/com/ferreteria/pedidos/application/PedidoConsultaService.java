package com.ferreteria.pedidos.application;

import com.ferreteria.pedidos.application.port.in.ConsultarPedidos;
import com.ferreteria.pedidos.application.port.out.AlmacenFotos;
import com.ferreteria.pedidos.application.port.out.CatalogoProductos;
import com.ferreteria.pedidos.application.port.out.ClienteRepository;
import com.ferreteria.pedidos.application.port.out.Domiciliarios;
import com.ferreteria.pedidos.application.port.out.PedidoRepository;
import com.ferreteria.pedidos.application.port.out.Vendedores;
import com.ferreteria.pedidos.domain.Cliente;
import com.ferreteria.pedidos.domain.ContenidoFoto;
import com.ferreteria.pedidos.domain.Domiciliario;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.pedidos.domain.PedidoDetalle;
import com.ferreteria.pedidos.domain.PedidoMapa;
import com.ferreteria.pedidos.domain.PedidoResumen;
import com.ferreteria.pedidos.domain.ProductoVendible;
import com.ferreteria.pedidos.domain.Vendedor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoConsultaService implements ConsultarPedidos {

    private final PedidoRepository pedidos;
    private final ClienteRepository clientes;
    private final CatalogoProductos catalogo;
    private final Vendedores vendedores;
    private final Domiciliarios domiciliarios;
    private final AlmacenFotos almacenFotos;

    public PedidoConsultaService(PedidoRepository pedidos, ClienteRepository clientes,
                                 CatalogoProductos catalogo, Vendedores vendedores,
                                 Domiciliarios domiciliarios, AlmacenFotos almacenFotos) {
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.catalogo = catalogo;
        this.vendedores = vendedores;
        this.domiciliarios = domiciliarios;
        this.almacenFotos = almacenFotos;
    }

    @Override
    public List<PedidoResumen> recientes(String nombreUsuario, boolean esAdmin) {
        if (esAdmin) {
            return pedidos.recientes(null);
        }
        return vendedores.porNombre(nombreUsuario)
                .map(v -> pedidos.recientes(v.id()))
                .orElse(List.of());
    }

    @Override
    public List<PedidoResumen> sinAsignar() {
        return pedidos.sinAsignar();
    }

    @Override
    public Optional<PedidoDetalle> detalle(int pedidoId, String nombreUsuario, boolean esAdmin) {
        int vendedorId = 0;
        if (!esAdmin) {
            Optional<Vendedor> vendedor = vendedores.porNombre(nombreUsuario);
            if (vendedor.isEmpty()) {
                return Optional.empty();
            }
            vendedorId = vendedor.get().id();
        }

        return pedidos.cabecera(pedidoId, esAdmin, vendedorId).map(this::conItemsYFotos);
    }

    @Override
    public List<PedidoMapa> paraElMapa() {
        return pedidos.ubicados();
    }

    @Override
    public List<ProductoVendible> productosDisponibles(int sucursalId) {
        return catalogo.conExistencias(sucursalId);
    }

    @Override
    public BigDecimal descuentoMaximo(String nombreUsuario) {
        return vendedores.porNombre(nombreUsuario).map(Vendedor::descuentoMaximo).orElse(BigDecimal.ZERO);
    }

    @Override
    public Optional<Cliente> buscarCliente(String celular) {
        String buscado = celular == null ? "" : celular.trim();
        if (buscado.isEmpty()) {
            return Optional.empty();
        }
        return clientes.porCelular(buscado);
    }

    @Override
    public List<Domiciliario> domiciliariosDe(int sucursalId) {
        return domiciliarios.activosDe(sucursalId);
    }

    @Override
    public List<PedidoResumen> entregasDe(int domiciliarioId) {
        return pedidos.entregasDe(domiciliarioId);
    }

    @Override
    public Optional<PedidoDetalle> entrega(int pedidoId, int domiciliarioId) {
        return pedidos.cabeceraDeEntrega(pedidoId, domiciliarioId).map(this::conItemsYFotos);
    }

    @Override
    public Optional<ContenidoFoto> foto(int pedidoId, int fotoId, int usuarioId, boolean esAdmin) {
        // Solo el admin, el vendedor dueño del pedido o el domiciliario asignado pueden ver la foto
        boolean autorizado = esAdmin
                || pedidos.cabecera(pedidoId, false, usuarioId).isPresent()
                || pedidos.cabeceraDeEntrega(pedidoId, usuarioId).isPresent();
        if (!autorizado) {
            return Optional.empty();
        }

        return pedidos.foto(pedidoId, fotoId).flatMap(foto -> almacenFotos.leer(foto.archivo())
                .map(bytes -> new ContenidoFoto(bytes, foto.tipoContenido())));
    }

    private PedidoDetalle conItemsYFotos(PedidoCabecera cabecera) {
        return new PedidoDetalle(cabecera, pedidos.items(cabecera.id()), pedidos.fotos(cabecera.id()));
    }
}
