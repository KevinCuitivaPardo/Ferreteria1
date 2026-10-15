package com.ferreteria.tiemporeal.application;

import com.ferreteria.tiemporeal.application.port.in.ConsultarPosiciones;
import com.ferreteria.tiemporeal.application.port.in.PublicarAvisos;
import com.ferreteria.tiemporeal.application.port.in.ReportarPosicion;
import com.ferreteria.tiemporeal.application.port.out.CanalTiempoReal;
import com.ferreteria.tiemporeal.application.port.out.PosicionesActuales;
import com.ferreteria.tiemporeal.domain.Aviso;
import com.ferreteria.tiemporeal.domain.MensajeTiempoReal;
import com.ferreteria.tiemporeal.domain.PosicionDomiciliario;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TiempoRealService implements ReportarPosicion, ConsultarPosiciones, PublicarAvisos {

    /** Un teléfono que no reporta hace más de esto deja de aparecer. */
    static final Duration VIGENCIA = Duration.ofMinutes(30);

    /** No se reenvían posiciones más seguidas que esto: ahorra datos y trabajo. */
    static final Duration INTERVALO_MINIMO = Duration.ofMillis(1500);

    private final PosicionesActuales posiciones;
    private final CanalTiempoReal canal;

    public TiempoRealService(PosicionesActuales posiciones, CanalTiempoReal canal) {
        this.posiciones = posiciones;
        this.canal = canal;
    }

    // ---------- ReportarPosicion ----------

    @Override
    public void reportar(int usuarioId, String nombre, Integer sucursalId, double latitud, double longitud,
                         Double precisionMetros) {
        boolean valida = !Double.isNaN(latitud) && !Double.isNaN(longitud)
                && Math.abs(latitud) <= 90 && Math.abs(longitud) <= 180;
        if (!valida) {
            return;
        }

        Instant ahora = Instant.now();
        boolean demasiadoSeguido = posiciones.de(usuarioId)
                .map(p -> Duration.between(p.actualizada(), ahora).compareTo(INTERVALO_MINIMO) < 0)
                .orElse(false);
        if (demasiadoSeguido) {
            return;
        }

        PosicionDomiciliario nueva = new PosicionDomiciliario(usuarioId, nombre, sucursalId, latitud, longitud,
                precisionMetros, ahora);
        posiciones.guardar(nueva);
        canal.enviarAAdmins(new MensajeTiempoReal("posicion", nueva));
    }

    // ---------- ConsultarPosiciones ----------

    @Override
    public List<PosicionDomiciliario> actuales() {
        return posiciones.desde(Instant.now().minus(VIGENCIA));
    }

    // ---------- PublicarAvisos ----------

    @Override
    public void avisarAUsuario(int usuarioId, Aviso aviso) {
        canal.enviarAUsuario(usuarioId, new MensajeTiempoReal("aviso", aviso));
    }

    @Override
    public void avisarAAdmins(Aviso aviso) {
        canal.enviarAAdmins(new MensajeTiempoReal("aviso", aviso));
    }
}
