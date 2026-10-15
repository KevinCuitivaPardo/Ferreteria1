package com.ferreteria.inventario.infrastructure.out.persistence;

import com.ferreteria.inventario.application.port.out.ProductoRepository;
import com.ferreteria.inventario.domain.CodigoProductoDuplicadoException;
import com.ferreteria.inventario.domain.DatosProducto;
import com.ferreteria.inventario.domain.ExistenciaSucursal;
import com.ferreteria.inventario.domain.Producto;
import com.ferreteria.inventario.domain.ProductoConMovimientosException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** El stock vive en inventario_sucursal; aquí cada producto se lee con el stock de una sucursal. */
@Repository
class JdbcProductoRepository implements ProductoRepository {

    private static final String SELECT = """
            SELECT p.id, p.codigo, p.nombre, p.medida, p.precio_compra, p.precio_venta,
                   COALESCE(i.stock, 0) AS stock, p.stock_minimo
            FROM productos p
            LEFT JOIN inventario_sucursal i ON i.producto_id = p.id AND i.sucursal_id = ?
            """;

    private static final RowMapper<Producto> MAPPER = (rs, n) -> new Producto(
            rs.getInt("id"),
            rs.getString("codigo"),
            rs.getString("nombre"),
            rs.getString("medida"),
            rs.getBigDecimal("precio_compra"),
            rs.getBigDecimal("precio_venta"),
            rs.getInt("stock"),
            rs.getInt("stock_minimo"));

    private final JdbcTemplate jdbc;

    JdbcProductoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Producto> listar(int sucursalId) {
        return jdbc.query(SELECT + "ORDER BY p.nombre, p.codigo", MAPPER, sucursalId);
    }

    @Override
    public List<Producto> buscar(String texto, int sucursalId, int limite) {
        String patron = "%" + texto + "%";
        return jdbc.query(SELECT + """
                WHERE p.codigo ILIKE ? OR p.nombre ILIKE ? OR p.medida ILIKE ?
                ORDER BY p.codigo
                LIMIT ?""", MAPPER, sucursalId, patron, patron, patron, limite);
    }

    @Override
    public List<Producto> conExistencias(int sucursalId) {
        return jdbc.query(SELECT + "WHERE COALESCE(i.stock, 0) > 0 ORDER BY p.codigo", MAPPER, sucursalId);
    }

    @Override
    public Optional<Producto> porId(int productoId, int sucursalId) {
        return jdbc.query(SELECT + "WHERE p.id = ?", MAPPER, sucursalId, productoId).stream().findFirst();
    }

    @Override
    public List<ExistenciaSucursal> existencias(int productoId) {
        return jdbc.query("""
                SELECT s.id, s.nombre, COALESCE(i.stock, 0) AS stock
                FROM sucursales s
                LEFT JOIN inventario_sucursal i ON i.sucursal_id = s.id AND i.producto_id = ?
                WHERE s.activa
                ORDER BY s.nombre""", (rs, n) -> new ExistenciaSucursal(
                rs.getInt("id"), rs.getString("nombre"), rs.getInt("stock")), productoId);
    }

    @Override
    public void insertar(DatosProducto d, int sucursalId) {
        try {
            Integer id = jdbc.queryForObject("""
                    INSERT INTO productos (codigo, nombre, medida, precio_compra, stock_minimo)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id""", Integer.class,
                    d.codigo(), d.nombre(), d.medida(), d.precioCompra(), d.stockMinimo());
            jdbc.update("INSERT INTO inventario_sucursal (producto_id, sucursal_id, stock) VALUES (?, ?, ?)",
                    id, sucursalId, d.stock());
        } catch (DuplicateKeyException e) {
            throw new CodigoProductoDuplicadoException("Ya existe un producto con ese código.");
        }
    }

    @Override
    public void actualizar(int productoId, DatosProducto d) {
        try {
            jdbc.update("""
                    UPDATE productos
                    SET codigo = ?, nombre = ?, medida = ?, precio_compra = ?, stock_minimo = ?
                    WHERE id = ?""",
                    d.codigo(), d.nombre(), d.medida(), d.precioCompra(), d.stockMinimo(), productoId);
        } catch (DuplicateKeyException e) {
            throw new CodigoProductoDuplicadoException("Ya existe otro producto con ese código.");
        }
    }

    @Override
    public void eliminar(int productoId) {
        try {
            jdbc.update("DELETE FROM productos WHERE id = ?", productoId);
        } catch (DataIntegrityViolationException e) {
            throw new ProductoConMovimientosException();
        }
    }

    @Override
    public Optional<Producto> porIdBloqueado(int productoId, int sucursalId) {
        asegurarFila(productoId, sucursalId);
        return jdbc.query("""
                SELECT p.id, p.codigo, p.nombre, p.medida, p.precio_compra, p.precio_venta,
                       i.stock, p.stock_minimo
                FROM productos p
                JOIN inventario_sucursal i ON i.producto_id = p.id AND i.sucursal_id = ?
                WHERE p.id = ?
                FOR UPDATE OF i""", MAPPER, sucursalId, productoId).stream().findFirst();
    }

    @Override
    public void fijarStock(int productoId, int sucursalId, int stock) {
        asegurarFila(productoId, sucursalId);
        jdbc.update("UPDATE inventario_sucursal SET stock = ? WHERE producto_id = ? AND sucursal_id = ?",
                stock, productoId, sucursalId);
    }

    @Override
    public boolean sumarStock(int productoId, int sucursalId, int delta) {
        asegurarFila(productoId, sucursalId);
        return jdbc.update("""
                UPDATE inventario_sucursal SET stock = stock + ?
                WHERE producto_id = ? AND sucursal_id = ? AND stock + ? >= 0""",
                delta, productoId, sucursalId, delta) > 0;
    }

    /** Una sucursal nueva no tiene fila de un producto hasta que se mueve su stock por primera vez. */
    private void asegurarFila(int productoId, int sucursalId) {
        jdbc.update("""
                INSERT INTO inventario_sucursal (producto_id, sucursal_id, stock)
                SELECT id, ?, 0 FROM productos WHERE id = ?
                ON CONFLICT (producto_id, sucursal_id) DO NOTHING""", sucursalId, productoId);
    }
}
