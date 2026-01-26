package com.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario de clave secreta (Staff/Admin - Paso 3 del MFA).
 * 
 * Flujo:
 * 1. Usuario verifica identidad (cédula + biométrico contra Registro Civil)
 * 2. Sistema identifica que es Staff/Admin
 * 3. Solicita clave secreta (este formulario)
 * 4. Luego solicita OTP (paso 4)
 * 
 * Validación:
 * - Mínimo 8 caracteres (BCrypt hash comparado en servidor)
 * - No se muestra en plain text en la UI
 * - No se transmite nunca en plain text (solo HTTPS POST)
 * - Comparación resistente a timing attacks (BCrypt.checkpw)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public class SecretKeyForm {

    @NotBlank(message = "La clave secreta es requerida")
    @Size(min = 8, message = "La clave secreta debe tener al menos 8 caracteres")
    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}

