package com.ferreteria.pedidos.infrastructure.out.persistence;

import com.ferreteria.pedidos.application.port.out.ClienteRepository;
import com.ferreteria.pedidos.domain.Cliente;
import com.ferreteria.pedidos.domain.DatosCliente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JdbcClienteRepository implements ClienteRepository {

    private final JdbcTemplate jdbc;

    JdbcClienteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Cliente> porCelular(String celular) {
        return jdbc.query("""
                SELECT nombre_ferreteria, nombre_cliente, direccion, ubicacion, horario, celular_2
                FROM clientes
                WHERE celular_1 = ? OR celular_2 = ?
                ORDER BY id
                LIMIT 1""", (rs, n) -> new Cliente(
                rs.getString("nombre_ferreteria"),
                rs.getString("nombre_cliente"),
                rs.getString("direccion"),
                rs.getString("ubicacion"),
                rs.getString("horario"),
                rs.getString("celular_2")), celular, celular).stream().findFirst();
    }

    @Override
    public Optional<Integer> idPorCelular(String celular) {
        return jdbc.query("SELECT id FROM clientes WHERE celular_1 = ? OR celular_2 = ? ORDER BY id LIMIT 1",
                (rs, n) -> rs.getInt("id"), celular, celular).stream().findFirst();
    }

    @Override
    public int crear(DatosCliente d) {
        return jdbc.queryForObject("""
                INSERT INTO clientes
                    (nombre_ferreteria, nombre_cliente, direccion, ubicacion, horario, celular_1, celular_2)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id""", Integer.class,
                d.nombreFerreteria(), d.nombreCliente(), d.direccion(), d.ubicacion(), d.horario(),
                d.celular1(), d.celular2());
    }
}
