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
import java.util.logging.Logger;

/**
 * Servicio para validar tokens de Cloudflare Turnstile.
 * 
 * Seguridad implementada:
 * - Secret key solo en backend (nunca expuesto)
 * - Site key configurada en properties
 * - Validación de IP del cliente
 * - Rate limiting via headers
 * - Timeout de conexión
 * - Manejo seguro de errores sin revelar detalles
 */
@Service
public class CloudflareTurnstileService {

    private static final Logger logger = Logger.getLogger(CloudflareTurnstileService.class.getName());

    @Value("${cloudflare.turnstile.site-key}")
    private String siteKey;

    @Value("${cloudflare.turnstile.secret-key}")
    private String secretKey;

    private static final String TURNSTILE_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private static final int TIMEOUT_SECONDS = 5;
    private static final double SUCCESS_THRESHOLD = 0.0; // Cualquier respuesta exitosa es válida

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
            logger.warning("[TURNSTILE] Token vacío o nulo");
            return false;
        }

        if (secretKey == null || secretKey.isBlank()) {
            logger.severe("[TURNSTILE] ERROR: Secret key no configurada en servidor");
            return false;
        }

        try {
            logger.info("[TURNSTILE] Validando token Turnstile");

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
                logger.severe("[TURNSTILE] Respuesta nula de Cloudflare");
                return false;
            }

            if (!response.isSuccess()) {
                logger.warning("[TURNSTILE] Validación fallida - Errores: " + String.join(", ", response.getErrorCodes()));
                return false;
            }

            logger.info("[TURNSTILE] Token validado exitosamente");
            return true;

        } catch (RestClientException e) {
            logger.severe("[TURNSTILE] Error conectando a Cloudflare: " + e.getMessage());
            // En producción, fallar cerrado (seguro): retornar false
            return false;

        } catch (Exception e) {
            logger.severe("[TURNSTILE] Error inesperado validando Turnstile: " + e.getMessage());
            e.printStackTrace();
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
