package com.ferreteria.usuarios.infrastructure.out.persistence;

import com.ferreteria.usuarios.application.port.out.UsuarioRepository;
import com.ferreteria.usuarios.domain.NombreUsuarioDuplicadoException;
import com.ferreteria.usuarios.domain.Rol;
import com.ferreteria.usuarios.domain.Usuario;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
class JdbcUsuarioRepository implements UsuarioRepository {

    private static final String SELECT = """
            SELECT id, nombre_usuario, clave_hash, rol, activo, descuento_pct, sucursal_id
            FROM usuarios
            """;

    private static final RowMapper<Usuario> MAPPER = (rs, n) -> {
        BigDecimal descuento = rs.getBigDecimal("descuento_pct");
        int sucursalLeida = rs.getInt("sucursal_id");
        Integer sucursal = rs.wasNull() ? null : sucursalLeida;
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nombre_usuario"),
                rs.getString("clave_hash"),
                Rol.deClave(rs.getString("rol")),
                rs.getBoolean("activo"),
                descuento == null ? BigDecimal.ZERO : descuento,
                sucursal);
    };

    private final JdbcTemplate jdbc;

    JdbcUsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Usuario> porNombre(String nombreUsuario) {
        return jdbc.query(SELECT + "WHERE nombre_usuario = ?", MAPPER, nombreUsuario).stream().findFirst();
    }

    @Override
    public Optional<Usuario> porId(int usuarioId) {
        return jdbc.query(SELECT + "WHERE id = ?", MAPPER, usuarioId).stream().findFirst();
    }

    @Override
    public List<Usuario> listar() {
        return jdbc.query(SELECT + "ORDER BY nombre_usuario", MAPPER);
    }

    @Override
    public List<Usuario> listarVendedores() {
        return jdbc.query(SELECT + "WHERE rol = 'vendedor' ORDER BY nombre_usuario", MAPPER);
    }

    @Override
    public List<Usuario> domiciliariosActivos(int sucursalId) {
        return jdbc.query(SELECT + "WHERE rol = 'domiciliario' AND activo AND sucursal_id = ? ORDER BY nombre_usuario",
                MAPPER, sucursalId);
    }

    @Override
    public boolean hayUsuarios() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM usuarios)", Boolean.class));
    }

    @Override
    public void crear(String nombreUsuario, String claveHash, Rol rol, Integer sucursalId) {
        try {
            jdbc.update("INSERT INTO usuarios (nombre_usuario, clave_hash, rol, sucursal_id) VALUES (?, ?, ?, ?)",
                    nombreUsuario, claveHash, rol.getClave(), sucursalId);
        } catch (DuplicateKeyException e) {
            throw new NombreUsuarioDuplicadoException();
        }
    }

    @Override
    public void alternarActivo(int usuarioId) {
        jdbc.update("UPDATE usuarios SET activo = NOT activo WHERE id = ? AND nombre_usuario <> ?",
                usuarioId, Usuario.NOMBRE_ADMIN_PRINCIPAL);
    }

    @Override
    public void actualizarDescuento(int usuarioId, BigDecimal porcentaje) {
        jdbc.update("UPDATE usuarios SET descuento_pct = ? WHERE id = ? AND rol = 'vendedor'", porcentaje, usuarioId);
    }
}
