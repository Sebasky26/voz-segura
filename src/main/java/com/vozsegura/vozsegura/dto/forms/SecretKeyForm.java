package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario de clave secreta (Staff/Admin).
 * Después de verificar identidad, se solicita la clave secreta de AWS.
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

