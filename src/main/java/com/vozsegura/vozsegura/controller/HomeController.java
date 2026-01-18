package com.vozsegura.vozsegura.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador para la página de inicio.
 * Redirige a la página de login unificado.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Redirigir al login unificado (ZTA)
        return "redirect:/auth/login";
    }
}
