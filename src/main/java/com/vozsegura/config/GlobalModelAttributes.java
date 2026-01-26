package com.vozsegura.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global Model Attributes - Inyecta datos comunes en todas las vistas Thymeleaf.
 *
 * Responsabilidades:
 * - Providenciar gatewayUrl a todos los templates (para URLs del Gateway)
 * - Providenciar loginUrl para redirecciones de autenticación
 * - Evitar duplicación de configuración en cada controlador
 * - Centralizar acceso a GatewayConfig
 *
 * Integración:
 * - @ControllerAdvice: Aplica a TODOS los controladores del proyecto
 * - @ModelAttribute: Inyecta automáticamente en Model de cada request
 * - Atributos disponibles en templates: ${gatewayUrl}, ${loginUrl}
 *
 * Uso en Thymeleaf:
 * - Enlace a Gateway: th:href="${gatewayUrl + '/auth/login'}"
 * - Redirección login: th:href="${loginUrl}"
 * - Forms: th:action="${gatewayUrl + '/api/endpoint'}"
 *
 * @author Voz Segura Team
 * @version 1.0
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final GatewayConfig gatewayConfig;

    /**
     * Constructor con inyeccion de dependencias.
     *
     * @param gatewayConfig Configuración del Gateway
     */
    public GlobalModelAttributes(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Inyecta la URL base del Gateway en todas las vistas.
     * Disponible como ${gatewayUrl} en Thymeleaf templates.
     *
     * @return URL base del Gateway (ej: https://gateway.voz-segura.ec)
     */
    @ModelAttribute("gatewayUrl")
    public String gatewayUrl() {
        return gatewayConfig.getBaseUrl();
    }

    /**
     * Inyecta la URL de login del Gateway en todas las vistas.
     * Disponible como ${loginUrl} en Thymeleaf templates.
     *
     * @return URL de login (GatewayConfig.getLoginUrl())
     */
    @ModelAttribute("loginUrl")
    public String loginUrl() {
        return gatewayConfig.getLoginUrl();
    }
}
