package com.ferreteria.inventario.infrastructure.out.persistence;

import com.ferreteria.inventario.application.port.out.MovimientoRepository;
import com.ferreteria.inventario.domain.Movimiento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class JdbcMovimientoRepository implements MovimientoRepository {

    private final JdbcTemplate jdbc;

    JdbcMovimientoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insertar(int productoId, int sucursalId, int cantidad, String motivo) {
        jdbc.update("""
                INSERT INTO movimientos_inventario (producto_id, sucursal_id, cantidad, motivo)
                VALUES (?, ?, ?, ?)""", productoId, sucursalId, cantidad, motivo);
    }

    @Override
    public List<Movimiento> historial(int productoId, int sucursalId, int limite) {
        return jdbc.query("""
                SELECT cantidad, motivo, fecha
                FROM movimientos_inventario
                WHERE producto_id = ? AND sucursal_id = ?
                ORDER BY fecha DESC
                LIMIT ?""", (rs, n) -> new Movimiento(
                rs.getInt("cantidad"),
                rs.getString("motivo"),
                rs.getTimestamp("fecha").toLocalDateTime()), productoId, sucursalId, limite);
    }
}
