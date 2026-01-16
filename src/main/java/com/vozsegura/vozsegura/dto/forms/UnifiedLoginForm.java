package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario de login unificado (denunciantes, staff, admin).
 * Todos los usuarios se autentican primero contra el Registro Civil + Cloudflare Turnstile.
 * 
 * Nota: La validación de Turnstile se realiza en el controlador,
 * no en este DTO, ya que es un token de cliente no incluido en el formulario HTML tradicional.
 */
public class UnifiedLoginForm {

    @NotBlank(message = "La cédula es requerida")
    @Size(min = 10, max = 10, message = "La cédula debe tener 10 dígitos")
    private String cedula;

    @NotBlank(message = "El código dactilar es requerido")
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

