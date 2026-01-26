package com.vozsegura.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para generar y validar CAPTCHAs de texto.
 * 
 * Propósito:
 * - Protección anti-bot en formularios públicos
 * - De un solo uso (se borra tras validación exitosa)
 * - Código de 6 caracteres alfanuméricos (sin ambigüedad: no I/1/L, no O/0)
 * 
 * IMPORTANTE:
 * - Este es el CAPTCHA alternativo (fallback)
 * - Primario es Cloudflare Turnstile (mucho más seguro)
 * - Este se usa solo si Turnstile falla o no disponible
 * 
 * Almacenamiento:
 * - En DEV: ConcurrentHashMap en memoria (no persistente)
 * - En PROD: Usar Redis o Memcached (distribuido)
 * - TTL: No implementado (los CAPTCHAs viejos nunca expiran)
 * 
 * Limitaciones:
 * - NO está distribuido (si app tiene múltiples instancias, caos)
 * - NO tiene límite de tiempo (usuario puede guardar código eternamente)
 * - Mejor usar CloudflareTurnstileService en producción
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * @see CloudflareTurnstileService - Alternativa más segura (Cloudflare)
 */
@Service
public class CaptchaService {

    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    // Almacén temporal de captchas por sesión (en producción usar Redis)
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();

    /**
     * Genera un nuevo CAPTCHA para una sesión.
     * @param sessionId ID de sesión
     * @return Código CAPTCHA generado
     */
    public String generateCaptcha(String sessionId) {
        StringBuilder captcha = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            captcha.append(CAPTCHA_CHARS.charAt(RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        String captchaCode = captcha.toString();
        captchaStore.put(sessionId, captchaCode);
        return captchaCode;
    }

    /**
     * Valida un CAPTCHA contra el almacenado.
     * @param sessionId ID de sesión
     * @param userInput Entrada del usuario
     * @return true si es válido
     */
    public boolean validateCaptcha(String sessionId, String userInput) {
        String storedCaptcha = captchaStore.get(sessionId);
        if (storedCaptcha == null || userInput == null) {
            return false;
        }
        boolean isValid = storedCaptcha.equalsIgnoreCase(userInput.trim());
        if (isValid) {
            captchaStore.remove(sessionId); // Usar una sola vez
        }
        return isValid;
    }

    /**
     * Limpia el CAPTCHA de una sesión.
     * @param sessionId ID de sesión
     */
    public void clearCaptcha(String sessionId) {
        captchaStore.remove(sessionId);
    }
}

