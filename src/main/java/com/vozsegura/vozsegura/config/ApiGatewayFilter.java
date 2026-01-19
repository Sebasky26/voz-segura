package com.vozsegura.vozsegura.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
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
 * @author Voz Segura Team
 * @version 2.0 - 2026
 */
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
            String loginUrl = gatewayBaseUrl + "/auth/login?session_expired";
            httpResponse.sendRedirect(loginUrl);
            return;
        }

        // 3. Validar autorización
        if (!isAuthorized(requestUri, userType)) {
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

        if ("ADMIN".equals(userType)) {
            return requestUri.equals("/admin") || requestUri.startsWith("/admin/") ||
                   requestUri.equals("/staff") || requestUri.startsWith("/staff/");
        }

        if ("ANALYST".equals(userType)) {
            return requestUri.equals("/staff") || requestUri.startsWith("/staff/") ||
                   requestUri.startsWith("/denuncia/");
        }

        if ("DENUNCIANTE".equals(userType)) {
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

