package com.vozsegura.vozsegura.client;

/**
 * Interface para cliente OTP (One-Time Password).
 * 
 * Responsabilidades:
 * - Enviar códigos OTP a través de canal secundario (SMS/Email)
 * - Verificar códigos OTP entregados por el usuario
 * - Gestionar estado de tokens (expiración, intentos fallidos)
 * 
 * Flujo de uso (en UnifiedAuthService - Step 2):
 * 
 * 1. Envío de OTP:
 *    otpClient.sendOtp("+593987654321") → Retorna otpId (ej: "uuid-xxxx-yyyy")
 *    El sistema envía código a ese número telefónico
 * 
 * 2. Verificación de OTP:
 *    otpClient.verifyOtp(otpId, "123456") → true/false
 *    Valida que el código sea correcto, no expirado, no bloqueado por brute-force
 * 
 * Políticas de seguridad:
 * - Códigos OTP válidos por 5 minutos máximo
 * - Máximo 3 intentos fallidos antes de bloqueo
 * - Protección anti-replay: cada código solo válido una vez
 * - Generación criptográficamente segura (SecureRandom)
 * 
 * Implementaciones:
 * - MockOtpClient (dev/test): Genera códigos de prueba, sin envío real
 * - Integración real (prod): AWS SNS para SMS, SendGrid para Email
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * 
 * @see UnifiedAuthService#autenticar(String, String, String) - Paso 2 (OTP)
 * @see MockOtpClient - Implementación mock para desarrollo
 */
public interface OtpClient {

    /**
     * Envía un código OTP al destino especificado.
     * 
     * Flujo interno:
     * 1. Generar código aleatorio de 6 dígitos
     * 2. Crear token con ID único (UUID)
     * 3. Guardar token con metadatos (destino, expiración)
     * 4. Enviar código al destino (SMS/Email) en background
     * 5. Retornar token ID para que usuario lo guarde
     * 
     * @param destination Número telefónico o email destino (ej: "+593987654321" o "user@example.com")
     * @return otpId: ID único para este envío (UUID). Usuario debe guardar esto.
     *         Usado posteriormente en verifyOtp() para identificar el token.
     * 
     * @throws IllegalArgumentException si destination no está en formato válido
     * @throws RuntimeException si falla el envío (red down, servicio no disponible)
     */
    String sendOtp(String destination);

    /**
     * Verifica si el código OTP coincide con el enviado.
     * 
     * Validaciones:
     * - Token existe y no ha expirado (5 min)
     * - Código coincide exactamente
     * - Intentos fallidos < 3 (no bloqueado por brute-force)
     * - Token aún no fue verificado (anti-replay)
     * 
     * Flujo de seguridad:
     * 1. Buscar token por otpId
     * 2. Verificar no esté expirado
     * 3. Verificar no esté bloqueado (demasiados intentos)
     * 4. Comparar código (constante-time para evitar timing attacks)
     * 5. Si falla: incrementar contador de intentos
     * 6. Si éxito: marcar token como consumido (anti-replay)
     * 7. Limpiar tokens expirados de la memoria
     * 
     * @param otpId Token ID retornado por sendOtp()
     * @param code Código OTP entregado por usuario (ej: "123456")
     * @return true si verificación exitosa, false si falla
     */
    boolean verifyOtp(String otpId, String code);
}
