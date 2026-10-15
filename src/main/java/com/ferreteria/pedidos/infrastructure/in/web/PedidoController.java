package com.ferreteria.pedidos.infrastructure.in.web;

import com.ferreteria.pedidos.application.port.in.AsignarDomiciliario;
import com.ferreteria.pedidos.application.port.in.CambiarEstadoPedido;
import com.ferreteria.pedidos.application.port.in.ConsultarPedidos;
import com.ferreteria.pedidos.application.port.in.CorregirUbicacion;
import com.ferreteria.pedidos.application.port.in.CrearPedido;
import com.ferreteria.pedidos.domain.Cliente;
import com.ferreteria.pedidos.domain.Domiciliario;
import com.ferreteria.pedidos.domain.EstadoPedido;
import com.ferreteria.pedidos.domain.PedidoDetalle;
import com.ferreteria.pedidos.domain.PedidoMapa;
import com.ferreteria.pedidos.domain.PedidoResumen;
import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.web.Sesion;
import com.ferreteria.shared.web.SucursalActual;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Domicilios, vistos por el administrador y por los vendedores. */
@Controller
class PedidoController {

    private final ConsultarPedidos consulta;
    private final CrearPedido creacion;
    private final CambiarEstadoPedido cambioEstado;
    private final AsignarDomiciliario asignacion;
    private final CorregirUbicacion ubicacion;

    PedidoController(ConsultarPedidos consulta, CrearPedido creacion, CambiarEstadoPedido cambioEstado,
                     AsignarDomiciliario asignacion, CorregirUbicacion ubicacion) {
        this.consulta = consulta;
        this.creacion = creacion;
        this.cambioEstado = cambioEstado;
        this.asignacion = asignacion;
        this.ubicacion = ubicacion;
    }

    // ---------- Lista y mapa ----------

    @GetMapping("/Pedidos/Lista")
    String lista(Authentication auth, Model model) {
        model.addAttribute("pedidos", consulta.recientes(auth.getName(), Sesion.esAdmin(auth)));
        return "pedidos/lista";
    }

    /** Mapa con los domicilios ubicados y los domiciliarios en vivo, y los pedidos que faltan por asignar. */
    @GetMapping("/Pedidos/Mapa")
    String mapa(Model model) {
        List<PedidoResumen> porAsignar = consulta.sinAsignar();

        // Los domiciliarios que se pueden elegir para cada pedido son los de su misma sucursal
        Map<Integer, List<Domiciliario>> domiciliarios = new LinkedHashMap<>();
        porAsignar.stream().map(PedidoResumen::sucursalId).distinct()
                .forEach(id -> domiciliarios.put(id, consulta.domiciliariosDe(id)));

        model.addAttribute("porAsignar", porAsignar);
        model.addAttribute("domiciliariosPorSucursal", domiciliarios);
        return "pedidos/mapa";
    }

    @GetMapping("/Pedidos/Mapa/datos")
    @ResponseBody
    List<PedidoMapa> datosDelMapa() {
        return consulta.paraElMapa();
    }

    // ---------- Detalle ----------

    @GetMapping("/Pedidos/Detalle/{id}")
    String detalle(@PathVariable int id, Authentication auth, Model model) {
        Optional<PedidoDetalle> detalle = consulta.detalle(id, auth.getName(), Sesion.esAdmin(auth));
        if (detalle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("detalle", detalle.get());
        model.addAttribute("pedido", detalle.get().pedido());
        model.addAttribute("estados", EstadoPedido.values());
        if (Sesion.esAdmin(auth)) {
            model.addAttribute("domiciliarios", consulta.domiciliariosDe(detalle.get().pedido().sucursalId()));
        }
        return "pedidos/detalle";
    }

    @PostMapping("/Pedidos/Detalle/{id}/estado")
    String cambiarEstado(@PathVariable int id, @RequestParam String estado, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("mensaje", cambioEstado.cambiarEstado(id, estado));
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/Pedidos/Detalle/" + id;
    }

    @PostMapping("/Pedidos/Detalle/{id}/domiciliario")
    String asignar(@PathVariable int id, @RequestParam(defaultValue = "0") int domiciliarioId,
                   @RequestParam(required = false) String volver, RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("mensaje", asignacion.asignar(id, domiciliarioId));
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        // Desde el mapa se asigna sin salir de él
        return "mapa".equals(volver) ? "redirect:/Pedidos/Mapa" : "redirect:/Pedidos/Detalle/" + id;
    }

    /** El administrador ajusta a mano el punto de entrega en el mapa. */
    @PostMapping("/Pedidos/Detalle/{id}/ubicacion")
    String corregirUbicacion(@PathVariable int id, @RequestParam double latitud, @RequestParam double longitud,
                             RedirectAttributes flash) {
        try {
            flash.addFlashAttribute("mensaje", ubicacion.corregir(id, latitud, longitud));
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/Pedidos/Detalle/" + id;
    }

    // ---------- Nuevo pedido ----------

    @GetMapping("/Pedidos/Nuevo")
    String nuevo(@SucursalActual int sucursalId, Authentication auth, Model model) {
        return pantallaNuevo(PedidoForm.vacio(CrearPedido.MAX_LINEAS), List.of(), sucursalId, auth, model);
    }

    @PostMapping("/Pedidos/Nuevo")
    String crear(@ModelAttribute("form") PedidoForm form, @SucursalActual int sucursalId,
                 Authentication auth, Model model) {
        try {
            int id = creacion.crear(new CrearPedido.Comando(sucursalId, auth.getName(), form.datosCliente(),
                    form.getDescuentoElegido(), MedioPago.deClave(form.getMedioPago()), form.getMontoRecibido(),
                    form.lineasPedido()));
            return "redirect:/Pedidos/Detalle/" + id;
        } catch (NegocioException e) {
            return pantallaNuevo(form, e.getMensajes(), sucursalId, auth, model);
        }
    }

    /** Búsqueda de cliente por celular, la llama el formulario al salir del campo. */
    @GetMapping("/Pedidos/Nuevo/cliente")
    @ResponseBody
    Map<String, Object> cliente(@RequestParam(defaultValue = "") String cel) {
        Optional<Cliente> encontrado = consulta.buscarCliente(cel);
        Map<String, Object> respuesta = new LinkedHashMap<>();

        if (encontrado.isEmpty()) {
            respuesta.put("encontrado", false);
            return respuesta;
        }

        Cliente c = encontrado.get();
        respuesta.put("encontrado", true);
        respuesta.put("nombreFerreteria", textoOVacio(c.nombreFerreteria()));
        respuesta.put("nombreCliente", textoOVacio(c.nombreCliente()));
        respuesta.put("direccion", textoOVacio(c.direccion()));
        respuesta.put("ubicacion", textoOVacio(c.ubicacion()));
        respuesta.put("horario", textoOVacio(c.horario()));
        respuesta.put("celular2", textoOVacio(c.celular2()));
        return respuesta;
    }

    private String pantallaNuevo(PedidoForm form, List<String> errores, int sucursalId, Authentication auth,
                                 Model model) {
        model.addAttribute("form", form);
        model.addAttribute("errores", errores);
        model.addAttribute("nombreVendedor", auth.getName());
        model.addAttribute("descuentoMaximo", consulta.descuentoMaximo(auth.getName()));
        model.addAttribute("productos", consulta.productosDisponibles(sucursalId));
        model.addAttribute("mediosPago", MedioPago.values());
        return "pedidos/nuevo";
    }

    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto;
    }
}
