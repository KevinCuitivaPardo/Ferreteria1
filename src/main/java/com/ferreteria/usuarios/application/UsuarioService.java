package com.ferreteria.usuarios.application;

import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import com.ferreteria.usuarios.application.port.in.GestionarUsuarios;
import com.ferreteria.usuarios.application.port.out.CodificadorClave;
import com.ferreteria.usuarios.application.port.out.UsuarioRepository;
import com.ferreteria.usuarios.domain.Rol;
import com.ferreteria.usuarios.domain.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService implements ConsultarUsuarios, GestionarUsuarios {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final UsuarioRepository usuarios;
    private final CodificadorClave codificador;
    private final RegistroEventos eventos;

    public UsuarioService(UsuarioRepository usuarios, CodificadorClave codificador, RegistroEventos eventos) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.eventos = eventos;
    }

    // ---------- Consultas ----------

    @Override
    public List<Usuario> listar() {
        return usuarios.listar();
    }

    @Override
    public List<Usuario> vendedores() {
        return usuarios.listarVendedores();
    }

    @Override
    public Optional<Usuario> porNombre(String nombreUsuario) {
        return usuarios.porNombre(nombreUsuario);
    }

    @Override
    public Optional<Usuario> porId(int usuarioId) {
        return usuarios.porId(usuarioId);
    }

    @Override
    public List<Usuario> domiciliarios(int sucursalId) {
        return usuarios.domiciliariosActivos(sucursalId);
    }

    // ---------- Gestión ----------

    @Override
    @Transactional
    public void crear(NuevoUsuario nuevo) {
        String nombre = nuevo.nombreUsuario() == null ? "" : nuevo.nombreUsuario().trim();
        if (nombre.isEmpty() || nuevo.clave() == null || nuevo.clave().isBlank()) {
            throw new NegocioException("Escribe el usuario y la contraseña.");
        }
        if (nuevo.rol() != Rol.ADMIN && nuevo.sucursalId() == null) {
            throw new NegocioException("Elige la sucursal a la que pertenece el usuario.");
        }
        Integer sucursalId = nuevo.rol() == Rol.ADMIN ? null : nuevo.sucursalId();

        usuarios.crear(nombre, codificador.codificar(nuevo.clave()), nuevo.rol(), sucursalId);
        eventos.registrar(TipoEvento.USUARIO, sucursalId,
                "Usuario creado: " + nombre + " (" + nuevo.rol().getEtiqueta() + ")");
    }

    @Override
    @Transactional
    public void alternarActivo(int usuarioId) {
        usuarios.alternarActivo(usuarioId);
        usuarios.porId(usuarioId).ifPresent(u -> eventos.registrar(TipoEvento.USUARIO, u.sucursalId(),
                (u.activo() ? "Usuario activado: " : "Usuario desactivado: ") + u.nombreUsuario()));
    }

    @Override
    @Transactional
    public void guardarDescuentos(List<DescuentoVendedor> descuentos) {
        for (DescuentoVendedor d : descuentos) {
            BigDecimal pct = d.porcentaje();
            if (pct == null || pct.signum() < 0 || pct.compareTo(CIEN) > 0) {
                throw new NegocioException(
                        "El descuento de " + d.nombreUsuario() + " debe estar entre 0 y 100.");
            }
        }
        for (DescuentoVendedor d : descuentos) {
            usuarios.actualizarDescuento(d.usuarioId(), d.porcentaje());
            eventos.registrar(TipoEvento.DESCUENTO, null, "Descuento máximo de " + d.nombreUsuario()
                    + " definido en " + d.porcentaje().stripTrailingZeros().toPlainString() + "%");
        }
    }

    @Override
    @Transactional
    public void quitarDescuento(int vendedorId) {
        Usuario vendedor = usuarios.porId(vendedorId)
                .filter(u -> u.rol() == Rol.VENDEDOR)
                .orElseThrow(() -> new NegocioException("El vendedor no existe."));

        if (vendedor.descuentoPct().signum() == 0) {
            throw new NegocioException(vendedor.nombreUsuario() + " no tiene descuento para quitar.");
        }

        usuarios.actualizarDescuento(vendedorId, BigDecimal.ZERO);
        eventos.registrar(TipoEvento.DESCUENTO, vendedor.sucursalId(), "Se quitó el descuento de "
                + vendedor.nombreUsuario() + " (era " + vendedor.descuentoPct().stripTrailingZeros().toPlainString()
                + "%)");
    }

    @Override
    public void crearAdminInicialSiHaceFalta(String nombreUsuario, String clave) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null || clave.isBlank()) {
            return;
        }
        if (!usuarios.hayUsuarios()) {
            usuarios.crear(nombreUsuario, codificador.codificar(clave), Rol.ADMIN, null);
        }
    }
}
