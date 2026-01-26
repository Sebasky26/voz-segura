package com.vozsegura.config;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.vozsegura.service.AuditService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Handler personalizado para auditar el cierre de sesión.
 *
 * Registra en logs.evento_auditoria cada logout exitoso con:
 * - Usuario que cerró sesión
 * - Rol del usuario
 * - IP y User-Agent
 *
 * Seguridad: No registra datos sensibles, solo información técnica.
 */
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);

    private final AuditService auditService;

    public CustomLogoutSuccessHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        HttpSession session = request.getSession(false);

        String username = null;
        String role = null;
        Long staffId = null;

        // Intentar obtener datos de la sesión antes de invalidar
        if (session != null) {
            username = (String) session.getAttribute("username");
            role = (String) session.getAttribute("userType");
            staffId = (Long) session.getAttribute("staffUserId");
        }

        // Si no hay sesión, intentar obtener de Authentication
        if (username == null && authentication != null) {
            username = authentication.getName();
        }

        // Auditar logout
        try {
            auditService.logSecurityEvent(
                "LOGOUT",
                "SUCCESS",
                staffId,
                username != null ? username : "unknown",
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "role", role != null ? role : "unknown",
                    "session_invalidated", session != null ? "true" : "false"
                )
            );

        } catch (Exception e) {
            log.error("Error auditing logout", e);
        }

        // Redirigir a página de login
        response.sendRedirect(request.getContextPath() + "/verification/inicio?logout");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}
