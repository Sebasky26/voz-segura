package com.vozsegura.dto.forms;

import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para formulario de muestra biométrica (OTP biométrica).
 * 
 * Uso:
 * - Ciudadano captura muestra biométrica (huella dactilar, rostro, iris, etc.)
 * - Se envía como multipart file a verificación contra Registro Civil
 * - Resultado: válido/inválido
 * 
 * Formato:
 * - Archivo binario (jpg, png, etc. según tipo de biométrico)
 * - Máximo tamaño: 5MB
 * - Validación en lado del servidor
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public class BiometricOtpForm {

    private MultipartFile biometricSample;

    public MultipartFile getBiometricSample() { return biometricSample; }
    public void setBiometricSample(MultipartFile biometricSample) { this.biometricSample = biometricSample; }
}
