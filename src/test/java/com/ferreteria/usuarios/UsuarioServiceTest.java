package com.ferreteria.usuarios;

import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import com.ferreteria.usuarios.application.UsuarioService;
import com.ferreteria.usuarios.application.port.out.CodificadorClave;
import com.ferreteria.usuarios.application.port.out.UsuarioRepository;
import com.ferreteria.usuarios.domain.Rol;
import com.ferreteria.usuarios.domain.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    UsuarioRepository usuarios = mock(UsuarioRepository.class);
    CodificadorClave codificador = mock(CodificadorClave.class);
    RegistroEventos eventos = mock(RegistroEventos.class);
    UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarios, codificador, eventos);
    }

    private static Usuario usuario(int id, String nombre, Rol rol, String descuento) {
        return new Usuario(id, nombre, "x", rol, true, new BigDecimal(descuento), 1);
    }

    @Test
    void quitarElDescuentoLoDejaEnCeroYQuedaRegistrado() {
        when(usuarios.porId(2)).thenReturn(Optional.of(usuario(2, "ana", Rol.VENDEDOR, "7.50")));

        servicio.quitarDescuento(2);

        verify(usuarios).actualizarDescuento(2, BigDecimal.ZERO);
        verify(eventos).registrar(eq(TipoEvento.DESCUENTO), eq(1), contains("ana (era 7.5%)"));
    }

    @Test
    void noHayNadaQueQuitarSiYaNoTieneDescuento() {
        when(usuarios.porId(2)).thenReturn(Optional.of(usuario(2, "ana", Rol.VENDEDOR, "0")));

        var e = assertThrows(NegocioException.class, () -> servicio.quitarDescuento(2));

        assertEquals("ana no tiene descuento para quitar.", e.getMessage());
        verify(usuarios, never()).actualizarDescuento(anyInt(), any());
    }

    @Test
    void soloLosVendedoresTienenDescuento() {
        when(usuarios.porId(1)).thenReturn(Optional.of(usuario(1, "admin", Rol.ADMIN, "0")));
        when(usuarios.porId(9)).thenReturn(Optional.of(usuario(9, "pedro", Rol.DOMICILIARIO, "0")));

        assertThrows(NegocioException.class, () -> servicio.quitarDescuento(1));
        assertThrows(NegocioException.class, () -> servicio.quitarDescuento(9));
        assertThrows(NegocioException.class, () -> servicio.quitarDescuento(404));
        verify(usuarios, never()).actualizarDescuento(anyInt(), any());
    }
}
