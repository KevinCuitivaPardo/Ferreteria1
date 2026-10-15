package com.ferreteria.sucursales.application;

import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.eventos.RegistroEventos;
import com.ferreteria.shared.eventos.TipoEvento;
import com.ferreteria.sucursales.application.port.in.ConsultarSucursales;
import com.ferreteria.sucursales.application.port.in.GestionarSucursales;
import com.ferreteria.sucursales.application.port.out.SucursalRepository;
import com.ferreteria.sucursales.domain.DatosSucursal;
import com.ferreteria.sucursales.domain.Sucursal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SucursalService implements ConsultarSucursales, GestionarSucursales {

    private final SucursalRepository sucursales;
    private final RegistroEventos eventos;

    public SucursalService(SucursalRepository sucursales, RegistroEventos eventos) {
        this.sucursales = sucursales;
        this.eventos = eventos;
    }

    // ---------- ConsultarSucursales ----------

    @Override
    public List<Sucursal> listar() {
        return sucursales.listar();
    }

    @Override
    public List<Sucursal> activas() {
        return sucursales.listar().stream().filter(Sucursal::activa).toList();
    }

    @Override
    public Optional<Sucursal> porId(int sucursalId) {
        return sucursales.porId(sucursalId);
    }

    // ---------- GestionarSucursales ----------

    @Override
    @Transactional
    public void crear(DatosSucursal datos) {
        DatosSucursal limpios = validar(datos);
        sucursales.insertar(limpios);
        eventos.registrar(TipoEvento.SUCURSAL, null, "Sucursal creada: " + limpios.nombre()
                + " (" + limpios.direccion() + ")");
    }

    @Override
    @Transactional
    public void actualizar(int sucursalId, DatosSucursal datos) {
        DatosSucursal limpios = validar(datos);
        sucursales.actualizar(sucursalId, limpios);
        eventos.registrar(TipoEvento.SUCURSAL, sucursalId, "Sucursal actualizada: " + limpios.nombre());
    }

    @Override
    @Transactional
    public void alternarActiva(int sucursalId) {
        Sucursal sucursal = sucursales.porId(sucursalId)
                .orElseThrow(() -> new NegocioException("La sucursal no existe."));

        if (sucursal.activa() && activas().size() <= 1) {
            throw new NegocioException("Debe quedar al menos una sucursal activa.");
        }
        sucursales.alternarActiva(sucursalId);
        eventos.registrar(TipoEvento.SUCURSAL, sucursalId,
                (sucursal.activa() ? "Sucursal desactivada: " : "Sucursal activada: ") + sucursal.nombre());
    }

    private static DatosSucursal validar(DatosSucursal d) {
        if (d.nombre() == null || d.nombre().isBlank()) {
            throw new NegocioException("Escribe el nombre de la sucursal.");
        }
        if (d.direccion() == null || d.direccion().isBlank()) {
            throw new NegocioException("Escribe la dirección de la sucursal.");
        }
        boolean unaCoordenada = (d.latitud() == null) != (d.longitud() == null);
        boolean fueraDeRango = d.latitud() != null
                && (Math.abs(d.latitud()) > 90 || Math.abs(d.longitud()) > 180);
        if (unaCoordenada || fueraDeRango) {
            throw new NegocioException("La ubicación en el mapa no es válida. Márcala de nuevo.");
        }
        return new DatosSucursal(d.nombre().trim(), d.direccion().trim(), d.latitud(), d.longitud());
    }
}
