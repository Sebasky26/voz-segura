package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.OtpClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        
        // Simulación de envío (en producción: AWS SNS, Twilio, etc.)
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  [OTP MOCK] Código enviado a: " + destination);
        System.out.println("║  Código: " + codigo);
        System.out.println("║  ID: " + otpId.substring(0, 8) + "...");
        System.out.println("║  Expira en: " + MINUTOS_EXPIRACION + " minutos");
        System.out.println("╚══════════════════════════════════════════╝");
        
        return otpId;
    }

    @Override
    public boolean verifyOtp(String otpId, String code) {
        // Validar que existe el token
        TokenData token = tokensActivos.get(otpId);
        if (token == null) {
            System.out.println("[OTP] ALERTA: Intento con ID inexistente: " + otpId);
            return false;
        }
        
        // Verificar expiración
        if (token.estaExpirado()) {
            System.out.println("[OTP] Token expirado para: " + token.destino);
            tokensActivos.remove(otpId);
            return false;
        }
        
        // Verificar bloqueo por intentos fallidos
        if (token.estaBloqueado()) {
            System.out.println("[OTP] BLOQUEO: Demasiados intentos fallidos para: " + token.destino);
            tokensActivos.remove(otpId);
            return false;
        }
        
        // Verificar código
        if (token.codigo.equals(code)) {
            // Éxito - eliminar token (anti-replay)
            tokensActivos.remove(otpId);
            System.out.println("[OTP] Verificación exitosa para: " + token.destino);
            return true;
        } else {
            // Fallo - incrementar contador
            token.intentosFallidos++;
            System.out.println("[OTP] Código incorrecto. Intento " + 
                             token.intentosFallidos + "/" + MAX_INTENTOS);
            return false;
        }
    }
}
