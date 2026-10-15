package com.ferreteria.pedidos.infrastructure.in.web;

import com.ferreteria.pedidos.application.port.in.ConsultarPedidos;
import com.ferreteria.pedidos.application.port.in.GestionarEntrega;
import com.ferreteria.pedidos.application.port.in.GestionarEntrega.Resultado;
import com.ferreteria.pedidos.domain.EstadoPedido;
import com.ferreteria.pedidos.domain.FotoEntrega;
import com.ferreteria.pedidos.domain.PedidoDetalle;
import com.ferreteria.pedidos.domain.PedidoResumen;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.web.Sesion;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/** Pantallas del domiciliario: su cola de pedidos, la ruta, y comenzar y terminar cada entrega. */
@Controller
class EntregaController {

    private final ConsultarPedidos consulta;
    private final GestionarEntrega entregas;

    EntregaController(ConsultarPedidos consulta, GestionarEntrega entregas) {
        this.consulta = consulta;
        this.entregas = entregas;
    }

    /** La cola: la entrega en ruta, las que esperan (en orden) y las ya cerradas. */
    @GetMapping("/Entregas")
    String lista(Authentication auth, Model model) {
        List<PedidoResumen> todas = consulta.entregasDe(Sesion.usuarioId(auth));

        model.addAttribute("enRuta",
                todas.stream().filter(e -> esEstado(e, EstadoPedido.EN_RUTA)).findFirst().orElse(null));
        model.addAttribute("enEspera", todas.stream().filter(EntregaController::estaEnEspera).toList());
        model.addAttribute("cerradas", todas.stream().filter(e -> !estaPorEntregar(e)).toList());
        return "entregas/lista";
    }

    @GetMapping("/Entregas/{id}")
    String detalle(@PathVariable int id, Authentication auth, Model model) {
        int domiciliarioId = Sesion.usuarioId(auth);
        Optional<PedidoDetalle> entrega = consulta.entrega(id, domiciliarioId);
        if (entrega.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        EstadoPedido estado = EstadoPedido.deClave(entrega.get().pedido().estado()).orElse(EstadoPedido.PENDIENTE);
        boolean otraEnRuta = consulta.entregasDe(domiciliarioId).stream()
                .anyMatch(e -> e.id() != id && esEstado(e, EstadoPedido.EN_RUTA));

        model.addAttribute("detalle", entrega.get());
        model.addAttribute("pedido", entrega.get().pedido());
        model.addAttribute("enRuta", estado == EstadoPedido.EN_RUTA);
        model.addAttribute("puedeComenzar", estado.estaPorEntregar() && estado != EstadoPedido.EN_RUTA && !otraEnRuta);
        model.addAttribute("otraEnRuta", otraEnRuta && estado.estaPorEntregar() && estado != EstadoPedido.EN_RUTA);
        model.addAttribute("cerrada", !estado.estaPorEntregar());
        return "entregas/detalle";
    }

    @PostMapping("/Entregas/{id}/comenzar")
    String comenzar(@PathVariable int id, Authentication auth, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("mensaje", entregas.comenzar(id, Sesion.usuarioId(auth)));
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/Entregas/" + id;
    }

    /** Cierra la entrega: Entregado (con foto) o No entregado (con el motivo). */
    @PostMapping("/Entregas/{id}/terminar")
    String terminar(@PathVariable int id, @RequestParam String resultado,
                    @RequestParam(defaultValue = "") String nota,
                    @RequestParam(required = false) MultipartFile foto,
                    Authentication auth, RedirectAttributes flash) {
        try {
            Resultado tipo = "no_entregado".equals(resultado) ? Resultado.NO_ENTREGADO : Resultado.ENTREGADO;
            FotoEntrega fotoEntrega = foto == null || foto.isEmpty() ? null : FotoEntrega.deBytes(foto.getBytes());

            flash.addFlashAttribute("mensaje", entregas.terminar(id, Sesion.usuarioId(auth), tipo, nota, fotoEntrega));
            return "redirect:/Entregas";
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            flash.addFlashAttribute("error", "No se pudo leer la foto. Inténtalo de nuevo.");
        }
        return "redirect:/Entregas/" + id;
    }

    private static boolean esEstado(PedidoResumen pedido, EstadoPedido estado) {
        return estado.getClave().equals(pedido.estado());
    }

    private static boolean estaPorEntregar(PedidoResumen pedido) {
        return EstadoPedido.deClave(pedido.estado()).map(EstadoPedido::estaPorEntregar).orElse(false);
    }

    private static boolean estaEnEspera(PedidoResumen pedido) {
        return estaPorEntregar(pedido) && !esEstado(pedido, EstadoPedido.EN_RUTA);
    }
}
