package com.ferreteria.ventas.infrastructure.in.web;

import com.ferreteria.shared.MedioPago;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.web.Sesion;
import com.ferreteria.shared.web.SucursalActual;
import com.ferreteria.ventas.application.port.in.ConsultarVentas;
import com.ferreteria.ventas.application.port.in.RegistrarVenta;
import com.ferreteria.ventas.domain.VentaDetalle;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/** Ventas en el local. */
@Controller
class VentaController {

    private final ConsultarVentas consulta;
    private final RegistrarVenta registro;

    VentaController(ConsultarVentas consulta, RegistrarVenta registro) {
        this.consulta = consulta;
        this.registro = registro;
    }

    @GetMapping("/Ventas")
    String lista(Authentication auth, Model model) {
        model.addAttribute("ventas", consulta.recientes(auth.getName(), Sesion.esAdmin(auth)));
        return "ventas/lista";
    }

    @GetMapping("/Ventas/{id}")
    String detalle(@PathVariable int id, Authentication auth, Model model) {
        Optional<VentaDetalle> detalle = consulta.detalle(id, auth.getName(), Sesion.esAdmin(auth));
        if (detalle.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("detalle", detalle.get());
        model.addAttribute("venta", detalle.get().venta());
        return "ventas/detalle";
    }

    // ---------- Nueva venta ----------

    @GetMapping("/Ventas/Nueva")
    String nueva(@SucursalActual int sucursalId, Authentication auth, Model model) {
        return pantallaNueva(VentaForm.vacio(RegistrarVenta.MAX_LINEAS), List.of(), sucursalId, auth, model);
    }

    @PostMapping("/Ventas/Nueva")
    String registrar(@ModelAttribute("form") VentaForm form, @SucursalActual int sucursalId,
                     Authentication auth, Model model) {
        try {
            int id = registro.registrar(new RegistrarVenta.Comando(sucursalId, auth.getName(),
                    form.getClienteNombre(), form.getClienteTelefono(), form.getDescuentoElegido(),
                    MedioPago.deClave(form.getMedioPago()), form.getMontoRecibido(), form.lineasVenta()));
            return "redirect:/Ventas/" + id;
        } catch (NegocioException e) {
            return pantallaNueva(form, e.getMensajes(), sucursalId, auth, model);
        }
    }

    private String pantallaNueva(VentaForm form, List<String> errores, int sucursalId, Authentication auth,
                                 Model model) {
        model.addAttribute("form", form);
        model.addAttribute("errores", errores);
        model.addAttribute("nombreVendedor", auth.getName());
        model.addAttribute("descuentoMaximo", consulta.descuentoMaximo(auth.getName()));
        model.addAttribute("productos", consulta.productosDisponibles(sucursalId));
        model.addAttribute("mediosPago", MedioPago.values());
        return "ventas/nueva";
    }
}
