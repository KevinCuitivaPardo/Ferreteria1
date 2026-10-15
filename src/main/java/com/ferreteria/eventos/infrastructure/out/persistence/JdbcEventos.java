package com.ferreteria.eventos.infrastructure.out.persistence;

import com.ferreteria.eventos.application.port.out.EventoRepository;
import com.ferreteria.eventos.domain.Evento;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Guarda y lee el registro de eventos en la base de datos. Al registrar toma como autor a quien
 * tiene la sesión iniciada; escribe en la misma transacción de la operación que lo origina.
 */
@Repository
class JdbcEventos implements RegistroEventos, EventoRepository {

    private final JdbcTemplate jdbc;

    JdbcEventos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(TipoEvento tipo, Integer sucursalId, String descripcion) {
        jdbc.update("INSERT INTO eventos (tipo, actor, sucursal_id, descripcion) VALUES (?, ?, ?, ?)",
                tipo.name(), autorActual(), sucursalId, descripcion);
    }

    @Override
    public List<Evento> recientes(TipoEvento tipo, Integer sucursalId, int limite) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.fecha, e.tipo, e.actor, s.nombre AS sucursal, e.descripcion
                FROM eventos e
                LEFT JOIN sucursales s ON s.id = e.sucursal_id
                WHERE 1 = 1""");
        List<Object> parametros = new ArrayList<>();

        if (tipo != null) {
            sql.append(" AND e.tipo = ?");
            parametros.add(tipo.name());
        }
        if (sucursalId != null) {
            sql.append(" AND e.sucursal_id = ?");
            parametros.add(sucursalId);
        }
        sql.append(" ORDER BY e.fecha DESC, e.id DESC LIMIT ?");
        parametros.add(limite);

        return jdbc.query(sql.toString(), (rs, n) -> new Evento(
                rs.getLong("id"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                TipoEvento.valueOf(rs.getString("tipo")),
                rs.getString("actor"),
                rs.getString("sucursal"),
                rs.getString("descripcion")), parametros.toArray());
    }

    private static String autorActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || !auth.isAuthenticated() ? "sistema" : auth.getName();
    }
}
