package com.vozsegura.vozsegura.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para validar tokens de Cloudflare Turnstile (CAPTCHA moderno).
 * 
 * Responsabilidades:
 * - Validar tokens de Turnstile contra servidores Cloudflare
 * - Proteger formularios públicos contra bots/ataques
 * - Validar IP del cliente (si disponible)
 * - Manejar fallos sin exponer detalles técnicos
 * 
 * Ventajas de Turnstile vs CAPTCHA tradicional:
 * - Desafío adaptativo (puede ser invisible si confianza alta)
 * - Mejor UX (usuario no hace click en cada imagen)
 * - Protección contra bypass (machine learning)
 * - Mejor accesibilidad (audio, etc.)
 * - Rate limiting nativo
 * 
 * Flujo en frontend:
 * 1. Inicializar: turnstile.render('#captcha-container', {sitekey: '...'})
 * 2. Usuario resuelve desafío
 * 3. Turnstile genera token
 * 4. Formulario envía token en field: 'cf-turnstile-response'
 * 
 * Flujo en backend:
 * 1. Recibir token del formulario
 * 2. Llamar: verifyTurnstileToken(token, userIp)
 * 3. Validar contra Cloudflare API
 * 4. Si ok: proceder con formulario
 * 5. Si fallo: rechazar y pedir reintentar
 * 
 * Configuración:
 * - cloudflare.turnstile.site-key: Clave pública (exponerla en HTML es OK)
 * - cloudflare.turnstile.secret-key: Clave privada (solo en backend, NUNCA frontend)
 * 
 * Endpoint de validación:
 * - URL: https://challenges.cloudflare.com/turnstile/v0/siteverify
 * - Método: POST
 * - Timeout: 10 segundos
 * - Rate limit: 50 requests/IP/segundo
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Service
public class CloudflareTurnstileService {

    @Value("${cloudflare.turnstile.site-key}")
    private String siteKey;

    @Value("${cloudflare.turnstile.secret-key}")
    private String secretKey;

    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final RestTemplate restTemplate;

    public CloudflareTurnstileService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Obtiene la site key pública (seguro exponerla en frontend)
     */
    public String getSiteKey() {
        return siteKey;
    }

    /**
     * Valida el token de Turnstile contra servidores de Cloudflare
     * 
     * @param token Token enviado desde el cliente
     * @param remoteIp IP del cliente (para validación adicional)
     * @return true si la validación fue exitosa
     */
    public boolean verifyTurnstileToken(String token, String remoteIp) {
        // Validaciones básicas
        if (token == null || token.isBlank()) {
            return false;
        }

        if (secretKey == null || secretKey.isBlank()) {
            return false;
        }

        try {
            // Preparar solicitud HTTP
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("secret", secretKey);
            requestBody.put("response", token);

            // Agregar IP remota si está disponible (validación adicional)
            if (remoteIp != null && !remoteIp.isBlank()) {
                requestBody.put("remoteip", remoteIp);
            }

            // Headers para la solicitud
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Realizar solicitud a Cloudflare
            TurnstileResponse response = restTemplate.postForObject(
                TURNSTILE_VERIFY_URL,
                requestEntity,
                TurnstileResponse.class
            );

            // Validar respuesta
            if (response == null) {
                return false;
            }

            return response.isSuccess();

        } catch (RestClientException e) {
            // En producción, fallar cerrado (seguro): retornar false
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * DTO para la respuesta de Cloudflare Turnstile.
     * 
     * Ejemplo de respuesta exitosa:
     * {
     *   "success": true,
     *   "challenge_ts": "2026-01-15T10:30:00Z",
     *   "hostname": "example.com",
     *   "error_codes": []
     * }
     */
    public static class TurnstileResponse {
        private boolean success;

        @JsonProperty("challenge_ts")
        private String challengeTs;

        private String hostname;

        @JsonProperty("error_codes")
        private String[] errorCodes;

        // Getters y Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getChallengeTs() {
            return challengeTs;
        }

        public void setChallengeTs(String challengeTs) {
            this.challengeTs = challengeTs;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String[] getErrorCodes() {
            return errorCodes;
        }

        public void setErrorCodes(String[] errorCodes) {
            this.errorCodes = errorCodes != null ? errorCodes : new String[]{};
        }
    }
}
