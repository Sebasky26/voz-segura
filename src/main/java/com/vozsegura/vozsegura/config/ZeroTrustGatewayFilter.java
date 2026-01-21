package com.vozsegura.vozsegura.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Filtro Zero Trust para validar requests desde API Gateway.
 *
 * SEGURIDAD CRÍTICA:
 * - Valida firma HMAC-SHA256 del Gateway
 * - NO confía en headers X-User-* sin validación
 * - Implementa arquitectura Zero Trust
 * - Rechaza requests directos al Core (sin pasar por Gateway)
 *
 * Funcionamiento:
 * 1. Gateway valida JWT y genera HMAC signature
 * 2. Core valida que HMAC signature coincida
 * 3. Si no coincide → 403 Forbidden (bypass detectado)
 *
 * @author Voz Segura Team - Zero Trust Architecture
 * @since 2026-01
 */
@Slf4j
@Component
@Order(1)
public class ZeroTrustGatewayFilter implements Filter {

    @Value("${vozsegura.gateway.shared-secret}")
    private String sharedSecret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestUri = httpRequest.getRequestURI();

        // Rutas públicas (no requieren Gateway)
        if (isPublicRoute(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        // Extraer headers del Gateway
        String cedula = httpRequest.getHeader("X-User-Cedula");
        String userType = httpRequest.getHeader("X-User-Type");
        String gatewaySignature = httpRequest.getHeader("X-Gateway-Signature");
        String timestamp = httpRequest.getHeader("X-Request-Timestamp");

        // Validar que todos los headers están presentes
        if (cedula == null || userType == null || gatewaySignature == null || timestamp == null) {
            log.warn("⚠️ ZERO TRUST VIOLATION: Missing Gateway headers (uri={}, ip={})",
                    requestUri, httpRequest.getRemoteAddr());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                                  "Access denied: requests must come through API Gateway");
            return;
        }

        // Validar timestamp (no más de 5 minutos de antigüedad - previene replay attacks)
        try {
            long requestTime = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            long diff = Math.abs(now - requestTime);

            if (diff > 300000) { // 5 minutos
                log.warn("⚠️ ZERO TRUST VIOLATION: Timestamp too old ({}ms old, uri={}, user={})",
                        diff, requestUri, cedula);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                                      "Request timestamp expired");
                return;
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ ZERO TRUST VIOLATION: Invalid timestamp (uri={}, user={})",
                    requestUri, cedula);
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp");
            return;
        }

        // Generar firma esperada (mismo algoritmo que Gateway)
        String expectedSignature = generateHmacSignature(
            timestamp,
            httpRequest.getMethod(),
            requestUri,
            cedula,
            userType
        );

        // Comparar firmas (timing-attack safe)
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            gatewaySignature.getBytes(StandardCharsets.UTF_8)
        )) {
            log.error("🚨 ZERO TRUST VIOLATION: Invalid Gateway signature (uri={}, user={}, ip={})",
                    requestUri, cedula, httpRequest.getRemoteAddr());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                                  "Invalid gateway signature");
            return;
        }

        log.debug("✅ Zero Trust validated: {} {} (user: {}, type: {})",
                 httpRequest.getMethod(), requestUri, cedula, userType);

        // Request válido → continuar
        chain.doFilter(request, response);
    }

    /**
     * Genera firma HMAC-SHA256 (mismo algoritmo que Gateway).
     */
    private String generateHmacSignature(String timestamp, String method, String path,
                                        String cedula, String userType) {
        try {
            String message = String.join(":", timestamp, method, path, cedula, userType);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                sharedSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate HMAC signature", e);
        }
    }

    /**
     * Verifica si la ruta es pública (no requiere Gateway).
     */
    private boolean isPublicRoute(String uri) {
        return uri.startsWith("/auth/") ||
               uri.startsWith("/public/") ||
               uri.startsWith("/webhooks/") ||
               uri.startsWith("/actuator/health") ||
               uri.startsWith("/health/config") ||
               uri.startsWith("/css/") ||
               uri.startsWith("/js/") ||
               uri.startsWith("/img/") ||
               uri.startsWith("/images/") ||
               uri.startsWith("/error");
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("🛡️ Zero Trust Gateway Filter initialized");
        log.info("Protected routes will require valid Gateway signature");
    }

    @Override
    public void destroy() {
        log.info("🛡️ Zero Trust Gateway Filter destroyed");
    }
}
