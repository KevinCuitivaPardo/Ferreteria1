package com.ferreteria.usuarios.application.port.out;

import com.ferreteria.usuarios.domain.Rol;
import com.ferreteria.usuarios.domain.Usuario;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Puerto de salida: almacenamiento de usuarios. */
public interface UsuarioRepository {

    Optional<Usuario> porNombre(String nombreUsuario);

    Optional<Usuario> porId(int usuarioId);

    List<Usuario> listar();

    List<Usuario> listarVendedores();

    List<Usuario> domiciliariosActivos(int sucursalId);

    boolean hayUsuarios();

    /** @throws com.ferreteria.usuarios.domain.NombreUsuarioDuplicadoException si el nombre ya existe */
    void crear(String nombreUsuario, String claveHash, Rol rol, Integer sucursalId);

    void alternarActivo(int usuarioId);

    void actualizarDescuento(int usuarioId, BigDecimal porcentaje);
}
