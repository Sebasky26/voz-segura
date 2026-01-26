package com.vozsegura.controller.publicview;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Terms and Conditions Controller - Muestra términos y condiciones públicos.
 *
 * Responsabilidades:
 * - Servir página de términos y condiciones (pública, sin autenticación)
 * - Permitir que usuarios denunciantes lean términos antes de hacer denuncia
 * - Cumplir requisitos legales de transparencia y consentimiento
 *
 * Rutas:
 * - GET /terms : Página HTML con términos y condiciones (pública)
 *
 * Integración:
 * - @Controller: MVC controller que renderiza HTML
 * - Ruta pública (permitida por ApiGatewayFilter)
 * - Template: public/terms.html (Thymeleaf)
 *
 * @author Voz Segura Team
 * @version 1.0
 */
@Controller
public class TermsController {

    /**
     * Muestra página de términos y condiciones.
     * Ruta pública: no requiere autenticación.
     *
     * @return Nombre de template a renderizar: "public/terms"
     */
    @GetMapping("/terms")
    public String terms() {
        return "public/terms";
    }
}
