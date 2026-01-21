package com.vozsegura.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

/**
 * Filtro JWT para validación de tokens en el API Gateway
 * 
 * Responsabilidades:
 * - Validar presencia de token JWT en header Authorization
 * - Verificar firma del JWT usando clave secreta
 * - Validar expiración del token
 * - Extraer información del usuario y agregar a headers
 * - Generar firma HMAC para Zero Trust con Core
 * - Denegar acceso si el token es inválido
 * 
 * @author Voz Segura Team
 */
@Component
public class JwtAuthenticationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${vozsegura.gateway.shared-secret}")
    private String sharedSecret;

    @PostConstruct
    private void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.isEmpty() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "SECURITY ERROR: jwt.secret must be configured with at least 32 characters"
            );
        }
        if (sharedSecret == null || sharedSecret.isEmpty() || sharedSecret.length() < 32) {
            throw new IllegalStateException(
                "SECURITY ERROR: vozsegura.gateway.shared-secret must be configured. " +
                "Must be the SAME value in Gateway and Core for Zero Trust validation. " +
                "Generate with: openssl rand -base64 32"
            );
        }
    }

    public JwtAuthenticationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            
            try {
                // 1. Extraer token del header Authorization o de cookies
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

                if (authHeader == null) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                if (!authHeader.startsWith("Bearer ")) {
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
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }

                // 4. Generar firma HMAC para Zero Trust (validación Core)
                String timestamp = String.valueOf(System.currentTimeMillis());
                String method = exchange.getRequest().getMethod().name();
                String requestPath = exchange.getRequest().getPath().value();
                String signature = generateHmacSignature(timestamp, method, requestPath, cedula, userType);

                // 5. Agregar información de usuario a headers para el backend
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

                // 6. Continuar con la petición
                return chain.filter(mutatedExchange);

            } catch (JwtException e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }
        };
    }

    /**
     * Genera firma HMAC-SHA256 para validación Zero Trust en el Core.
     *
     * @param timestamp Timestamp de la petición
     * @param method Método HTTP (GET, POST, etc)
     * @param path Ruta de la petición
     * @param cedula Cédula del usuario
     * @param userType Tipo de usuario
     * @return Firma Base64
     */
    private String generateHmacSignature(String timestamp, String method, String path,
                                         String cedula, String userType) {
        try {
            // La validación de shared-secret ya se hace en @PostConstruct
            // Si llegamos aquí, el secret está configurado

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
            return Base64.getEncoder().encodeToString(hmacBytes);

        } catch (Exception e) {
            // En caso de error, retornar vacío (no bloquear el flujo)
            return "";
        }
    }

    /**
     * Configuración del filtro (vacía, puede extenderse)
     */
    public static class Config {
        // Configuración adicional si es necesaria
    }
}
