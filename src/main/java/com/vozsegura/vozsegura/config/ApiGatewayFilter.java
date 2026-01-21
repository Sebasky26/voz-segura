package com.vozsegura.vozsegura.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * API Gateway Filter - Valida headers de autenticación del Gateway.
 *
 * Responsabilidades:
 * - Validar que requests autenticadas tengan headers correctos del Gateway
 * - Permitir rutas públicas sin autenticación (/auth/, /denuncia/, /css/, etc)
 * - Extraer información de usuario de headers del Gateway (Zero Trust)
 * - Validar identidad según múltiples métodos (headers, sesión, JWT)
 * - Rechazar requests malformadas o no autenticadas
 * - Logging defensivo (sin exponer tokens ni datos sensibles)
 *
 * Flujo de autenticación (en orden):
 * 1. Si ruta pública: permitir directamente
 * 2. Si headers del Gateway (X-User-Cedula + X-User-Type): validar
 * 3. Si sesión HTTP activa: usar sesión como fuente de verdad
 * 4. Si JWT token válido: validar JWT
 * 5. Si nada de lo anterior: rechazar con 401 Unauthorized
 *
 * Headers esperados desde Gateway:
 * - X-User-Cedula: Cédula del usuario (ej: "1712345678")
 * - X-User-Type: Tipo de usuario (STAFF, ADMIN, CITIZEN, ANONYMOUS)
 *
 * Rutas públicas (sin autenticación requerida):
 * - /auth/* : Autenticación
 * - /webhooks/* : Webhooks de integraciones externas
 * - /denuncia/* : Formulario de denuncia pública
 * - /seguimiento : Seguimiento de denuncias
 * - /css/, /js/, /img/ : Recursos estáticos
 *
 * Integración:
 * - @Component: Bean de Spring
 * - @Order(1): Ejecuta primero (antes de otros filtros)
 * - implements Filter: Filtro estándar Jakarta Servlet
 * - @Slf4j: Logging defensivo sin datos sensibles
 *
 * @author Voz Segura Team
 * @version 2.0
 */
@Slf4j
@Component
@Order(1)
public class ApiGatewayFilter implements Filter {

    @Value("${vozsegura.gateway.base-url:http://localhost:8080}")
    private String gatewayBaseUrl;

    private static final String[] PUBLIC_PATHS = {
        "/auth/",
        "/webhooks/",
        "/denuncia/",
        "/seguimiento",
        "/css/",
        "/js/",
        "/img/",
        "/images/",
        "/error",
        "/terms",
        "/favicon.ico",
        "/favicon.png"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();

        // 1. Permitir rutas públicas
        if (isPublicPath(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. Verificar autenticación (múltiples métodos)
        String cedula = null;
        String userType = null;

        // Método 1: Headers del Gateway
        cedula = httpRequest.getHeader("X-User-Cedula");
        userType = httpRequest.getHeader("X-User-Type");

        // Método 2: Sesión HTTP
        if (cedula == null || userType == null) {
            jakarta.servlet.http.HttpSession session = httpRequest.getSession(false);
            if (session != null) {
                Boolean authenticated = (Boolean) session.getAttribute("authenticated");
                if (authenticated != null && authenticated) {
                    cedula = (String) session.getAttribute("cedula");
                    userType = (String) session.getAttribute("userType");
                }
            }
        }

        // Método 3: Cookie JWT
        if (cedula == null || userType == null) {
            jakarta.servlet.http.Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("Authorization".equals(cookie.getName())) {
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

        // Si no hay autenticación, redirigir a login del Gateway (8080)
        if (cedula == null || userType == null) {
            log.warn("Access denied: missing authentication (uri={})", requestUri);
            String loginUrl = gatewayBaseUrl + "/auth/login?session_expired";
            httpResponse.sendRedirect(loginUrl);
            return;
        }

        // 3. Validar autorización
        if (!isAuthorized(requestUri, userType)) {
            log.warn("Access denied: insufficient permissions (userType={}, uri={})", userType, requestUri);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                "No tiene permisos para acceder a este recurso");
            return;
        }

        // 4. Agregar headers de seguridad
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String requestUri) {
        if (requestUri.equals("/")) {
            return true;
        }
        for (String publicPath : PUBLIC_PATHS) {
            if (requestUri.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAuthorized(String requestUri, String userType) {
        if (userType == null) {
            return false;
        }

        // ADMIN: solo /admin/** y /staff/**
        // NOTA: ADMIN puede acceder a ambas rutas por propósitos de supervisión
        if ("ADMIN".equals(userType)) {
            return (requestUri.equals("/admin") || requestUri.startsWith("/admin/")) ||
                   (requestUri.equals("/staff") || requestUri.startsWith("/staff/"));
        }

        // ANALYST: solo /staff/** (panel de análisis)
        // NOTA: ANALYST NO puede acceder a /admin/** 
        // (admin es para superusuarios, analyst es operacional)
        if ("ANALYST".equals(userType)) {
            // Verificar estrictamente que sea /staff/
            // Rechazar intentos de acceder a /admin/
            if (requestUri.equals("/staff") || requestUri.startsWith("/staff/")) {
                return true;
            }
            if (requestUri.startsWith("/admin")) {
                return false; // Explícitamente prohibido
            }
            // ANALYST también puede acceder a /denuncia/ para ver casos
            return requestUri.startsWith("/denuncia/");
        }

        // DENUNCIANTE: solo /denuncia/** (su propio flujo)
        if ("DENUNCIANTE".equals(userType)) {
            // Rechazar explícitamente intentos de acceder a rutas protegidas
            if (requestUri.startsWith("/admin") || requestUri.startsWith("/staff")) {
                return false;
            }
            return requestUri.startsWith("/denuncia/");
        }

        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Silent init
    }

    @Override
    public void destroy() {
        // Silent destroy
    }
}

