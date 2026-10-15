package com.ferreteria.ventas.application;

import com.ferreteria.ventas.application.port.in.ConsultarVentas;
import com.ferreteria.ventas.application.port.out.CatalogoProductos;
import com.ferreteria.ventas.application.port.out.VentaRepository;
import com.ferreteria.ventas.application.port.out.Vendedores;
import com.ferreteria.ventas.domain.ProductoEnVenta;
import com.ferreteria.ventas.domain.Vendedor;
import com.ferreteria.ventas.domain.VentaDetalle;
import com.ferreteria.ventas.domain.VentaResumen;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class VentaConsultaService implements ConsultarVentas {

    private final VentaRepository ventas;
    private final CatalogoProductos catalogo;
    private final Vendedores vendedores;

    public VentaConsultaService(VentaRepository ventas, CatalogoProductos catalogo, Vendedores vendedores) {
        this.ventas = ventas;
        this.catalogo = catalogo;
        this.vendedores = vendedores;
    }

    @Override
    public List<VentaResumen> recientes(String nombreUsuario, boolean esAdmin) {
        if (esAdmin) {
            return ventas.recientes(null);
        }
        return vendedores.porNombre(nombreUsuario)
                .map(v -> ventas.recientes(v.id()))
                .orElse(List.of());
    }

    @Override
    public Optional<VentaDetalle> detalle(int ventaId, String nombreUsuario, boolean esAdmin) {
        int vendedorId = 0;
        if (!esAdmin) {
            Optional<Vendedor> vendedor = vendedores.porNombre(nombreUsuario);
            if (vendedor.isEmpty()) {
                return Optional.empty();
            }
            vendedorId = vendedor.get().id();
        }

        return ventas.cabecera(ventaId, esAdmin, vendedorId)
                .map(cabecera -> new VentaDetalle(cabecera, ventas.items(ventaId)));
    }

    @Override
    public List<ProductoEnVenta> productosDisponibles(int sucursalId) {
        return catalogo.conExistencias(sucursalId);
    }

    @Override
    public BigDecimal descuentoMaximo(String nombreUsuario) {
        return vendedores.porNombre(nombreUsuario).map(Vendedor::descuentoMaximo).orElse(BigDecimal.ZERO);
    }
}
