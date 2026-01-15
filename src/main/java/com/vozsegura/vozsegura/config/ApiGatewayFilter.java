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
import jakarta.servlet.http.HttpSession;

/**
 * API Gateway Filter - Implementación de Zero Trust Architecture (ZTA).
 *
 * Este filtro actúa como un API Gateway que:
 * 1. Intercepta TODAS las peticiones
 * 2. Valida que el usuario esté autenticado
 * 3. Verifica que tenga permisos para el recurso solicitado
 * 4. Enruta las peticiones según el rol del usuario
 * 5. Registra todos los accesos en auditoría
 *
 * Principios ZTA implementados:
 * - Never Trust, Always Verify
 * - Assume Breach
 * - Verify Explicitly
 * - Least Privilege Access
 * - Microsegmentation
 *
 * @author Voz Segura Team
 * @version 1.0 - 2026
 */
@Component
@Order(1)
public class ApiGatewayFilter implements Filter {

    private static final String[] PUBLIC_PATHS = {
        "/auth/",
        "/denuncia/",  // Denunciantes - flujo completo
        "/css/",
        "/js/",
        "/images/",
        "/favicon.ico",
        "/error",
        "/terms"
    };

    private static final String[] DENUNCIANTE_PATHS = {
        "/denuncia/"
    };

    private static final String[] STAFF_PATHS = {
        "/staff/"
    };

    private static final String[] ADMIN_PATHS = {
        "/admin/"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        String requestUri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Log de acceso (en producción enviar a SIEM)
        System.out.println("[API GATEWAY ZTA] " + method + " " + requestUri +
                          " | Session: " + (session != null ? session.getId() : "NO_SESSION") +
                          " | IP: " + httpRequest.getRemoteAddr());

        // 1. Permitir recursos públicos
        if (isPublicPath(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Verificar autenticación (Zero Trust: Never Trust, Always Verify)
        if (session == null || session.getAttribute("authenticated") == null) {
            System.out.println("[API GATEWAY ZTA] BLOCKED - No authenticated session");
            httpResponse.sendRedirect("/auth/login");
            return;
        }

        // 3. Obtener información del usuario
        String userType = (String) session.getAttribute("userType");
        String cedula = (String) session.getAttribute("cedula");
        String authMethod = (String) session.getAttribute("authMethod");

        if (userType == null || cedula == null) {
            System.out.println("[API GATEWAY ZTA] BLOCKED - Invalid session data");
            session.invalidate();
            httpResponse.sendRedirect("/auth/login");
            return;
        }

        // 4. Verificar autorización según el path solicitado (Least Privilege)
        if (!isAuthorized(requestUri, userType)) {
            System.out.println("[API GATEWAY ZTA] BLOCKED - Unauthorized access attempt by " +
                             userType + " to " + requestUri);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                "No tiene permisos para acceder a este recurso");
            return;
        }

        // 5. Verificar que Staff/Admin usaron autenticación ZTA completa (con o sin MFA)
        if (("ADMIN".equals(userType) || "ANALYST".equals(userType)) &&
            !("UNIFIED_ZTA".equals(authMethod) || "UNIFIED_ZTA_MFA".equals(authMethod))) {
            System.out.println("[API GATEWAY ZTA] BLOCKED - Staff/Admin must use ZTA auth");
            session.invalidate();
            httpResponse.sendRedirect("/auth/login");
            return;
        }

        // 6. Agregar headers de seguridad (Defense in Depth)
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        httpResponse.setHeader("Content-Security-Policy", "default-src 'self'");

        // 7. Petición autorizada - continuar
        System.out.println("[API GATEWAY ZTA] ALLOWED - " + userType + " accessing " + requestUri);
        chain.doFilter(request, response);
    }

    /**
     * Verifica si la ruta es pública (no requiere autenticación).
     */
    private boolean isPublicPath(String requestUri) {
        // Permitir exactamente favicon.ico
        if (requestUri.equals("/favicon.ico")) {
            return true;
        }

        // Verificar si comienza con algún path público
        for (String publicPath : PUBLIC_PATHS) {
            if (requestUri.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si el usuario tiene autorización para acceder al recurso.
     * Implementación de Control de Acceso Basado en Roles (RBAC).
     */
    private boolean isAuthorized(String requestUri, String userType) {
        switch (userType) {
            case "ADMIN":
                // Admin tiene acceso a todo
                return true;

            case "ANALYST":
                // Analyst tiene acceso a Staff pero no a Admin
                if (matchesPath(requestUri, ADMIN_PATHS)) {
                    return false;
                }
                return matchesPath(requestUri, STAFF_PATHS) ||
                       matchesPath(requestUri, DENUNCIANTE_PATHS);

            case "DENUNCIANTE":
                // Denunciante solo tiene acceso a /denuncia/
                return matchesPath(requestUri, DENUNCIANTE_PATHS);

            default:
                return false;
        }
    }

    /**
     * Verifica si el URI coincide con alguno de los paths permitidos.
     */
    private boolean matchesPath(String requestUri, String[] paths) {
        for (String path : paths) {
            if (requestUri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("===========================================");
        System.out.println(" API GATEWAY ZTA INITIALIZED - 2026");
        System.out.println(" Zero Trust Architecture Active");
        System.out.println(" All requests will be verified");
        System.out.println("===========================================");
    }

    @Override
    public void destroy() {
        System.out.println("[API GATEWAY ZTA] Shutting down...");
    }
}

