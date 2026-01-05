package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para el formulario de clave secreta (Staff/Admin).
 * Después de verificar identidad, se solicita la clave secreta de AWS.
 */
public class SecretKeyForm {

    @NotBlank(message = "La clave secreta es requerida")
    private String secretKey;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}

