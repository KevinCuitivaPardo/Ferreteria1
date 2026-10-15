package com.ferreteria.ventas.application.port.in;

import com.ferreteria.shared.MedioPago;
import com.ferreteria.ventas.domain.LineaVenta;

import java.math.BigDecimal;
import java.util.List;

public interface RegistrarVenta {

    int MAX_LINEAS = 10;
    int MAX_CANTIDAD_POR_LINEA = 100_000;

    /**
     * Una venta en el local: quién compró, qué se llevó, cuántas unidades y cómo pagó.
     * La fecha la pone el sistema al guardar.
     *
     * @param montoRecibido con cuánto pagó el cliente (opcional, solo para pagos en efectivo)
     */
    record Comando(int sucursalId, String nombreVendedor, String clienteNombre, String clienteTelefono,
                   BigDecimal descuentoElegido, MedioPago medioPago, BigDecimal montoRecibido,
                   List<LineaVenta> lineas) {
    }

    /**
     * Valida, descuenta el stock de la sucursal y guarda la venta.
     *
     * @return el id de la venta nueva
     * @throws com.ferreteria.shared.NegocioException con todos los problemas encontrados
     */
    int registrar(Comando comando);
}
