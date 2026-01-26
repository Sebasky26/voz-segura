package com.vozsegura.client.mock;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vozsegura.client.OtpClient;

/**
 * Mock OTP Client - Genera y valida códigos OTP en memoria para desarrollo.
 *
 * Responsabilidades:
 * - Generar códigos OTP de 6 dígitos usando SecureRandom (igual a producción)
 * - Almacenar en ConcurrentHashMap (in-memory, no envía email/SMS real)
 * - Validar OTP con protecciones reales: anti-brute-force, expiración, anti-replay
 * - Limpiar tokens automáticamente tras verificación o expiración
 *
 * Diferencias con producción:
 * - NO envía SMS/Email real (solo simula - desarrollo)
 * - NO integra con Twilio/AWS SNS
 * - Códigos de prueba accesibles en memory (via debugger en IDE)
 *
 * Protecciones (reales, idénticas a producción):
 * - SecureRandom para generación criptográfica de códigos
 * - Expiración de tokens (5 minutos TTL)
 * - Anti-brute-force: máximo 3 intentos fallidos por token
 * - Anti-replay: token se marca como consumido tras verificación exitosa
 * - Thread-safe: ConcurrentHashMap para acceso concurrente
 *
 * Integración:
 * - Implementa interfaz OtpClient
 * - @Profile("dev", "default") - solo en desarrollo
 * - En producción: reemplazar por AwsSesOtpClient (SES real)
 * - Spring selecciona automáticamente según profile activo
 *
 * Ciclo de vida de token:
 * - sendOtp(): Generar código UUID + guardar TokenData
 * - verifyOtp(): Validar y consumir token o incrementar intentos
 * - Auto-limpieza: Tokens expirados/consumidos se remueven del mapa
 *
 * @author Voz Segura Team
 * @version 2.0
 * @see AwsSesOtpClient
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
     * Gestiona: código, destino, expiración, contador de intentos fallidos.
     * Thread-safe: utilizado dentro de ConcurrentHashMap.
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
