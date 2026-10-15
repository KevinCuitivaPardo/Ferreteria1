package com.ferreteria.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Páginas generales: inicio, login y acceso denegado. */
@Controller
class InicioController {

    @GetMapping("/")
    String inicio() {
        return "index";
    }

    /** Pantalla de confirmación para quien llega a /Salir con un enlace; el cierre real es un POST. */
    @GetMapping("/Salir")
    String confirmarSalida() {
        return "salir";
    }

    @GetMapping("/Login")
    String login() {
        return "login";
    }

    /** A donde se manda a quien no tiene permiso; acepta cualquier método (por ejemplo un POST sin token CSRF). */
    @RequestMapping("/acceso-denegado")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String accesoDenegado(Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("mensaje", "No tienes permiso para ver esta página.");
        return "error";
    }
}
