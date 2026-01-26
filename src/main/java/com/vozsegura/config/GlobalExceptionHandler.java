package com.vozsegura.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.vozsegura.service.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Manejador global de excepciones.
 *
 * Captura errores no manejados y:
 * - Los audita en logs.evento_auditoria
 * - Muestra página de error amigable al usuario
 * - NO expone stack traces ni información sensible
 *
 * Seguridad: Solo registra información técnica, sin PII.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditService auditService;

    public GlobalExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Maneja errores 404 (página no encontrada).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ModelAndView handleNotFound(HttpServletRequest request, NoHandlerFoundException ex) {

        auditError(request, ex, "NOT_FOUND", 404);

        ModelAndView mav = new ModelAndView("error/generic-error");
        mav.addObject("error", "Página no encontrada");
        mav.addObject("statusCode", 404);
        mav.setStatus(HttpStatus.NOT_FOUND);

        return mav;
    }

    /**
     * Maneja todos los demás errores no capturados.
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericError(HttpServletRequest request, Exception ex) {

        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        auditError(request, ex, "SYSTEM_ERROR", 500);

        ModelAndView mav = new ModelAndView("error/generic-error");
        mav.addObject("error", "Ha ocurrido un error inesperado. Por favor intente nuevamente.");
        mav.addObject("statusCode", 500);
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);

        return mav;
    }

    /**
     * Audita el error en logs.evento_auditoria.
     */
    private void auditError(HttpServletRequest request, Exception ex, String eventType, int statusCode) {
        try {
            HttpSession session = request.getSession(false);

            String username = "anonymous";
            String role = "ANON";
            Long staffId = null;

            if (session != null) {
                username = (String) session.getAttribute("username");
                role = (String) session.getAttribute("userType");
                staffId = (Long) session.getAttribute("staffUserId");

                if (username == null) {
                    username = "anonymous";
                }
                if (role == null) {
                    role = "ANON";
                }
            }

            auditService.logSecurityEvent(
                eventType,
                "FAILURE",
                staffId,
                username,
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "path", request.getRequestURI(),
                    "method", request.getMethod(),
                    "status_code", statusCode,
                    "exception_type", ex.getClass().getSimpleName(),
                    "error_message", ex.getMessage() != null ? ex.getMessage().substring(0, Math.min(200, ex.getMessage().length())) : "null"
                )
            );
        } catch (Exception e) {
            log.error("Error auditing exception", e);
        }
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
