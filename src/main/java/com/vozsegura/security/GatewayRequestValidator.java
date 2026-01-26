package com.vozsegura.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Validador de autenticidad de peticiones del Gateway.
 *
 * Implementa Zero Trust: El Core NO confía en headers manipulables.
 *
 * Mecanismo:
 * 1. Gateway genera firma HMAC-SHA256 de la petición
 * 2. Firma incluye: timestamp + método + ruta + userCedula + userType
 * 3. Gateway envía firma en header: X-Gateway-Signature
 * 4. Core valida firma y timestamp (anti-replay)
 *
 * Headers del Gateway:
 * - X-User-Cedula: Cédula del usuario autenticado
 * - X-User-Type: ADMIN | ANALYST | DENUNCIANTE
 * - X-Api-Key: API Key de Supabase
 * - X-Auth-Time: Timestamp de autenticación JWT
 * - X-Gateway-Signature: HMAC-SHA256(timestamp:method:path:cedula:userType)
 * - X-Request-Timestamp: Timestamp de la petición (anti-replay)
 *
 * Seguridad:
 * - Clave compartida entre Gateway y Core (no en repo, en env)
 * - Timestamp: máximo 60 segundos de diferencia (anti-replay)
 * - Firma: imposible falsificar sin la clave
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Component
public class GatewayRequestValidator {

    @Value("${vozsegura.gateway.shared-secret}")
    private String sharedSecret;

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long MAX_TIMESTAMP_DIFF_MS = 60_000; // 60 segundos

    /**
     * Valida que una petición proviene realmente del Gateway.
     *
     * @param signature Firma recibida en X-Gateway-Signature
     * @param timestamp Timestamp en X-Request-Timestamp
     * @param method Método HTTP (GET, POST, etc)
     * @param path Ruta de la petición
     * @param userCedula Cédula del usuario (header X-User-Cedula)
     * @param userType Tipo de usuario (header X-User-Type)
     * @return true si válido, false si falsificado o expirado
     */
    public boolean validateRequest(String signature, String timestamp, String method,
                                   String path, String userCedula, String userType) {

        // Validar presencia de parámetros
        if (signature == null || timestamp == null || method == null ||
            path == null || userCedula == null || userType == null) {
            log.warn("❌ Validación fallida: Parámetros faltantes");
            return false;
        }

        // Anti-replay: validar timestamp
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long diff = Math.abs(currentTime - requestTime);

            if (diff > MAX_TIMESTAMP_DIFF_MS) {
                log.warn("❌ Validación fallida: Timestamp expirado (diff: {}ms)", diff);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("❌ Validación fallida: Timestamp inválido");
            return false;
        }

        // Generar firma esperada
        String expectedSignature = generateSignature(timestamp, method, path, userCedula, userType);

        // Comparación constante en tiempo (anti timing-attack)
        boolean valid = constantTimeEquals(signature, expectedSignature);

        if (!valid) {
            log.warn("❌ Validación fallida: Firma no coincide (Path: {}, User: {})",
                     path, maskCedula(userCedula));
        }

        return valid;
    }

    /**
     * Genera firma HMAC-SHA256 de una petición.
     *
     * @param timestamp Timestamp de la petición
     * @param method Método HTTP
     * @param path Ruta
     * @param userCedula Cédula
     * @param userType Tipo de usuario
     * @return Firma Base64
     */
    public String generateSignature(String timestamp, String method, String path,
                                    String userCedula, String userType) {
        try {
            // Construir mensaje a firmar
            String message = String.join(":", timestamp, method, path, userCedula, userType);

            // Generar HMAC
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                sharedSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
            );
            mac.init(keySpec);

            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generando firma HMAC", e);
        }
    }

    /**
     * Comparación de strings constante en tiempo (anti timing-attack).
     *
     * @param a String 1
     * @param b String 2
     * @return true si son iguales
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }

    /**
     * Enmascara cédula para logs (privacidad).
     *
     * @param cedula Cédula completa
     * @return Cédula enmascarada (ej: 172***7415)
     */
    private String maskCedula(String cedula) {
        if (cedula == null || cedula.length() < 4) {
            return "***";
        }
        return cedula.substring(0, 3) + "***" + cedula.substring(cedula.length() - 4);
    }
}
