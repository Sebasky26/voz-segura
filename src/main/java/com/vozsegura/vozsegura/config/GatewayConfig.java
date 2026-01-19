package com.vozsegura.vozsegura.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Proporciona la URL base del API Gateway para redirecciones.
 * Esto asegura que todas las redirecciones vayan al puerto 8080 (Gateway)
 * en lugar del puerto 8082 (Core).
 */
@Component
public class GatewayConfig {

    @Value("${vozsegura.gateway.base-url:http://localhost:8080}")
    private String gatewayBaseUrl;

    /**
     * Obtiene la URL base del Gateway.
     */
    public String getBaseUrl() {
        return gatewayBaseUrl;
    }

    /**
     * Construye una URL completa al Gateway con la ruta especificada.
     */
    public String buildUrl(String path) {
        if (path == null || path.isEmpty()) {
            return gatewayBaseUrl;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return gatewayBaseUrl + path;
    }

    /**
     * Obtiene la URL de login del Gateway.
     */
    public String getLoginUrl() {
        return buildUrl("/auth/login");
    }

    /**
     * Obtiene la URL de login del Gateway con parámetro de sesión expirada.
     */
    public String getSessionExpiredUrl() {
        return buildUrl("/auth/login?session_expired");
    }

    /**
     * Genera un redirect string para Spring MVC que va al Gateway.
     * Uso: return gatewayConfig.redirectTo("/auth/login");
     */
    public String redirectTo(String path) {
        return "redirect:" + buildUrl(path);
    }

    /**
     * Genera un redirect al login del Gateway.
     */
    public String redirectToLogin() {
        return "redirect:" + getLoginUrl();
    }

    /**
     * Genera un redirect al login del Gateway con sesión expirada.
     */
    public String redirectToSessionExpired() {
        return "redirect:" + getSessionExpiredUrl();
    }
}
