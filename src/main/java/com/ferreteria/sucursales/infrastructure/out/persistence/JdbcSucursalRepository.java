package com.ferreteria.sucursales.infrastructure.out.persistence;

import com.ferreteria.sucursales.application.port.out.SucursalRepository;
import com.ferreteria.sucursales.domain.DatosSucursal;
import com.ferreteria.sucursales.domain.Sucursal;
import com.ferreteria.sucursales.domain.SucursalDuplicadaException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
class JdbcSucursalRepository implements SucursalRepository {

    private static final String COLUMNAS = "id, nombre, direccion, latitud, longitud, activa";

    private static final RowMapper<Sucursal> MAPPER = (rs, n) -> new Sucursal(
            rs.getInt("id"),
            rs.getString("nombre"),
            rs.getString("direccion"),
            decimalONulo(rs, "latitud"),
            decimalONulo(rs, "longitud"),
            rs.getBoolean("activa"));

    private final JdbcTemplate jdbc;

    JdbcSucursalRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Sucursal> listar() {
        return jdbc.query("SELECT " + COLUMNAS + " FROM sucursales ORDER BY nombre", MAPPER);
    }

    @Override
    public Optional<Sucursal> porId(int sucursalId) {
        return jdbc.query("SELECT " + COLUMNAS + " FROM sucursales WHERE id = ?", MAPPER, sucursalId)
                .stream().findFirst();
    }

    @Override
    public void insertar(DatosSucursal d) {
        try {
            jdbc.update("INSERT INTO sucursales (nombre, direccion, latitud, longitud) VALUES (?, ?, ?, ?)",
                    d.nombre(), d.direccion(), d.latitud(), d.longitud());
        } catch (DuplicateKeyException e) {
            throw new SucursalDuplicadaException();
        }
    }

    @Override
    public void actualizar(int sucursalId, DatosSucursal d) {
        try {
            jdbc.update("UPDATE sucursales SET nombre = ?, direccion = ?, latitud = ?, longitud = ? WHERE id = ?",
                    d.nombre(), d.direccion(), d.latitud(), d.longitud(), sucursalId);
        } catch (DuplicateKeyException e) {
            throw new SucursalDuplicadaException();
        }
    }

    @Override
    public void alternarActiva(int sucursalId) {
        jdbc.update("UPDATE sucursales SET activa = NOT activa WHERE id = ?", sucursalId);
    }

    private static Double decimalONulo(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }
}
