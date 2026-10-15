package com.ferreteria.ventas.infrastructure.out.persistence;

import com.ferreteria.shared.MedioPago;
import com.ferreteria.ventas.application.port.out.VentaRepository;
import com.ferreteria.ventas.domain.VentaCabecera;
import com.ferreteria.ventas.domain.VentaGuardada;
import com.ferreteria.ventas.domain.VentaItem;
import com.ferreteria.ventas.domain.VentaResumen;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
class JdbcVentaRepository implements VentaRepository {

    private final JdbcTemplate jdbc;

    JdbcVentaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<VentaResumen> recientes(Integer vendedorId) {
        String filtro = vendedorId == null ? "" : "AND v.vendedor_id = ?";
        String sql = """
                SELECT v.id, v.numero, v.fecha, v.cliente_nombre, v.cliente_telefono,
                       u.nombre_usuario, s.nombre AS sucursal, v.total, v.medio_pago
                FROM ventas v
                JOIN usuarios u ON u.id = v.vendedor_id
                JOIN sucursales s ON s.id = v.sucursal_id
                WHERE v.fecha >= now() - interval '3 months'
                %s
                ORDER BY v.fecha DESC, v.id DESC""".formatted(filtro);

        RowMapper<VentaResumen> mapper = (rs, n) -> new VentaResumen(
                rs.getInt("id"),
                rs.getString("numero"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getString("cliente_nombre"),
                rs.getString("cliente_telefono"),
                rs.getString("nombre_usuario"),
                rs.getString("sucursal"),
                rs.getBigDecimal("total"),
                MedioPago.deClave(rs.getString("medio_pago")));

        return vendedorId == null ? jdbc.query(sql, mapper) : jdbc.query(sql, mapper, vendedorId);
    }

    /** La condición del vendedor va dentro de la consulta: nunca puede traer una venta ajena. */
    @Override
    public Optional<VentaCabecera> cabecera(int ventaId, boolean esAdmin, int vendedorId) {
        return jdbc.query("""
                SELECT v.id, v.numero, v.fecha, v.cliente_nombre, v.cliente_telefono,
                       u.nombre_usuario, s.nombre AS sucursal, v.descuento_pct, v.total,
                       v.medio_pago, v.monto_recibido
                FROM ventas v
                JOIN usuarios u ON u.id = v.vendedor_id
                JOIN sucursales s ON s.id = v.sucursal_id
                WHERE v.id = ?
                  AND (? OR v.vendedor_id = ?)""", (rs, n) -> new VentaCabecera(
                rs.getInt("id"),
                rs.getString("numero"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getString("cliente_nombre"),
                rs.getString("cliente_telefono"),
                rs.getString("nombre_usuario"),
                rs.getString("sucursal"),
                rs.getBigDecimal("descuento_pct"),
                rs.getBigDecimal("total"),
                MedioPago.deClave(rs.getString("medio_pago")),
                rs.getBigDecimal("monto_recibido")), ventaId, esAdmin, vendedorId).stream().findFirst();
    }

    @Override
    public List<VentaItem> items(int ventaId) {
        return jdbc.query("""
                SELECT pr.codigo, pr.nombre, pr.medida, vi.cantidad, vi.precio_unitario
                FROM venta_items vi
                JOIN productos pr ON pr.id = vi.producto_id
                WHERE vi.venta_id = ?
                ORDER BY vi.id""", (rs, n) -> new VentaItem(
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("medida"),
                rs.getInt("cantidad"),
                rs.getBigDecimal("precio_unitario")), ventaId);
    }

    @Override
    public VentaGuardada insertar(int sucursalId, int vendedorId, String clienteNombre, String clienteTelefono,
                                  BigDecimal descuentoPct, BigDecimal total, MedioPago medioPago,
                                  BigDecimal montoRecibido) {
        return jdbc.queryForObject("""
                INSERT INTO ventas (sucursal_id, vendedor_id, cliente_nombre, cliente_telefono, descuento_pct,
                                    total, medio_pago, monto_recibido)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, numero""", (rs, n) -> new VentaGuardada(rs.getInt("id"), rs.getString("numero")),
                sucursalId, vendedorId, clienteNombre, clienteTelefono, descuentoPct, total,
                medioPago.getClave(), montoRecibido);
    }

    @Override
    public void insertarItem(int ventaId, int productoId, int cantidad, BigDecimal precioUnitario) {
        jdbc.update("""
                INSERT INTO venta_items (venta_id, producto_id, cantidad, precio_unitario)
                VALUES (?, ?, ?, ?)""", ventaId, productoId, cantidad, precioUnitario);
    }
}
