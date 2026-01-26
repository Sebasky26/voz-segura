package com.vozsegura.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor para configurar tiempos de sesión diferenciados por rol.
 *
 * Implementa estándares de seguridad OWASP/NIST:
 * - ADMIN: 15 minutos (acceso crítico)
 * - ANALYST: 20 minutos (acceso a datos sensibles)
 * - CITIZEN: 30 minutos (usabilidad para formularios largos)
 *
 * El timeout se refresca automáticamente en cada request del usuario.
 */
@Component
public class SessionTimeoutConfig implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SessionTimeoutConfig.class);

    // Tiempos en SEGUNDOS (Spring usa segundos, no minutos)
    private static final int ADMIN_TIMEOUT = 15 * 60;    // 15 minutos - Máxima seguridad
    private static final int ANALYST_TIMEOUT = 20 * 60;  // 20 minutos - Seguridad alta
    private static final int CITIZEN_TIMEOUT = 30 * 60;  // 30 minutos - Usabilidad
    private static final int DEFAULT_TIMEOUT = 15 * 60;  // 15 minutos por defecto (más restrictivo)

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            // Obtener el tipo de usuario de la sesión (establecido en UnifiedAuthController)
            String userType = (String) session.getAttribute("userType");

            if (userType != null) {
                int timeout = getTimeoutForUserType(userType);
                session.setMaxInactiveInterval(timeout);

                // Log solo en debug (sin datos sensibles)
                if (log.isDebugEnabled()) {
                    log.debug("Session timeout set to {} seconds for user type: {}", timeout, userType);
                }
            }
        }

        return true;
    }

    /**
     * Retorna el timeout apropiado según el tipo de usuario.
     *
     * @param userType Tipo de usuario (ADMIN, ANALYST, USER)
     * @return Timeout en segundos
     */
    private int getTimeoutForUserType(String userType) {
        return switch (userType) {
            case "ADMIN" -> ADMIN_TIMEOUT;
            case "ANALYST" -> ANALYST_TIMEOUT;
            case "USER" -> CITIZEN_TIMEOUT;
            default -> {
                log.warn("Unknown user type: {}, using default timeout", userType);
                yield DEFAULT_TIMEOUT;
            }
        };
    }
}
