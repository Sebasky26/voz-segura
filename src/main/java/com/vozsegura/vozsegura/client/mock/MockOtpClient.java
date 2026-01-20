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
 * Propósito:
 * - Permitir desarrollo y testing sin depender de Twilio/AWS SNS
 * - Simular comportamiento de servicio OTP real
 * - Incluir todas las protecciones de seguridad (anti-brute-force, expiración, anti-replay)
 * 
 * Diferencias con producción:
 * - NO envía SMS/Email real (solo simula)
 * - NO integra con Twilio/AWS SNS
 * - Códigos de prueba accesibles en logs de test (seguro en dev)
 * 
 * Protecciones implementadas (reales):
 * - Generación criptográficamente segura de códigos (SecureRandom)
 * - Expiración de tokens (5 minutos)
 * - Protección anti-brute-force (máximo 3 intentos fallidos)
 * - Protección anti-replay (token de un solo uso)
 * - Limpieza automática de tokens expirados
 * 
 * Flujo de uso:
 * 
 * 1. Cliente solicit OTP:
 *    otpId = mockOtp.sendOtp("+593987654321")
 *    → Retorna ID único (ej: "uuid-xxxx-yyyy")
 *    → Código guardado en memoria (puede verse en debugger)
 * 
 * 2. Usuario verifica código:
 *    resultado = mockOtp.verifyOtp(otpId, "123456")
 *    → true si código correcto
 *    → false si código incorrecto, expirado, o bloqueado
 * 
 * Seguridad en desarrollo:
 * - Usar println/logger para mostrar código durante testing
 * - NUNCA hacer esto en producción (imprimir códigos)
 * - En producción, códigos van solo a Twilio/AWS SNS
 * 
 * Configuración:
 * - Activo en profiles: "dev", "default" (solo desarrollo)
 * - En producción: reemplazar por OtpClientImpl (Twilio/AWS SNS)
 * - Spring inyecta esta clase si profile es dev
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026
 */
@Component
@Profile({"dev", "default"})
public class MockOtpClient implements OtpClient {

    /** Map concurrente: otpId → TokenData (thread-safe para concurrencia) */
    private final Map<String, TokenData> tokensActivos = new ConcurrentHashMap<>();
    
    /** Generador criptográficamente seguro de números aleatorios */
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Políticas de seguridad (constantes)
    private static final int MAX_INTENTOS = 3;
    private static final int MINUTOS_EXPIRACION = 5;

    /**
     * Datos internos del token OTP con metadatos de seguridad.
     */
    private static class TokenData {
        /** Código OTP de 6 dígitos (ej: "123456") */
        final String codigo;
        
        /** Destino del envío (teléfono o email) */
        final String destino;
        
        /** Timestamp de expiración (createdAt + 5 minutos) */
        final LocalDateTime expiracion;
        
        /** Contador de intentos fallidos (para protección anti-brute-force) */
        int intentosFallidos;

        /**
         * Constructor: crea nuevo token con expiración.
         * @param codigo Código OTP de 6 dígitos
         * @param destino Número telefónico o email
         */
        TokenData(String codigo, String destino) {
            this.codigo = codigo;
            this.destino = destino;
            this.expiracion = LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION);
            this.intentosFallidos = 0;
        }
        
        /**
         * Verifica si el token ha expirado (actual > expiration).
         * @return true si ha pasado el tiempo de expiración
         */
        boolean estaExpirado() {
            return LocalDateTime.now().isAfter(expiracion);
        }
        
        /**
         * Verifica si el token está bloqueado por brute-force.
         * @return true si intentosFallidos >= 3
         */
        boolean estaBloqueado() {
            return intentosFallidos >= MAX_INTENTOS;
        }
    }

    /**
     * Envía un código OTP al destino (mock, no envía realmente).
     * 
     * Implementación:
     * 1. Generar código aleatorio de 6 dígitos (100000-999999)
     *    → Rango: no empieza en 0 (evita confusión "01234" vs "1234")
     *    → Usa SecureRandom (criptográficamente seguro)
     * 
     * 2. Generar otpId único (UUID)
     *    → Formato: 36 caracteres "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
     *    → Es lo que usuario guarda y usa en verifyOtp
     * 
     * 3. Guardar token en map (token activo)
     *    → Con metadatos: código, destino, expiración, intentos
     * 
     * 4. Retornar otpId para que usuario lo guarde
     *    → En desarrollo: revisar TokenData en debugger para ver código
     *    → En producción: ver código en SMS/Email recibido
     * 
     * @param destination Destino ("+593987654321" o "user@example.com")
     * @return otpId UUID para usar en verifyOtp
     */
    @Override
    public String sendOtp(String destination) {
        // Generar código de 6 dígitos criptográficamente seguro
        int numero = 100000 + secureRandom.nextInt(900000);
        String codigo = String.valueOf(numero);
        
        // Generar ID único para este OTP
        String otpId = UUID.randomUUID().toString();
        
        // Guardar token con su estado
        tokensActivos.put(otpId, new TokenData(codigo, destination));

        return otpId;
    }

    /**
     * Enmascara el destino para logs (seguridad, no registra completo).
     * 
     * Ejemplos:
     * - "+593987654321" → "+593****4321"
     * - "usuario@example.com" → "usu***@example.com"
     * 
     * @param destination Destino a enmascarar
     * @return Destino parcialmente visible (para logging)
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

    /**
     * Verifica si el código OTP es correcto (mock, valida en memoria).
     * 
     * Validaciones de seguridad:
     * 
     * 1. Token existe:
     *    → Si no existe otpId en map → false (token inexistente)
     * 
     * 2. Token no expirado:
     *    → Si actual > expiration → false + remove token
     * 
     * 3. Token no bloqueado:
     *    → Si intentosFallidos >= 3 → false + remove token
     *    → Protección anti-brute-force: máximo 3 intentos
     * 
     * 4. Código correcto:
     *    → Si código matches exactamente → true + remove token (anti-replay)
     *    → Si no coincide → false + increment intentosFallidos
     * 
     * Lógica de limpieza:
     *    → Token consumido exitosamente: remove (anti-replay)
     *    → Token expirado: remove
     *    → Token bloqueado: remove
     *    → Token activo fallido: mantener (para incrementar contador)
     * 
     * @param otpId Token ID retornado por sendOtp
     * @param code Código OTP ingresado por usuario (ej: "123456")
     * @return true si verificación exitosa, false si falla por cualquier razón
     */
    @Override
    public boolean verifyOtp(String otpId, String code) {
        TokenData token = tokensActivos.get(otpId);
        if (token == null) {
            return false;  // Token no existe
        }
        
        if (token.estaExpirado()) {
            tokensActivos.remove(otpId);  // Limpiar expirado
            return false;
        }
        
        if (token.estaBloqueado()) {
            tokensActivos.remove(otpId);  // Limpiar bloqueado
            return false;
        }
        
        if (token.codigo.equals(code)) {
            tokensActivos.remove(otpId);  // Limpiar usado (anti-replay)
            return true;
        } else {
            token.intentosFallidos++;  // Incrementar contador
            return false;
        }
    }
}
