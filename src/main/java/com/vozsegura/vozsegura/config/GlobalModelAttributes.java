package com.vozsegura.vozsegura.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Advice global que inyecta la URL del Gateway en todas las vistas.
 * Esto permite que los templates Thymeleaf usen URLs absolutas al Gateway.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final GatewayConfig gatewayConfig;

    public GlobalModelAttributes(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Inyecta la URL base del Gateway en todas las vistas.
     * Uso en Thymeleaf: th:href="${gatewayUrl + '/auth/login'}"
     */
    @ModelAttribute("gatewayUrl")
    public String gatewayUrl() {
        return gatewayConfig.getBaseUrl();
    }

    /**
     * Inyecta la URL de login del Gateway.
     * Uso en Thymeleaf: th:href="${loginUrl}"
     */
    @ModelAttribute("loginUrl")
    public String loginUrl() {
        return gatewayConfig.getLoginUrl();
    }
}
