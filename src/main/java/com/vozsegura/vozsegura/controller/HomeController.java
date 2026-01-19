package com.vozsegura.vozsegura.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.vozsegura.vozsegura.config.GatewayConfig;

/**
 * Controlador para la página de inicio.
 * Redirige a la página de login unificado.
 */
@Controller
public class HomeController {

    private final GatewayConfig gatewayConfig;

    public HomeController(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    @GetMapping("/")
    public String home() {
        // Redirigir al login unificado (ZTA) a través del gateway
        return gatewayConfig.redirectToLogin();
    }
}
