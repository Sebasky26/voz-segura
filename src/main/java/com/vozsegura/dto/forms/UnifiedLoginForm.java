package com.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para formulario de login unificado (entry point para todos los usuarios).
 * 
 * Flujo de Autenticación:
 * 1. Usuario ingresa cédula + código dactilar (biométrico)
 * 2. Validación en cliente (este DTO)
 * 3. Validación Cloudflare Turnstile (en controlador)
 * 4. Verificación contra Registro Civil (en UnifiedAuthService)
 * 5. Enrutamiento según tipo de usuario:
 *    - DENUNCIANTE → crear denuncia anónima
 *    - ANALYST/ADMIN → MFA (clave secreta + OTP)
 * 
 * Campos:
 * - cedula: Número de cédula (10 dígitos exactos)
 * - codigoDactilar: Código biométrico formato: A1234B5678 (letra-4dígitos-letra-4dígitos)
 * 
 * Seguridad:
 * - Solo validaciones de formato en este DTO
 * - Credenciales se transmiten como formulario HTTP POST (HTTPS obligatorio)
 * - Nunca se almacena cédula en plain text
 * - Inmediatamente se convierte a hash SHA-256 después de verificar
 * - Turnstile reCAPTCHA valida en controlador (previene fuerza bruta)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public class UnifiedLoginForm {

    @NotBlank(message = "La cédula es requerida")
    @Size(min = 10, max = 10, message = "La cédula debe tener 10 dígitos")
    private String cedula;

    @NotBlank(message = "El código dactilar es requerido")
    @Size(min = 10, max = 10, message = "El código dactilar debe tener exactamente 10 caracteres")
    @Pattern(
            regexp = "^[A-Z][1-9]{4}[A-Z][1-9]{4}$",
            message = "Formato inválido. Debe ser: 1 letra, 4 números, 1 letra, 4 números (ej: A1234B5678)"
    )
    private String codigoDactilar;

    // Getters y Setters
    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getCodigoDactilar() {
        return codigoDactilar;
    }

    public void setCodigoDactilar(String codigoDactilar) {
        this.codigoDactilar = codigoDactilar;
    }
}

