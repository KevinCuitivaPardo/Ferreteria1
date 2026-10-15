package com.ferreteria.inventario.infrastructure.in.web;

import com.ferreteria.inventario.application.port.in.ConsultarInventario;
import com.ferreteria.inventario.application.port.in.GestionarProductos;
import com.ferreteria.inventario.domain.Producto;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.web.SucursalActual;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
class ProductoController {

    private static final String REVISA_DATOS = "Revisa los datos: hay números con formato incorrecto.";

    private final ConsultarInventario consulta;
    private final GestionarProductos gestion;

    ProductoController(ConsultarInventario consulta, GestionarProductos gestion) {
        this.consulta = consulta;
        this.gestion = gestion;
    }

    @GetMapping("/Productos")
    String lista(@SucursalActual int sucursalId, Model model) {
        model.addAttribute("lista", consulta.listar(sucursalId));
        return "productos/lista";
    }

    /** Lo llama el buscador de la lista mientras se escribe. */
    @GetMapping("/Productos/buscar")
    @ResponseBody
    List<Producto> buscar(@RequestParam(defaultValue = "") String q, @SucursalActual int sucursalId) {
        return consulta.buscar(q, sucursalId);
    }

    @PostMapping("/Productos/{id}/eliminar")
    String eliminar(@PathVariable int id, RedirectAttributes flash) {
        try {
            gestion.eliminar(id);
            flash.addFlashAttribute("mensaje", "Producto eliminado.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/Productos";
    }

    // ---------- Nuevo ----------

    @GetMapping("/Nuevo")
    String nuevo(Model model) {
        model.addAttribute("producto", new ProductoForm());
        return "productos/nuevo";
    }

    @PostMapping("/Nuevo")
    String crear(@ModelAttribute("producto") ProductoForm form, BindingResult binding,
                 @SucursalActual int sucursalId, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("error", REVISA_DATOS);
            return "productos/nuevo";
        }
        try {
            gestion.crear(form.aDatos(), sucursalId);
            return "redirect:/Productos";
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
            return "productos/nuevo";
        }
    }

    // ---------- Editar ----------

    @GetMapping("/Editar/{id}")
    String editar(@PathVariable int id, @SucursalActual int sucursalId, Model model) {
        return consulta.porId(id, sucursalId)
                .map(p -> {
                    model.addAttribute("producto", ProductoForm.desde(p));
                    return "productos/editar";
                })
                .orElse("redirect:/Productos");
    }

    @PostMapping("/Editar/{id}")
    String guardar(@PathVariable int id, @ModelAttribute("producto") ProductoForm form,
                   BindingResult binding, Model model) {
        form.setId(id);
        if (binding.hasErrors()) {
            model.addAttribute("error", REVISA_DATOS);
            return "productos/editar";
        }
        try {
            gestion.actualizar(id, form.aDatos());
            return "redirect:/Productos";
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
            return "productos/editar";
        }
    }
}
