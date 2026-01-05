package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario de login unificado (denunciantes, staff, admin).
 * Todos los usuarios se autentican primero contra el Registro Civil.
 */
public class UnifiedLoginForm {

    @NotBlank(message = "La cédula es requerida")
    @Size(min = 10, max = 10, message = "La cédula debe tener 10 dígitos")
    private String cedula;

    @NotBlank(message = "El código dactilar es requerido")
    private String codigoDactilar;

    @NotBlank(message = "El CAPTCHA es requerido")
    @Size(min = 6, max = 6, message = "El CAPTCHA debe tener 6 caracteres")
    private String captcha;

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

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}

