package com.vozsegura.vozsegura.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador global de errores.
 * Evita exponer información sensible en mensajes de error.
 */
@Controller
@ControllerAdvice
public class GlobalErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorController.class);

    /**
     * Maneja la ruta /error por defecto de Spring Boot.
     */
    @RequestMapping("/error")
    public String handleError() {
        return "error/generic-error";
    }

    /**
     * Maneja excepciones no controladas.
     * Registra internamente pero no expone detalles al usuario.
     */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        // Log interno sin exponer al usuario
        log.error("Error no controlado", e);
        return "error/generic-error";
    }
}

