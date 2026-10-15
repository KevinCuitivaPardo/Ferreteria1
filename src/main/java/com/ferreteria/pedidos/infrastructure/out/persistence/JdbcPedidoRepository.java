package com.ferreteria.pedidos.infrastructure.out.persistence;

import com.ferreteria.pedidos.application.port.out.PedidoRepository;
import com.ferreteria.pedidos.domain.EntregaEnCursoException;
import com.ferreteria.pedidos.domain.EstadoActual;
import com.ferreteria.pedidos.domain.FotoGuardada;
import com.ferreteria.pedidos.domain.FotoPedido;
import com.ferreteria.pedidos.domain.LineaStock;
import com.ferreteria.pedidos.domain.PedidoCabecera;
import com.ferreteria.pedidos.domain.PedidoGuardado;
import com.ferreteria.pedidos.domain.PedidoItem;
import com.ferreteria.pedidos.domain.PedidoMapa;
import com.ferreteria.pedidos.domain.PedidoResumen;
import com.ferreteria.pedidos.domain.Ubicacion;
import com.ferreteria.shared.MedioPago;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
class JdbcPedidoRepository implements PedidoRepository {

    private static final String RESUMEN = """
            SELECT p.id, p.numero, p.fecha, c.nombre_ferreteria, c.nombre_cliente, p.direccion_entrega,
                   u.nombre_usuario AS vendedor, s.nombre AS sucursal, d.nombre_usuario AS domiciliario,
                   p.total, p.estado, p.medio_pago, p.asignado_en, p.sucursal_id
            FROM pedidos p
            JOIN clientes c ON c.id = p.cliente_id
            JOIN usuarios u ON u.id = p.vendedor_id
            JOIN sucursales s ON s.id = p.sucursal_id
            LEFT JOIN usuarios d ON d.id = p.domiciliario_id
            """;

    private static final String CABECERA = """
            SELECT p.id, p.numero, p.fecha, p.estado, p.descuento_pct, p.total,
                   c.nombre_ferreteria, c.nombre_cliente, p.direccion_entrega, c.ubicacion,
                   c.horario, c.celular_1, c.celular_2, u.nombre_usuario AS vendedor,
                   p.sucursal_id, s.nombre AS sucursal, s.latitud AS sucursal_latitud,
                   s.longitud AS sucursal_longitud, p.latitud, p.longitud,
                   p.domiciliario_id, d.nombre_usuario AS domiciliario,
                   p.medio_pago, p.monto_recibido, p.asignado_en, p.iniciado_en, p.finalizado_en,
                   p.nota_entrega
            FROM pedidos p
            JOIN clientes c ON c.id = p.cliente_id
            JOIN usuarios u ON u.id = p.vendedor_id
            JOIN sucursales s ON s.id = p.sucursal_id
            LEFT JOIN usuarios d ON d.id = p.domiciliario_id
            """;

    private static final RowMapper<PedidoResumen> RESUMEN_MAPPER = (rs, n) -> new PedidoResumen(
            rs.getInt("id"),
            rs.getString("numero"),
            rs.getTimestamp("fecha").toLocalDateTime(),
            rs.getString("nombre_ferreteria"),
            rs.getString("nombre_cliente"),
            rs.getString("direccion_entrega"),
            rs.getString("vendedor"),
            rs.getString("sucursal"),
            rs.getString("domiciliario"),
            rs.getBigDecimal("total"),
            rs.getString("estado"),
            MedioPago.deClave(rs.getString("medio_pago")),
            fechaONula(rs, "asignado_en"),
            rs.getInt("sucursal_id"));

    private static final RowMapper<PedidoCabecera> CABECERA_MAPPER = (rs, n) -> new PedidoCabecera(
            rs.getInt("id"),
            rs.getString("numero"),
            rs.getTimestamp("fecha").toLocalDateTime(),
            rs.getString("estado"),
            rs.getBigDecimal("descuento_pct"),
            rs.getBigDecimal("total"),
            rs.getString("nombre_ferreteria"),
            rs.getString("nombre_cliente"),
            rs.getString("direccion_entrega"),
            rs.getString("ubicacion"),
            rs.getString("horario"),
            rs.getString("celular_1"),
            rs.getString("celular_2"),
            rs.getString("vendedor"),
            rs.getInt("sucursal_id"),
            rs.getString("sucursal"),
            decimalONulo(rs, "sucursal_latitud"),
            decimalONulo(rs, "sucursal_longitud"),
            decimalONulo(rs, "latitud"),
            decimalONulo(rs, "longitud"),
            enteroONulo(rs, "domiciliario_id"),
            rs.getString("domiciliario"),
            MedioPago.deClave(rs.getString("medio_pago")),
            rs.getBigDecimal("monto_recibido"),
            fechaONula(rs, "asignado_en"),
            fechaONula(rs, "iniciado_en"),
            fechaONula(rs, "finalizado_en"),
            rs.getString("nota_entrega"));

    private final JdbcTemplate jdbc;

    JdbcPedidoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<PedidoResumen> recientes(Integer vendedorId) {
        String filtro = vendedorId == null ? "" : "AND p.vendedor_id = ? ";
        String sql = RESUMEN + "WHERE p.fecha >= now() - interval '3 months' " + filtro
                + "ORDER BY p.fecha DESC, p.id DESC";
        return vendedorId == null ? jdbc.query(sql, RESUMEN_MAPPER) : jdbc.query(sql, RESUMEN_MAPPER, vendedorId);
    }

    @Override
    public List<PedidoResumen> sinAsignar() {
        return jdbc.query(RESUMEN + """
                WHERE p.domiciliario_id IS NULL AND p.estado IN ('pendiente', 'alistado')
                ORDER BY p.fecha, p.id""", RESUMEN_MAPPER);
    }

    /**
     * La condición del vendedor va dentro de la consulta: un vendedor
     * nunca puede traer un pedido que no sea suyo.
     */
    @Override
    public Optional<PedidoCabecera> cabecera(int pedidoId, boolean esAdmin, int vendedorId) {
        return jdbc.query(CABECERA + "WHERE p.id = ? AND (? OR p.vendedor_id = ?)", CABECERA_MAPPER,
                pedidoId, esAdmin, vendedorId).stream().findFirst();
    }

    @Override
    public Optional<PedidoCabecera> cabeceraDeEntrega(int pedidoId, int domiciliarioId) {
        return jdbc.query(CABECERA + "WHERE p.id = ? AND p.domiciliario_id = ?", CABECERA_MAPPER,
                pedidoId, domiciliarioId).stream().findFirst();
    }

    @Override
    public List<PedidoItem> items(int pedidoId) {
        return jdbc.query("""
                SELECT pr.codigo, pr.nombre, pr.medida, pi.cantidad, pi.precio_unitario
                FROM pedido_items pi
                JOIN productos pr ON pr.id = pi.producto_id
                WHERE pi.pedido_id = ?
                ORDER BY pi.id""", (rs, n) -> new PedidoItem(
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("medida"),
                rs.getInt("cantidad"),
                rs.getBigDecimal("precio_unitario")), pedidoId);
    }

    @Override
    public List<LineaStock> lineasDeStock(int pedidoId) {
        return jdbc.query("SELECT producto_id, cantidad FROM pedido_items WHERE pedido_id = ? ORDER BY id",
                (rs, n) -> new LineaStock(rs.getInt("producto_id"), rs.getInt("cantidad")), pedidoId);
    }

    @Override
    public List<PedidoMapa> ubicados() {
        return jdbc.query("""
                SELECT p.id, p.numero, p.estado, c.nombre_cliente, p.direccion_entrega, p.latitud, p.longitud,
                       s.nombre AS sucursal, d.nombre_usuario AS domiciliario
                FROM pedidos p
                JOIN clientes c ON c.id = p.cliente_id
                JOIN sucursales s ON s.id = p.sucursal_id
                LEFT JOIN usuarios d ON d.id = p.domiciliario_id
                WHERE p.latitud IS NOT NULL AND p.longitud IS NOT NULL
                  AND p.fecha >= now() - interval '3 months'
                ORDER BY p.fecha DESC""", (rs, n) -> new PedidoMapa(
                rs.getInt("id"),
                rs.getString("numero"),
                rs.getString("estado"),
                rs.getString("nombre_cliente"),
                rs.getString("direccion_entrega"),
                rs.getDouble("latitud"),
                rs.getDouble("longitud"),
                rs.getString("sucursal"),
                rs.getString("domiciliario")));
    }

    @Override
    public List<PedidoResumen> entregasDe(int domiciliarioId) {
        return jdbc.query(RESUMEN + """
                WHERE p.domiciliario_id = ?
                ORDER BY CASE p.estado WHEN 'en_ruta' THEN 0
                                       WHEN 'pendiente' THEN 1 WHEN 'alistado' THEN 1 ELSE 2 END,
                         CASE WHEN p.estado IN ('pendiente', 'alistado') THEN p.asignado_en END ASC NULLS LAST,
                         p.finalizado_en DESC NULLS LAST, p.id DESC
                LIMIT 100""", RESUMEN_MAPPER, domiciliarioId);
    }

    @Override
    public Optional<Integer> entregaEnCurso(int domiciliarioId) {
        return jdbc.query("SELECT id FROM pedidos WHERE domiciliario_id = ? AND estado = 'en_ruta'",
                (rs, n) -> rs.getInt("id"), domiciliarioId).stream().findFirst();
    }

    @Override
    public PedidoGuardado insertar(int clienteId, int vendedorId, int sucursalId, String direccionEntrega,
                                   Ubicacion ubicacion, BigDecimal descuentoPct, BigDecimal total,
                                   MedioPago medioPago, BigDecimal montoRecibido) {
        return jdbc.queryForObject("""
                INSERT INTO pedidos (cliente_id, vendedor_id, sucursal_id, direccion_entrega, latitud, longitud,
                                     descuento_pct, total, medio_pago, monto_recibido)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, numero""", (rs, n) -> new PedidoGuardado(rs.getInt("id"), rs.getString("numero")),
                clienteId, vendedorId, sucursalId, direccionEntrega,
                ubicacion == null ? null : ubicacion.latitud(),
                ubicacion == null ? null : ubicacion.longitud(),
                descuentoPct, total, medioPago.getClave(), montoRecibido);
    }

    @Override
    public void insertarItem(int pedidoId, int productoId, int cantidad, BigDecimal precioUnitario) {
        jdbc.update("""
                INSERT INTO pedido_items (pedido_id, producto_id, cantidad, precio_unitario)
                VALUES (?, ?, ?, ?)""", pedidoId, productoId, cantidad, precioUnitario);
    }

    @Override
    public Optional<EstadoActual> estadoBloqueado(int pedidoId) {
        return jdbc.query("SELECT estado, sucursal_id, domiciliario_id FROM pedidos WHERE id = ? FOR UPDATE",
                (rs, n) -> new EstadoActual(rs.getString("estado"), rs.getInt("sucursal_id"),
                        enteroONulo(rs, "domiciliario_id")), pedidoId).stream().findFirst();
    }

    @Override
    public void actualizarEstado(int pedidoId, String claveEstado) {
        try {
            jdbc.update("""
                    UPDATE pedidos
                    SET estado = ?,
                        iniciado_en = CASE WHEN ? = 'en_ruta' THEN COALESCE(iniciado_en, now()) ELSE iniciado_en END,
                        finalizado_en = CASE WHEN ? IN ('entregado', 'no_entregado', 'cancelado') THEN now()
                                             WHEN ? IN ('pendiente', 'alistado', 'en_ruta') THEN NULL
                                             ELSE finalizado_en END
                    WHERE id = ?""", claveEstado, claveEstado, claveEstado, claveEstado, pedidoId);
        } catch (DuplicateKeyException e) {
            // El índice único impide dos pedidos en ruta para el mismo domiciliario
            throw new EntregaEnCursoException();
        }
    }

    @Override
    public void guardarNotaDeEntrega(int pedidoId, String nota) {
        jdbc.update("UPDATE pedidos SET nota_entrega = ? WHERE id = ?", nota, pedidoId);
    }

    @Override
    public void asignarDomiciliario(int pedidoId, Integer domiciliarioId) {
        if (domiciliarioId == null) {
            jdbc.update("UPDATE pedidos SET domiciliario_id = NULL, asignado_en = NULL WHERE id = ?", pedidoId);
        } else {
            jdbc.update("UPDATE pedidos SET domiciliario_id = ?, asignado_en = now() WHERE id = ?",
                    domiciliarioId, pedidoId);
        }
    }

    @Override
    public void actualizarUbicacion(int pedidoId, Ubicacion ubicacion) {
        jdbc.update("UPDATE pedidos SET latitud = ?, longitud = ? WHERE id = ?",
                ubicacion.latitud(), ubicacion.longitud(), pedidoId);
    }

    // ---------- Fotos ----------

    @Override
    public int insertarFoto(int pedidoId, String archivo, String tipoContenido, int tamano, int subidaPor) {
        return jdbc.queryForObject("""
                INSERT INTO pedido_fotos (pedido_id, archivo, tipo_contenido, tamano, subida_por)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id""", Integer.class, pedidoId, archivo, tipoContenido, tamano, subidaPor);
    }

    @Override
    public List<FotoPedido> fotos(int pedidoId) {
        return jdbc.query("""
                SELECT f.id, f.pedido_id, f.tipo_contenido, f.tamano, u.nombre_usuario, f.fecha
                FROM pedido_fotos f
                JOIN usuarios u ON u.id = f.subida_por
                WHERE f.pedido_id = ?
                ORDER BY f.fecha, f.id""", (rs, n) -> new FotoPedido(
                rs.getInt("id"),
                rs.getInt("pedido_id"),
                rs.getString("tipo_contenido"),
                rs.getInt("tamano"),
                rs.getString("nombre_usuario"),
                rs.getTimestamp("fecha").toLocalDateTime()), pedidoId);
    }

    @Override
    public Optional<FotoGuardada> foto(int pedidoId, int fotoId) {
        return jdbc.query("SELECT archivo, tipo_contenido FROM pedido_fotos WHERE id = ? AND pedido_id = ?",
                (rs, n) -> new FotoGuardada(rs.getString("archivo"), rs.getString("tipo_contenido")),
                fotoId, pedidoId).stream().findFirst();
    }

    private static Double decimalONulo(ResultSet rs, String columna) throws SQLException {
        double valor = rs.getDouble(columna);
        return rs.wasNull() ? null : valor;
    }

    private static Integer enteroONulo(ResultSet rs, String columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    private static LocalDateTime fechaONula(ResultSet rs, String columna) throws SQLException {
        Timestamp t = rs.getTimestamp(columna);
        return t == null ? null : t.toLocalDateTime();
    }
}
