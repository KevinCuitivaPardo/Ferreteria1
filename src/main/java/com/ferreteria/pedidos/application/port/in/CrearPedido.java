package com.ferreteria.pedidos.application.port.in;

import com.ferreteria.pedidos.domain.DatosCliente;
import com.ferreteria.pedidos.domain.LineaPedido;
import com.ferreteria.shared.MedioPago;

import java.math.BigDecimal;
import java.util.List;

public interface CrearPedido {

    int MAX_LINEAS = 10;
    int MAX_CANTIDAD_POR_LINEA = 100_000;

    /**
     * Un domicilio: la dirección de entrega es la del {@code cliente}. Sale de la sucursal indicada,
     * de cuyo inventario se descuenta.
     *
     * @param montoRecibido con cuánto pagará el cliente (opcional, solo efectivo), para llevar el cambio
     */
    record Comando(int sucursalId, String nombreVendedor, DatosCliente cliente, BigDecimal descuentoElegido,
                   MedioPago medioPago, BigDecimal montoRecibido, List<LineaPedido> lineas) {
    }

    /**
     * Valida, ubica la dirección en el mapa, descuenta el stock y guarda el pedido.
     *
     * @return el id del pedido nuevo
     * @throws com.ferreteria.shared.NegocioException con todos los problemas encontrados
     */
    int crear(Comando comando);
}
