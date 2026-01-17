package com.vozsegura.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Collections;

/**
 * Filtro JWT para validación de tokens en el API Gateway
 * 
 * Responsabilidades:
 * - Validar presencia de token JWT en header Authorization
 * - Verificar firma del JWT usando clave secreta
 * - Validar expiración del token
 * - Extraer información del usuario y agregar a headers
 * - Denegar acceso si el token es inválido
 * 
 * @author Voz Segura Team
 */
@Slf4j
@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            log.info("[JWT FILTER] Processing request for path: {}", path);
            log.info("[JWT FILTER] Using JWT secret: {}...", jwtSecret != null ? jwtSecret.substring(0, Math.min(20, jwtSecret.length())) : "NULL");
            
            try {
                // 1. Extraer token del header Authorization o de cookies
                String authHeader = exchange.getRequest().getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

                // Si no está en Authorization header, buscar en cookies
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.info("[JWT FILTER] No JWT in Authorization header, checking cookies...");
                    String cookieHeader = exchange.getRequest().getHeaders()
                            .getFirst(HttpHeaders.COOKIE);
                    if (cookieHeader != null) {
                        String[] cookies = cookieHeader.split(";");
                        for (String cookie : cookies) {
                            if (cookie.trim().startsWith("Authorization=")) {
                                // La cookie solo tiene el token, sin "Bearer "
                                String tokenFromCookie = cookie.trim().substring("Authorization=".length());
                                authHeader = "Bearer " + tokenFromCookie;
                                log.info("[JWT FILTER] Token extracted from cookie");
                                break;
                            }
                        }
                    }
                }

                if (authHeader == null) {
                    log.warn("[JWT FILTER] Authorization header/cookie missing for path: {}",
                            exchange.getRequest().getPath());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                if (!authHeader.startsWith("Bearer ")) {
                    log.warn("[JWT FILTER] Invalid Authorization format");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                String token = authHeader.substring(7);

                // 2. Validar y parsear JWT
                byte[] keyBytes = jwtSecret.getBytes();
                Claims claims = Jwts.parser()
                        .verifyWith(new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256"))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // 3. Extraer información del usuario
                String cedula = claims.getSubject();
                String userType = claims.get("userType", String.class);
                String apiKey = claims.get("apiKey", String.class);

                if (cedula == null || userType == null || apiKey == null) {
                    log.warn("[JWT FILTER] Missing required claims in token");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                log.info("[JWT FILTER] Token validated for user: {} (type: {})",
                        cedula, userType);

                // 4. Agregar información de usuario a headers para el backend
                var mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Cedula", cedula)
                        .header("X-User-Type", userType)
                        .header("X-Api-Key", apiKey)
                        .header("X-Auth-Time", String.valueOf(claims.getIssuedAt().getTime()))
                        .build();

                var mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                // 5. Continuar con la petición
                return chain.filter(mutatedExchange);

            } catch (JwtException e) {
                log.error("[JWT FILTER] JWT validation failed: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            } catch (Exception e) {
                log.error("[JWT FILTER] Unexpected error: {}", e.getMessage(), e);
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * Configuración del filtro (vacía, puede extenderse)
     */
    public static class Config {
        // Configuración adicional si es necesaria
    }
}
