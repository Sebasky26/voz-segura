package com.vozsegura.gateway.config;

import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuración de seguridad por rutas del Gateway.
 * 
 * Define qué rutas son públicas (sin autenticación JWT)
 * y qué rutas son protegidas (requieren JWT válido).
 * 
 * Arquitectura Zero Trust:
 * - Rutas públicas: Sin headers del Gateway (acceso abierto)
 * - Rutas protegidas: Requieren JWT válido en Authorization header
 * - Core siempre valida HMAC-SHA256 de Gateway
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Component
public class RouteSecurityConfig {

    /**
     * Rutas públicas que NO requieren autenticación JWT.
     * El Gateway las deja pasar sin validar token.
     */
    private static final Set<String> PUBLIC_ROUTES = new HashSet<>();
    
    static {
        // Autenticación
        PUBLIC_ROUTES.add("/auth/login");
        PUBLIC_ROUTES.add("/auth/verify-start");
        PUBLIC_ROUTES.add("/auth/verify-callback");
        PUBLIC_ROUTES.add("/auth/logout");
        
        // Denuncias públicas
        PUBLIC_ROUTES.add("/denuncia");
        PUBLIC_ROUTES.add("/denuncia/");
        
        // Seguimiento público
        PUBLIC_ROUTES.add("/seguimiento");
        PUBLIC_ROUTES.add("/seguimiento/");
        
        // Términos y condiciones
        PUBLIC_ROUTES.add("/terms");
        PUBLIC_ROUTES.add("/terms/");
        
        // Recursos estáticos
        PUBLIC_ROUTES.add("/css");
        PUBLIC_ROUTES.add("/js");
        PUBLIC_ROUTES.add("/img");
        PUBLIC_ROUTES.add("/images");
        
        // Webhooks
        PUBLIC_ROUTES.add("/webhooks");
        PUBLIC_ROUTES.add("/webhooks/");
        
        // Error
        PUBLIC_ROUTES.add("/error");
        
        // Health check
        PUBLIC_ROUTES.add("/actuator/health");
    }

    /**
     * Verifica si una ruta es pública (no requiere autenticación).
     * 
     * @param path ruta solicitada
     * @return true si la ruta es pública
     */
    public boolean isPublicRoute(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // Eliminar query string
        String cleanPath = path.split("\\?")[0];
        
        // Verificar coincidencias exactas
        if (PUBLIC_ROUTES.contains(cleanPath)) {
            return true;
        }
        
        // Verificar prefijos (rutas dinámicas)
        for (String publicRoute : PUBLIC_ROUTES) {
            if (cleanPath.startsWith(publicRoute + "/")) {
                return true;
            }
            if (cleanPath.startsWith(publicRoute)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Verifica si una ruta requiere autenticación JWT.
     * 
     * @param path ruta solicitada
     * @return true si la ruta requiere JWT válido
     */
    public boolean isProtectedRoute(String path) {
        return !isPublicRoute(path);
    }

    /**
     * Obtiene los roles permitidos para una ruta específica.
     * 
     * Ejemplo:
     * - /staff/** → {ANALYST, ADMIN}
     * - /admin/** → {ADMIN}
     * - /denuncia/** → {} (público)
     * 
     * @param path ruta solicitada
     * @return Set de roles permitidos (vacío si público)
     */
    public Set<String> getAllowedRoles(String path) {
        Set<String> allowedRoles = new HashSet<>();
        
        if (path == null) {
            return allowedRoles;
        }
        
        // Rutas protegidas por rol
        if (path.startsWith("/staff")) {
            allowedRoles.add("ANALYST");
            allowedRoles.add("ADMIN");
        } else if (path.startsWith("/admin")) {
            allowedRoles.add("ADMIN");
        }
        
        return allowedRoles;
    }
}
