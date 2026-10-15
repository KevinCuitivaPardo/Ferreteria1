package com.ferreteria.pedidos.infrastructure.out.tiemporeal;

import com.ferreteria.pedidos.application.port.out.AvisosDeEntrega;
import com.ferreteria.tiemporeal.application.port.in.PublicarAvisos;
import com.ferreteria.tiemporeal.domain.Aviso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Conecta los pedidos con el módulo de tiempo real, solo a través de su puerto público. */
@Component
class AvisosTiempoReal implements AvisosDeEntrega {

    private static final Logger log = LoggerFactory.getLogger(AvisosTiempoReal.class);

    private final PublicarAvisos avisos;

    AvisosTiempoReal(PublicarAvisos avisos) {
        this.avisos = avisos;
    }

    @Override
    public void pedidoAsignado(int domiciliarioId, int pedidoId, String numero, String cliente, String direccion) {
        enviar(() -> avisos.avisarAUsuario(domiciliarioId, new Aviso("asignacion", "Nuevo pedido asignado",
                numero + " · " + cliente + " · " + direccion, "/Entregas")));
    }

    @Override
    public void pedidoRetirado(int domiciliarioId, int pedidoId, String numero) {
        enviar(() -> avisos.avisarAUsuario(domiciliarioId, new Aviso("cola", "Cambió tu lista de pedidos",
                "Te quitaron el pedido " + numero, "/Entregas")));
    }

    @Override
    public void avisarAAdmins(String titulo, String mensaje, String enlace) {
        enviar(() -> avisos.avisarAAdmins(new Aviso("admin", titulo, mensaje, enlace)));
    }

    /** Un aviso que falla nunca debe deshacer ni frenar la operación que lo originó. */
    private static void enviar(Runnable aviso) {
        try {
            aviso.run();
        } catch (RuntimeException e) {
            log.warn("No se pudo enviar el aviso en tiempo real: {}", e.getMessage());
        }
    }
}
