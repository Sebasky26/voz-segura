package com.vozsegura.vozsegura.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuración de URLs del API Gateway.
 * 
 * Propósito:
 * - Centralizar URL base del Gateway (port 8080)
 * - Proporcionar métodos de utilidad para redirecciones
 * - Evitar hardcodear URLs en código
 * 
 * Arquitectura:
 * - API Gateway (puerto 8080): Frontend con JWT
 * - Core (puerto 8082): Backend de procesamiento
 * - Esta aplicación Core debe redirigir al Gateway cuando sea necesario
 * 
 * Casos de Uso:
 * - Redirigir a login cuando sesión expirada
 * - Redirigir a home después de logout
 * - Construir URLs para email/SMS notifications
 * 
 * Configuración:
 * - Property: vozsegura.gateway.base-url (ej: http://localhost:8080)
 * - Environment: VOZSEGURA_GATEWAY_BASE_URL
 * - Default: http://localhost:8080
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Component
public class GatewayConfig {

    @Value("${vozsegura.gateway.base-url:http://localhost:8080}")
    /** URL base del API Gateway (configurable via properties/environment) */
    private String gatewayBaseUrl;

    /**
     * Obtiene la URL base del Gateway.
     * 
     * Ejemplo: http://localhost:8080 (dev) o https://segura.gob.pe (prod)
     * 
     * @return URL base del Gateway
     */
    public String getBaseUrl() {
        return gatewayBaseUrl;
    }

    /**
     * Construye una URL completa al Gateway con la ruta especificada.
     * 
     * Ejemplos:
     * - buildUrl("/auth/login") → "http://localhost:8080/auth/login"
     * - buildUrl("auth/login") → "http://localhost:8080/auth/login"
     * - buildUrl("") → "http://localhost:8080"
     * 
     * @param path Ruta relativa (con o sin leading slash)
     * @return URL completa
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
     * 
     * @return URL completa a /auth/login
     */
    public String getLoginUrl() {
        return buildUrl("/auth/login");
    }

    /**
     * Obtiene la URL de login del Gateway con parámetro de sesión expirada.
     * 
     * @return URL con query parameter ?session_expired
     */
    public String getSessionExpiredUrl() {
        return buildUrl("/auth/login?session_expired");
    }

    /**
     * Genera un redirect string para Spring MVC que va al Gateway.
     * 
     * Uso en Controller:
     * - return gatewayConfig.redirectTo("/auth/login");
     * 
     * @param path Ruta relativa
     * @return String con "redirect:" prefix
     */
    public String redirectTo(String path) {
        return "redirect:" + buildUrl(path);
    }

    /**
     * Genera un redirect al login del Gateway.
     * Utilizado cuando usuario no autenticado accede a ruta protegida.
     * 
     * @return "redirect:http://localhost:8080/auth/login"
     */
    public String redirectToLogin() {
        return "redirect:" + getLoginUrl();
    }

    /**
     * Genera un redirect al login del Gateway con sesión expirada.
     * Utilizado cuando JWT expiró.
     * 
     * @return "redirect:http://localhost:8080/auth/login?session_expired"
     */
    public String redirectToSessionExpired() {
        return "redirect:" + getSessionExpiredUrl();
    }
}
