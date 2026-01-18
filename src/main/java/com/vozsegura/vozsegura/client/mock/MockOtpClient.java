package com.vozsegura.vozsegura.client.mock;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vozsegura.vozsegura.client.OtpClient;

/**
 * Implementación Mock de OTP con protecciones de seguridad reales.
 * 
 * Incluye:
 * - Generación criptográficamente segura de códigos
 * - Expiración de tokens (5 minutos)
 * - Protección anti-brute force (máximo 3 intentos)
 * - Protección anti-replay (token de un solo uso)
 * 
 * En producción, reemplazar por integración con AWS SNS o Twilio.
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026
 */
@Component
@Profile({"dev", "default"})
public class MockOtpClient implements OtpClient {

    private final Map<String, TokenData> tokensActivos = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Políticas de seguridad
    private static final int MAX_INTENTOS = 3;
    private static final int MINUTOS_EXPIRACION = 5;

    /**
     * Datos internos del token con estado de seguridad.
     */
    private static class TokenData {
        final String codigo;
        final String destino;
        final LocalDateTime expiracion;
        int intentosFallidos;

        TokenData(String codigo, String destino) {
            this.codigo = codigo;
            this.destino = destino;
            this.expiracion = LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION);
            this.intentosFallidos = 0;
        }
        
        boolean estaExpirado() {
            return LocalDateTime.now().isAfter(expiracion);
        }
        
        boolean estaBloqueado() {
            return intentosFallidos >= MAX_INTENTOS;
        }
    }

    @Override
    public String sendOtp(String destination) {
        // Generar código de 6 dígitos criptográficamente seguro
        int numero = 100000 + secureRandom.nextInt(900000);
        String codigo = String.valueOf(numero);
        
        // Generar ID único para este OTP
        String otpId = UUID.randomUUID().toString();
        
        // Guardar token con su estado
        tokensActivos.put(otpId, new TokenData(codigo, destination));
        
        // En desarrollo: imprimir código para testing (SOLO en dev)
        // En producción esto se envía vía AWS SES/SNS
        if (Boolean.parseBoolean(System.getenv().getOrDefault("DEV_SHOW_OTP", "false"))) {
            System.out.println("[DEV] OTP Code: " + codigo);
        }

        return otpId;
    }

    /**
     * Enmascara el destino para los logs (seguridad).
     */
    private String maskDestination(String destination) {
        if (destination == null || destination.length() < 6) {
            return "***";
        }
        if (destination.contains("@")) {
            String[] parts = destination.split("@");
            String local = parts[0];
            return local.substring(0, Math.min(3, local.length())) + "***@" + parts[1];
        }
        return destination.substring(0, 3) + "****" + destination.substring(destination.length() - 2);
    }

    @Override
    public boolean verifyOtp(String otpId, String code) {
        TokenData token = tokensActivos.get(otpId);
        if (token == null) {
            return false;
        }
        
        if (token.estaExpirado()) {
            tokensActivos.remove(otpId);
            return false;
        }
        
        if (token.estaBloqueado()) {
            tokensActivos.remove(otpId);
            return false;
        }
        
        if (token.codigo.equals(code)) {
            tokensActivos.remove(otpId);
            return true;
        } else {
            token.intentosFallidos++;
            return false;
        }
    }
}
