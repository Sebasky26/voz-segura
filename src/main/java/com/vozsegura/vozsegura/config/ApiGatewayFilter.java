package com.vozsegura.vozsegura.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de Validación de Headers del Gateway
 * 
 * Este filtro valida que las peticiones lleguen con los headers correctos
 * desde el API Gateway, y los agrega al request para que puedan ser
 * utilizados por los controladores.
 * 
 * El verdadero gateway (validación JWT, auditoría, etc.) está en
 * voz-segura-gateway (puerto 8080).
 * 
 * Este filtro es el último nivel de validación en la aplicación Core.
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026 (Migrado a Spring Cloud Gateway)
 */
@Component
@Order(1)
public class ApiGatewayFilter implements Filter {

    private static final String[] PUBLIC_PATHS = {
        "/auth/",
        "/denuncia/",
        "/css/",
        "/js/",
        "/images/",
        "/favicon.ico",
        "/error",
        "/terms"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 1. Permitir rutas públicas
        if (isPublicPath(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Para rutas protegidas, validar headers del Gateway
        String cedula = httpRequest.getHeader("X-User-Cedula");
        String userType = httpRequest.getHeader("X-User-Type");
        String apiKey = httpRequest.getHeader("X-Api-Key");

        if (cedula == null || userType == null || apiKey == null) {
            System.out.println("[CORE SECURITY] BLOCKED - Missing headers from Gateway for " + requestUri);
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Petición debe venir desde el API Gateway");
            return;
        }

        // 3. Validar que el usuario tenga acceso a la ruta solicitada
        if (!isAuthorized(requestUri, userType)) {
            System.out.println("[CORE SECURITY] BLOCKED - Unauthorized access by " +
                             userType + " to " + requestUri);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                "No tiene permisos para acceder a este recurso");
            return;
        }

        // 4. Agregar headers de seguridad
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        System.out.println("[CORE SECURITY] ALLOWED - " + userType + " (" + cedula + ") accessing " + requestUri);
        
        chain.doFilter(request, response);
    }

    /**
     * Verifica si la ruta es pública (no requiere autenticación).
     */
    private boolean isPublicPath(String requestUri) {
        if (requestUri.equals("/favicon.ico")) {
            return true;
        }

        for (String publicPath : PUBLIC_PATHS) {
            if (requestUri.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el usuario tiene autorización para acceder al recurso.
     */
    private boolean isAuthorized(String requestUri, String userType) {
        if (userType == null) {
            return false;
        }

        if ("ADMIN".equals(userType)) {
            // Admin tiene acceso a todo excepto rutas públicas
            return requestUri.startsWith("/admin/") || requestUri.startsWith("/staff/");
        }

        if ("ANALYST".equals(userType)) {
            // Analyst (Staff) tiene acceso a /staff/ y /denuncia/
            return requestUri.startsWith("/staff/") || requestUri.startsWith("/denuncia/");
        }

        if ("DENUNCIANTE".equals(userType)) {
            // Denunciante solo tiene acceso a /denuncia/
            return requestUri.startsWith("/denuncia/");
        }

        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("===========================================");
        System.out.println(" VOZ SEGURA - CORE SERVICE INITIALIZED");
        System.out.println(" Spring Cloud Gateway Architecture v2.0");
        System.out.println(" JWT + API Keys validation enabled");
        System.out.println("===========================================");
    }

    @Override
    public void destroy() {
        System.out.println("[CORE SERVICE] Shutting down...");
    }
}

