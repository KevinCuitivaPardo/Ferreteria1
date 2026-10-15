package com.ferreteria.usuarios.infrastructure.in.web;

import com.ferreteria.shared.NegocioException;
import com.ferreteria.sucursales.application.port.in.ConsultarSucursales;
import com.ferreteria.sucursales.domain.Sucursal;
import com.ferreteria.usuarios.application.port.in.ConsultarUsuarios;
import com.ferreteria.usuarios.application.port.in.GestionarUsuarios;
import com.ferreteria.usuarios.application.port.in.GestionarUsuarios.NuevoUsuario;
import com.ferreteria.usuarios.domain.Rol;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

/** Administración de usuarios y de los descuentos de cada vendedor (solo admin). */
@Controller
class UsuarioController {

    private final ConsultarUsuarios consulta;
    private final GestionarUsuarios gestion;
    private final ConsultarSucursales sucursales;

    UsuarioController(ConsultarUsuarios consulta, GestionarUsuarios gestion, ConsultarSucursales sucursales) {
        this.consulta = consulta;
        this.gestion = gestion;
        this.sucursales = sucursales;
    }

    // ---------- Usuarios ----------

    @GetMapping("/Usuarios")
    String lista(Model model) {
        return pantalla(model, "", Rol.VENDEDOR.getClave(), null);
    }

    @PostMapping("/Usuarios")
    String crear(@RequestParam(defaultValue = "") String nombreUsuario,
                 @RequestParam(defaultValue = "") String clave,
                 @RequestParam(defaultValue = "vendedor") String rol,
                 @RequestParam(required = false) Integer sucursalId,
                 Model model, RedirectAttributes flash) {
        try {
            gestion.crear(new NuevoUsuario(nombreUsuario, clave, Rol.deClave(rol), sucursalId));
            flash.addFlashAttribute("mensaje", "Usuario '" + nombreUsuario.trim() + "' creado.");
            return "redirect:/Usuarios";
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
            return pantalla(model, nombreUsuario, rol, sucursalId);
        }
    }

    @PostMapping("/Usuarios/{id}/activo")
    String cambiarActivo(@PathVariable int id, RedirectAttributes flash) {
        gestion.alternarActivo(id);
        flash.addFlashAttribute("mensaje", "Estado del usuario actualizado.");
        return "redirect:/Usuarios";
    }

    private String pantalla(Model model, String nombreUsuario, String rol, Integer sucursalId) {
        Map<Integer, String> nombres = new LinkedHashMap<>();
        for (Sucursal s : sucursales.listar()) {
            nombres.put(s.id(), s.nombre());
        }

        model.addAttribute("lista", consulta.listar());
        model.addAttribute("sucursales", sucursales.activas());
        model.addAttribute("nombresSucursal", nombres);
        model.addAttribute("roles", Rol.values());
        model.addAttribute("nombreUsuario", nombreUsuario);
        model.addAttribute("rol", rol);
        model.addAttribute("sucursalElegida", sucursalId);
        return "usuarios/lista";
    }

    // ---------- Descuentos ----------

    @GetMapping("/Descuentos")
    String descuentos(Model model) {
        model.addAttribute("form", DescuentosForm.desde(consulta.vendedores()));
        return "usuarios/descuentos";
    }

    @PostMapping("/Descuentos/{id}/quitar")
    String quitarDescuento(@PathVariable int id, RedirectAttributes flash) {
        try {
            gestion.quitarDescuento(id);
            flash.addFlashAttribute("mensaje", "Descuento eliminado. Ese vendedor ahora vende sin descuento.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/Descuentos";
    }

    @PostMapping("/Descuentos")
    String guardarDescuentos(@ModelAttribute("form") DescuentosForm form, Model model) {
        try {
            gestion.guardarDescuentos(form.aDescuentos());
            model.addAttribute("mensaje", "Descuentos guardados. Se aplicarán a los pedidos y ventas nuevos.");
            model.addAttribute("form", DescuentosForm.desde(consulta.vendedores()));
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
        }
        return "usuarios/descuentos";
    }
}
