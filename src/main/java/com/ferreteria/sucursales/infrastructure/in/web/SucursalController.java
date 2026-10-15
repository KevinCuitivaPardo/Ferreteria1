package com.ferreteria.sucursales.infrastructure.in.web;

import com.ferreteria.shared.NegocioException;
import com.ferreteria.sucursales.application.port.in.ConsultarSucursales;
import com.ferreteria.sucursales.application.port.in.GestionarSucursales;
import com.ferreteria.sucursales.domain.Sucursal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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

import java.net.URI;
import java.util.List;
import java.util.Map;

/** Administración de sucursales y cambio de sucursal activa (solo admin). */
@Controller
class SucursalController {

    private final ConsultarSucursales consulta;
    private final GestionarSucursales gestion;
    private final ContextoSucursal contexto;

    SucursalController(ConsultarSucursales consulta, GestionarSucursales gestion, ContextoSucursal contexto) {
        this.consulta = consulta;
        this.gestion = gestion;
        this.contexto = contexto;
    }

    @GetMapping("/Sucursales")
    String lista(Model model) {
        model.addAttribute("lista", consulta.listar());
        return "sucursales/lista";
    }

    /** Sucursales que ya tienen punto en el mapa, para el mapa de domicilios. */
    @GetMapping("/Sucursales/datos")
    @ResponseBody
    List<Map<String, Object>> datos() {
        return consulta.activas().stream()
                .filter(Sucursal::tieneUbicacion)
                .map(s -> Map.<String, Object>of(
                        "nombre", s.nombre(),
                        "direccion", s.direccion(),
                        "latitud", s.latitud(),
                        "longitud", s.longitud()))
                .toList();
    }

    // ---------- Nueva ----------

    @GetMapping("/Sucursales/Nueva")
    String nueva(Model model) {
        model.addAttribute("sucursal", new SucursalForm());
        model.addAttribute("accion", "/Sucursales/Nueva");
        model.addAttribute("titulo", "Nueva sucursal");
        return "sucursales/formulario";
    }

    @PostMapping("/Sucursales/Nueva")
    String crear(@ModelAttribute("sucursal") SucursalForm form, BindingResult binding,
                 Model model, RedirectAttributes flash) {
        if (!binding.hasErrors()) {
            try {
                gestion.crear(form.aDatos());
                flash.addFlashAttribute("mensaje", "Sucursal creada.");
                return "redirect:/Sucursales";
            } catch (NegocioException e) {
                model.addAttribute("error", e.getMessage());
            }
        } else {
            model.addAttribute("error", "La ubicación en el mapa no es válida. Márcala de nuevo.");
        }
        model.addAttribute("accion", "/Sucursales/Nueva");
        model.addAttribute("titulo", "Nueva sucursal");
        return "sucursales/formulario";
    }

    // ---------- Editar ----------

    @GetMapping("/Sucursales/{id}/editar")
    String editar(@PathVariable int id, Model model) {
        return consulta.porId(id)
                .map(s -> {
                    model.addAttribute("sucursal", SucursalForm.desde(s));
                    model.addAttribute("accion", "/Sucursales/" + id + "/editar");
                    model.addAttribute("titulo", "Editar sucursal");
                    return "sucursales/formulario";
                })
                .orElse("redirect:/Sucursales");
    }

    @PostMapping("/Sucursales/{id}/editar")
    String guardar(@PathVariable int id, @ModelAttribute("sucursal") SucursalForm form,
                   BindingResult binding, Model model, RedirectAttributes flash) {
        if (!binding.hasErrors()) {
            try {
                gestion.actualizar(id, form.aDatos());
                flash.addFlashAttribute("mensaje", "Sucursal actualizada.");
                return "redirect:/Sucursales";
            } catch (NegocioException e) {
                model.addAttribute("error", e.getMessage());
            }
        } else {
            model.addAttribute("error", "La ubicación en el mapa no es válida. Márcala de nuevo.");
        }
        model.addAttribute("accion", "/Sucursales/" + id + "/editar");
        model.addAttribute("titulo", "Editar sucursal");
        return "sucursales/formulario";
    }

    @PostMapping("/Sucursales/{id}/activa")
    String alternarActiva(@PathVariable int id, RedirectAttributes flash) {
        try {
            gestion.alternarActiva(id);
            flash.addFlashAttribute("mensaje", "Estado de la sucursal actualizado.");
        } catch (NegocioException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/Sucursales";
    }

    // ---------- Sucursal activa del administrador ----------

    @PostMapping("/Sucursales/activa")
    String elegirActiva(@RequestParam int id, HttpSession sesion, HttpServletRequest peticion) {
        consulta.porId(id).filter(s -> s.activa())
                .ifPresent(s -> sesion.setAttribute(ContextoSucursal.CLAVE_SESION, s.id()));
        return "redirect:" + rutaDeRegreso(peticion.getHeader("Referer"));
    }

    /** Vuelve a la pantalla de donde vino, sin permitir saltos a otros sitios. */
    private static String rutaDeRegreso(String referer) {
        try {
            if (referer != null) {
                URI uri = URI.create(referer);
                String ruta = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
                return uri.getRawQuery() == null ? ruta : ruta + "?" + uri.getRawQuery();
            }
        } catch (IllegalArgumentException ignorado) {
            // referer mal formado: se vuelve al inicio
        }
        return "/";
    }
}
