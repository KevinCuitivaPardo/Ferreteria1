package com.ferreteria.inventario.infrastructure.in.web;

import com.ferreteria.inventario.application.port.in.ConsultarInventario;
import com.ferreteria.inventario.application.port.in.MoverStock;
import com.ferreteria.inventario.domain.Producto;
import com.ferreteria.inventario.domain.TipoMovimiento;
import com.ferreteria.shared.NegocioException;
import com.ferreteria.shared.web.Sesion;
import com.ferreteria.shared.web.SucursalActual;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/** Entradas, salidas y ajustes de inventario de un producto. */
@Controller
class MovimientoController {

    private final ConsultarInventario consulta;
    private final MoverStock stock;

    MovimientoController(ConsultarInventario consulta, MoverStock stock) {
        this.consulta = consulta;
        this.stock = stock;
    }

    // ---------- Movimientos (entrada / salida) ----------

    @GetMapping("/Movimientos/{id}")
    String ver(@PathVariable int id, @SucursalActual int sucursalId, Authentication auth, Model model) {
        return cargar(id, sucursalId, TipoMovimiento.ENTRADA, auth, model);
    }

    @PostMapping("/Movimientos/{id}")
    String registrar(@PathVariable int id,
                     @RequestParam(defaultValue = "entrada") String tipo,
                     @RequestParam(required = false) Integer cantidad,
                     @RequestParam(defaultValue = "") String motivo,
                     @SucursalActual int sucursalId, Authentication auth, Model model,
                     RedirectAttributes flash) {
        TipoMovimiento tipoMovimiento = TipoMovimiento.deClave(tipo);
        try {
            stock.registrarMovimiento(id, sucursalId, tipoMovimiento, cantidad == null ? 0 : cantidad, motivo);
            flash.addFlashAttribute("mensaje", "Movimiento guardado.");
            return "redirect:/Movimientos/" + id;
        } catch (NegocioException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cantidad", cantidad);
            model.addAttribute("motivo", motivo);
            return cargar(id, sucursalId, tipoMovimiento, auth, model);
        }
    }

    private String cargar(int id, int sucursalId, TipoMovimiento tipo, Authentication auth, Model model) {
        Optional<Producto> producto = consulta.porId(id, sucursalId);
        if (producto.isEmpty()) {
            return "redirect:/Productos";
        }

        model.addAttribute("producto", producto.get());
        model.addAttribute("tipo", tipo.getClave());
        // Solo el administrador ve el historial y las existencias de todas las sucursales.
        if (Sesion.esAdmin(auth)) {
            model.addAttribute("historial", consulta.historial(id, sucursalId));
            model.addAttribute("existencias", consulta.existencias(id));
        }
        return "productos/movimientos";
    }

    // ---------- Ajuste manual (solo admin) ----------

    @GetMapping("/Ajustar/{id}")
    String ajustar(@PathVariable int id, @SucursalActual int sucursalId, Model model) {
        return consulta.porId(id, sucursalId)
                .map(p -> {
                    model.addAttribute("producto", p);
                    model.addAttribute("nuevoStock", p.stock());
                    return "productos/ajustar";
                })
                .orElse("redirect:/Productos");
    }

    @PostMapping("/Ajustar/{id}")
    String guardarAjuste(@PathVariable int id,
                         @RequestParam(required = false) Integer nuevoStock,
                         @RequestParam(defaultValue = "") String motivo,
                         @SucursalActual int sucursalId, Model model, RedirectAttributes flash) {
        Optional<Producto> producto = consulta.porId(id, sucursalId);
        if (producto.isEmpty()) {
            return "redirect:/Productos";
        }

        try {
            stock.ajustar(id, sucursalId, nuevoStock == null ? 0 : nuevoStock, motivo);
            flash.addFlashAttribute("mensaje", "Inventario ajustado correctamente.");
            return "redirect:/Productos";
        } catch (NegocioException e) {
            model.addAttribute("producto", producto.get());
            model.addAttribute("nuevoStock", nuevoStock);
            model.addAttribute("motivo", motivo);
            model.addAttribute("error", e.getMessage());
            return "productos/ajustar";
        }
    }
}
