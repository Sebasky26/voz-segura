package com.vozsegura.vozsegura.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para generar JWT (JSON Web Tokens)
 * 
 * Responsabilidades:
 * - Generar tokens JWT con información del usuario
 * - Incluir claims: cedula, userType, apiKey, scopes
 * - Aplicar firma con clave secreta compartida
 * - Configurar tiempo de expiración
 * 
 * Este token será validado por el API Gateway (puerto 8080)
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026
 */
@Service
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

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
