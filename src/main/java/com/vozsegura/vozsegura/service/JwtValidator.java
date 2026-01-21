package com.vozsegura.vozsegura.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Servicio de validación JWT defensivo.
 * 
 * Responsabilidades:
 * - Validar firma JWT (HMAC-SHA256)
 * - Verificar expiración
 * - Extraer claims (cédula, userType, apiKey)
 * - Logging seguro de fallos (sin exponer tokens ni datos sensibles)
 * - Rechazar tokens inválidos, manipulados o expirados
 * 
 * Seguridad:
 * - Zero Trust: Valida CADA token
 * - Logs seguros: NO registra tokens completos
 * - Mensajes genéricos al cliente (no revela detalles)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Service
public class JwtValidator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Valida un JWT y extrae sus claims.
     * 
     * @param token Token JWT a validar
     * @return Optional con Claims si válido, vacío si es inválido/expirado
     */
    public Optional<Claims> validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.debug("JWT validation failed: token is null or empty");
            return Optional.empty();
        }

        try {
            // Validar firma y expiración (JJWT 0.12.x)
            Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            log.debug("JWT validation successful for subject: {}", claims.getSubject());
            return Optional.of(claims);

        } catch (ExpiredJwtException e) {
            log.warn("JWT validation failed: token expired (subject: {})", 
                e.getClaims() != null ? e.getClaims().getSubject() : "unknown");
            return Optional.empty();

        } catch (SignatureException e) {
            log.warn("JWT validation failed: invalid signature");
            return Optional.empty();

        } catch (MalformedJwtException e) {
            log.warn("JWT validation failed: malformed token");
            return Optional.empty();

        } catch (UnsupportedJwtException e) {
            log.warn("JWT validation failed: unsupported JWT");
            return Optional.empty();

        } catch (IllegalArgumentException e) {
            log.warn("JWT validation failed: invalid token structure");
            return Optional.empty();

        } catch (Exception e) {
            log.warn("JWT validation failed: unexpected error");
            return Optional.empty();
        }
    }

    /**
     * Extrae la cédula (subject) del JWT.
     * 
     * @param token Token JWT
     * @return Optional con cédula si válido
     */
    public Optional<String> extractCedula(String token) {
        return validateToken(token)
            .map(Claims::getSubject);
    }

    /**
     * Extrae el tipo de usuario del JWT.
     * 
     * @param token Token JWT
     * @return Optional con userType si válido
     */
    public Optional<String> extractUserType(String token) {
        return validateToken(token)
            .map(claims -> claims.get("userType", String.class));
    }

    /**
     * Extrae la API Key del JWT.
     * 
     * @param token Token JWT
     * @return Optional con apiKey si válido
     */
    public Optional<String> extractApiKey(String token) {
        return validateToken(token)
            .map(claims -> claims.get("apiKey", String.class));
    }

    /**
     * Valida que el token sea de un tipo de usuario específico.
     * 
     * @param token Token JWT
     * @param expectedUserType Tipo esperado (ej: "ADMIN", "ANALYST", "DENUNCIANTE")
     * @return true si válido y coincide el tipo
     */
    public boolean isUserType(String token, String expectedUserType) {
        return extractUserType(token)
            .map(userType -> userType.equals(expectedUserType))
            .orElse(false);
    }

    /**
     * Valida que el token contenga un scopo específico.
     * 
     * @param token Token JWT
     * @param requiredScope Scopo requerido (ej: "read:cases")
     * @return true si el token tiene el scopo
     */
    public boolean hasScope(String token, String requiredScope) {
        Optional<Claims> claimsOpt = validateToken(token);
        if (claimsOpt.isEmpty()) {
            return false;
        }

        Claims claims = claimsOpt.get();
        String[] scopes = claims.get("scopes", String[].class);
        
        if (scopes == null) {
            return false;
        }

        for (String scope : scopes) {
            if (scope.equals(requiredScope)) {
                return true;
            }
        }
        return false;
    }
}
