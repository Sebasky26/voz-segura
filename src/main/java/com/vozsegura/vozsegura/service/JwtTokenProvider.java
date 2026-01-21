package com.vozsegura.vozsegura.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generar y gestionar JWT (JSON Web Tokens).
 * 
 * Responsabilidades:
 * - Generar tokens JWT firmados con información del usuario
 * - Incluir claims: cedula (subject), userType, apiKey, scopes
 * - Aplicar firma HMAC-SHA256 con clave secreta compartida
 * - Configurar tiempo de expiración (24 horas por defecto)
 * 
 * Flujo de uso:
 * 1. Core app autentica usuario (cédula + MFA)
 * 2. Genera JWT con claims (cedula, userType, apiKey)
 * 3. Retorna JWT al cliente en cookie/header
 * 4. Cliente envía JWT en cada petición (Authorization header)
 * 5. Gateway valida firma del JWT
 * 6. Gateway inyecta headers X-User-Cedula, X-User-Type en peticiones internas
 * 
 * Seguridad:
 * - HMAC-SHA256 con clave de 32+ bytes (configurada en jwt.secret)
 * - Expiración: 24 horas por defecto (configurable)
 * - Algorithm HS256 (simétrico, clave compartida Gateway-Core)
 * - Claims incluyen userType (ADMIN, ANALYST) para RBAC en Gateway
 * 
 * Nota:
 * - El JWT es el token de SESSION (válido 24h)
 * - El OTP es transitorio (válido 5 min)
 * - La cookie de login es httpOnly, secure, sameSite=Strict
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Service
public class JwtTokenProvider {

    /** Clave secreta para firmar JWT (mínimo 32 bytes para HS256) */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /** Tiempo de expiración del token en milisegundos (24 horas = 86400000) */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @PostConstruct
    private void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.isEmpty() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "SECURITY ERROR: jwt.secret must be configured with at least 32 characters. " +
                "Generate with: openssl rand -base64 32"
            );
        }
        if (jwtExpiration <= 0) {
            throw new IllegalStateException(
                "SECURITY ERROR: jwt.expiration must be a positive number (milliseconds)"
            );
        }
    }

    /**
     * Genera JWT con información de usuario y API Key
     * 
     * @param cedula Identificación del usuario (DNI/RUC)
     * @param userType ADMIN, ANALYST, DENUNCIANTE
     * @param apiKey API Key de Supabase (anon-key o service-role-key)
     * @return Token JWT firmado
     */
    public String generateToken(String cedula, String userType, String apiKey) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", userType);
        claims.put("apiKey", apiKey);
        claims.put("iat", new Date());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(cedula)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    /**
     * Genera JWT con scopes específicos (RBAC)
     * 
     * @param cedula Identificación del usuario
     * @param userType Tipo de usuario
     * @param apiKey API Key de Supabase
     * @param scopes Permisos específicos (ej: "read:cases", "write:cases")
     * @return Token JWT firmado con scopes
     */
    public String generateTokenWithScopes(String cedula, String userType, 
                                         String apiKey, String... scopes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", userType);
        claims.put("apiKey", apiKey);
        claims.put("scopes", scopes);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(cedula)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    /**
     * Obtiene el tiempo de expiración del token en milisegundos
     */
    public long getExpirationTime() {
        return jwtExpiration;
    }
}
