package com.vozsegura.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.vozsegura.gateway.config.RouteSecurityConfig;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;

/**
 * Filtro JWT para validación de tokens en el API Gateway
 * 
 * Responsabilidades:
 * - Validar presencia de token JWT en header Authorization
 * - Verificar firma del JWT usando clave secreta
 * - Validar expiración del token
 * - Extraer información del usuario y agregar a headers
 * - Generar firma HMAC para Zero Trust con Core
 * - Validar que usuario tenga rol permitido para la ruta
 * - Denegar acceso si el token es inválido
 * 
 * Zero Trust Architecture:
 * - Headers firmados con HMAC-SHA256 hacia Core
 * - Timestamp con TTL de 60 segundos (anti-replay)
 * - Validación de rol según ruta accedida
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${vozsegura.gateway.shared-secret}")
    private String sharedSecret;

    @Autowired(required = false)
    private RouteSecurityConfig routeSecurityConfig;

    @PostConstruct
    private void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.isEmpty() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "SECURITY ERROR: jwt.secret must be configured with at least 32 characters. " +
                "Generate with: openssl rand -base64 32"
            );
        }
        if (sharedSecret == null || sharedSecret.isEmpty() || sharedSecret.length() < 32) {
            throw new IllegalStateException(
                "SECURITY ERROR: vozsegura.gateway.shared-secret must be configured. " +
                "Must be the SAME value in Gateway and Core for Zero Trust validation. " +
                "Generate with: openssl rand -base64 32"
            );
        }
        log.info("✓ Gateway JWT Filter initialized with valid security configuration");
    }

    public JwtAuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();
            
            log.debug("[Gateway] {} {} from {}", 
                method, path, exchange.getRequest().getRemoteAddress());
            
            try {
                // 1. Verificar si es ruta pública (sin autenticación)
                if (routeSecurityConfig != null && routeSecurityConfig.isPublicRoute(path)) {
                    log.debug("[Gateway] Public route: {} {}", method, path);
                    return chain.filter(exchange);
                }

                // 2. Extraer token del header Authorization o de cookies
                String authHeader = exchange.getRequest().getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

                // Si no está en Authorization header, buscar en cookies
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    String cookieHeader = exchange.getRequest().getHeaders()
                            .getFirst(HttpHeaders.COOKIE);
                    if (cookieHeader != null) {
                        String[] cookies = cookieHeader.split(";");
                        for (String cookie : cookies) {
                            if (cookie.trim().startsWith("Authorization=")) {
                                // La cookie solo tiene el token, sin "Bearer "
                                String tokenFromCookie = cookie.trim().substring("Authorization=".length());
                                authHeader = "Bearer " + tokenFromCookie;
                                break;
                            }
                        }
                    }
                }

                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("[Gateway] Missing or invalid JWT token for protected route: {} {}", 
                        method, path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String token = authHeader.substring(7);

                // 3. Validar y parsear JWT
                byte[] keyBytes = jwtSecret.getBytes();
                Claims claims = Jwts.parser()
                        .verifyWith(new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256"))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // 4. Extraer información del usuario
                String cedula = claims.getSubject();
                String userType = claims.get("userType", String.class);
                String apiKey = claims.get("apiKey", String.class);

                if (cedula == null || cedula.isBlank() || 
                    userType == null || userType.isBlank() || 
                    apiKey == null || apiKey.isBlank()) {
                    log.warn("[Gateway] Invalid JWT claims for route: {} {} - Missing cedula/userType/apiKey", 
                        method, path);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                // 5. Validar rol para la ruta específica
                if (routeSecurityConfig != null) {
                    Set<String> allowedRoles = routeSecurityConfig.getAllowedRoles(path);
                    if (!allowedRoles.isEmpty() && !allowedRoles.contains(userType)) {
                        log.warn("[Gateway] Access denied - Unauthorized role. User: {}, Type: {}, Route: {} {}", 
                            cedula, userType, method, path);
                        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                        return exchange.getResponse().setComplete();
                    }
                }

                // 6. Generar firma HMAC para Zero Trust (validación Core)
                String timestamp = String.valueOf(System.currentTimeMillis());
                String signature = generateHmacSignature(timestamp, method, path, cedula, userType);

                // 7. Agregar información de usuario a headers para el backend
                var mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Cedula", cedula)
                        .header("X-User-Type", userType)
                        .header("X-Api-Key", apiKey)
                        .header("X-Auth-Time", String.valueOf(claims.getIssuedAt().getTime()))
                        .header("X-Gateway-Signature", signature)
                        .header("X-Request-Timestamp", timestamp)
                        .build();

                var mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                log.debug("[Gateway] ✓ Authentication successful - User: {} ({}), Route: {} {}", 
                    cedula, userType, method, path);

                // 8. Continuar con la petición
                return chain.filter(mutatedExchange);

            } catch (JwtException e) {
                log.warn("[Gateway] JWT validation failed for route: {} {} - {}", 
                    method, path, e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            } catch (IllegalArgumentException e) {
                log.warn("[Gateway] Invalid JWT format for route: {} {} - {}", 
                    method, path, e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            } catch (Exception e) {
                log.error("[Gateway] Unexpected error processing request {} {} - {}", 
                    method, path, e.getMessage(), e);
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * Genera firma HMAC-SHA256 para validación Zero Trust en el Core.
     *
     * Formato del mensaje: "timestamp:method:path:cedula:userType"
     * 
     * Ejemplo:
     * - timestamp: "1673589234567"
     * - method: "GET"
     * - path: "/staff/casos"
     * - cedula: "1234567890"
     * - userType: "ANALYST"
     * - message: "1673589234567:GET:/staff/casos:1234567890:ANALYST"
     * 
     * La firma HMAC-SHA256 se calcula usando la clave compartida
     * y se retorna en Base64 para incluir en el header X-Gateway-Signature.
     *
     * @param timestamp Timestamp de la petición en milisegundos
     * @param method Método HTTP (GET, POST, PUT, DELETE, etc)
     * @param path Ruta de la petición (ej: /staff/casos)
     * @param cedula Cédula del usuario autenticado
     * @param userType Tipo de usuario (DENUNCIANTE, ANALYST, ADMIN)
     * @return Firma HMAC-SHA256 en Base64
     * 
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc2104">RFC 2104 - HMAC</a>
     */
    private String generateHmacSignature(String timestamp, String method, String path,
                                         String cedula, String userType) {
        try {
            // Construir mensaje a firmar (mismo formato que en Core)
            String message = String.join(":", timestamp, method, path, cedula, userType);

            // Generar HMAC-SHA256
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                sharedSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hmacBytes);
            
            log.debug("[Gateway] HMAC signature generated for {} {}", method, path);
            return signature;

        } catch (Exception e) {
            log.error("[Gateway] Error generating HMAC signature - {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * Configuración del filtro (puede extenderse con parámetros adicionales)
     */
    public static class Config {
        // Configuración adicional si es necesaria
    }
}
