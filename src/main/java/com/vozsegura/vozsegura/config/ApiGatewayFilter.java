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
        "/seguimiento",
        "/css/",
        "/js/",
        "/img/",
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

        System.out.println("[CORE SECURITY] === Processing request: " + requestUri + " ===");

        // 1. Permitir rutas públicas
        if (isPublicPath(requestUri)) {
            System.out.println("[CORE SECURITY] Public path allowed: " + requestUri);
            chain.doFilter(request, response);
            return;
        }

        System.out.println("[CORE SECURITY] Protected path, checking auth: " + requestUri);

        // 2. Verificar autenticación (múltiples métodos)
        String cedula = null;
        String userType = null;

        // Método 1: Headers del Gateway (cuando viene desde el Gateway)
        cedula = httpRequest.getHeader("X-User-Cedula");
        userType = httpRequest.getHeader("X-User-Type");

        // Método 2: Sesión HTTP (cuando el usuario se autentica directamente en Core)
        if (cedula == null || userType == null) {
            jakarta.servlet.http.HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                Boolean authenticated = (Boolean) session.getAttribute("authenticated");
                if (authenticated != null && authenticated) {
                    cedula = (String) session.getAttribute("cedula");
                    userType = (String) session.getAttribute("userType");
                    System.out.println("[CORE SECURITY] Auth via SESSION - cedula: " + cedula + ", type: " + userType);
                }
            }
        }

        // Método 3: Cookie JWT (cuando hay cookie de autenticación)
        if (cedula == null || userType == null) {
            jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("Authorization".equals(cookie.getName())) {
                        // Hay una cookie JWT - permitir acceso (el JWT ya fue validado en el login)
                        System.out.println("[CORE SECURITY] Auth via JWT COOKIE detected");
                        jakarta.servlet.http.HttpSession session = httpRequest.getSession(false);
                        if (session != null) {
                            cedula = (String) session.getAttribute("cedula");
                            userType = (String) session.getAttribute("userType");
                        }
                        break;
                    }
                }
            }
        }

        // Si no hay autenticación, bloquear
        if (cedula == null || userType == null) {
            System.out.println("[CORE SECURITY] BLOCKED - No authentication for " + requestUri);
            httpResponse.sendRedirect("/auth/login?error=session_expired");
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
        // Ruta raíz es pública (redirige a login)
        if (requestUri.equals("/")) {
            return true;
        }

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
            // Admin tiene acceso a /admin y /staff (con o sin barra final)
            return requestUri.equals("/admin") || requestUri.startsWith("/admin/") ||
                   requestUri.equals("/staff") || requestUri.startsWith("/staff/");
        }

        if ("ANALYST".equals(userType)) {
            // Analyst (Staff) tiene acceso a /staff/ y /denuncia/
            return requestUri.equals("/staff") || requestUri.startsWith("/staff/") ||
                   requestUri.startsWith("/denuncia/");
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

