package com.ferreteria.usuarios.application.port.in;

import com.ferreteria.usuarios.domain.Rol;

import java.math.BigDecimal;
import java.util.List;

public interface GestionarUsuarios {

    /** El administrador no pertenece a una sucursal; el vendedor y el domiciliario sí. */
    record NuevoUsuario(String nombreUsuario, String clave, Rol rol, Integer sucursalId) {
    }

    record DescuentoVendedor(int usuarioId, String nombreUsuario, BigDecimal porcentaje) {
    }

    void crear(NuevoUsuario nuevo);

    void alternarActivo(int usuarioId);

    /** Guarda el descuento de cada vendedor. Valida todo antes de guardar nada. */
    void guardarDescuentos(List<DescuentoVendedor> descuentos);

    /**
     * Elimina el descuento de un vendedor (queda en 0 %): desde ese momento solo puede vender sin descuento.
     * Los pedidos y ventas que ya hizo con descuento no cambian.
     */
    void quitarDescuento(int vendedorId);

    /** Crea el administrador inicial solo si todavía no hay ningún usuario. */
    void crearAdminInicialSiHaceFalta(String nombreUsuario, String clave);
}
