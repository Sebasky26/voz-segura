package com.vozsegura.vozsegura.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Arrays;
import java.util.List;

/**
 * Interceptor para validar sesión en rutas protegidas.
 * 
 * Responsabilidades:
 * - Verificar que la sesión sigue siendo válida (no expirada)
 * - Permitir acceso a rutas públicas sin sesión
 * - Redirigir a login si sesión expiró en ruta protegida
 * - Mostrar mensaje de "sesión expirada"
 * 
 * Rutas Públicas (sin requerir sesión):
 * - /auth/** (login, logout, verificación)
 * - /api/webhooks/** (webhooks externos)
 * - /verification/** (DIDIT verification)
 * - /seguimiento** (tracking anónimo - pero requiere autenticación)
 * 
 * Rutas Protegidas (requieren sesión válida):
 * - /denuncia/form (crear denuncia)
 * - /denuncia/submit (enviar denuncia)
 * - /staff/** (analistas)
 * - /admin/** (administradores)
 * 
 * Flujo de Sesión:
 * 1. Usuario se autentica → session.authenticated = true
 * 2. Usuario crea denuncia → session.authenticated = true
 * 3. Usuario envía denuncia exitosamente → session.invalidate()
 * 4. Usuario intenta acceder a /denuncia/form → Redirige a login con mensaje
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Component
public class SessionValidationInterceptor implements HandlerInterceptor {

    /**
     * Rutas públicas que NO requieren sesión autenticada
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/auth/",
        "/api/webhooks/",
        "/verification/",
        "/",
        "/css/",
        "/js/",
        "/img/",
        "/error"
    );

    /**
     * Rutas que requieren sesión autenticada explícitamente
     */
    private static final List<String> PROTECTED_PATHS = Arrays.asList(
        "/denuncia/form",
        "/denuncia/submit",
        "/denuncia/additional-info",
        "/seguimiento",
        "/staff/",
        "/admin/"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        
        String requestPath = request.getRequestURI();
        
        // Ignorar rutas públicas
        if (isPublicPath(requestPath)) {
            return true;
        }
        
        // Validar sesión para rutas protegidas
        if (isProtectedPath(requestPath)) {
            HttpSession session = request.getSession(false); // No crear nueva sesión
            
            // Si no hay sesión o no está autenticada
            if (session == null || !isSessionValid(session)) {
                // ✅ Sesión expirada o no válida - redirigir a login
                response.sendRedirect(request.getContextPath() + "/auth/login?session_expired=true");
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                          ModelAndView modelAndView) throws Exception {
        // No action needed
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                               Exception ex) throws Exception {
        // No action needed
    }

    /**
     * Verifica si la ruta es pública (no requiere sesión).
     * 
     * @param path ruta del request
     * @return true si es pública
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Verifica si la ruta es protegida (requiere sesión autenticada).
     * 
     * @param path ruta del request
     * @return true si es protegida
     */
    private boolean isProtectedPath(String path) {
        return PROTECTED_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Valida que la sesión sea válida y esté autenticada.
     * 
     * Criterios:
     * - session != null
     * - session.authenticated == true
     * - session tiene datos de autenticación (citizenHash o cedula)
     * 
     * @param session sesión HTTP
     * @return true si sesión es válida
     */
    private boolean isSessionValid(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        
        if (authenticated == null || !authenticated) {
            return false;
        }
        
        // Verificar que hay datos de autenticación
        String citizenHash = (String) session.getAttribute("citizenHash");
        String cedula = (String) session.getAttribute("cedula");
        
        // Al menos uno debe existir
        return citizenHash != null || cedula != null;
    }
}
