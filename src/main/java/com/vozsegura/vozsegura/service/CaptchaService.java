package com.vozsegura.vozsegura.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para generar y validar CAPTCHAs únicos por sesión.
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

