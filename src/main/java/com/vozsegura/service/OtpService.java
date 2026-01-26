package com.vozsegura.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.vozsegura.client.OtpClient;

/**
 * Servicio para generar y verificar codigos OTP (One-Time Password).
 *
 * Seguridad:
 * - Delegacion completa a OtpClient (AWS SES o implementacion local)
 * - OTP de 6 digitos generado con SecureRandom
 * - Expiracion: 5 minutos
 * - No se loguea el codigo OTP ni el email completo
 *
 * Implementacion:
 * - Usa OtpClient como abstraccion (AwsSesOtpClient en prod)
 * - OtpClient maneja rate limiting, generacion, almacenamiento y envio
 * - Este servicio es un wrapper para compatibilidad con codigo existente
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Service
public class OtpService {

    private final OtpClient otpClient;

    public OtpService(OtpClient otpClient) {
        this.otpClient = otpClient;
    }

    /**
     * Genera y envia codigo OTP por email.
     *
     * @param email Direccion de email destino (debe estar descifrado)
     * @return Token UUID para correlacionar con verificacion
     * @throws RuntimeException si el envio falla
     */
    public String sendOtp(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            log.warn("sendOtp: Invalid email format");
            throw new IllegalArgumentException("Email invalido");
        }

        String token = otpClient.sendOtp(email);
        if (token == null) {
            throw new RuntimeException("No se pudo enviar OTP. Intente nuevamente.");
        }

        return token;
    }

    /**
     * Verifica codigo OTP contra token.
     *
     * @param token Token UUID retornado por sendOtp()
     * @param otpCode Codigo ingresado por usuario (6 digitos)
     * @return true si el codigo es valido y no expiro
     */
    public boolean verifyOtp(String token, String otpCode) {
        if (token == null || otpCode == null) {
            log.warn("verifyOtp: Invalid parameters");
            return false;
        }

        return otpClient.verifyOtp(token, otpCode);
    }
}
