package com.vozsegura.vozsegura.controller;

import java.util.UUID;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vozsegura.vozsegura.config.GatewayConfig;

/**
 * Controlador global de errores.
 * Evita exponer información sensible en mensajes de error.
 */
@Controller
public class GlobalErrorController implements ErrorController {
    
    private final GatewayConfig gatewayConfig;
    
    public GlobalErrorController(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Maneja la ruta /error por defecto de Spring Boot.
     */
    @RequestMapping("/error")
    public String handleError(Model model) {
        String requestId = generateRequestId();
        model.addAttribute("requestId", requestId);
        model.addAttribute("loginUrl", gatewayConfig.getLoginUrl());
        return "error/generic-error";
    }

    /**
     * Genera un ID único de solicitud para trazabilidad.
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

