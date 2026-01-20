package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para formulario de seguimiento anónimo de denuncias.
 * 
 * Uso:
 * - Ciudadano ingresa tracking ID (hash SHA-256 de su cédula)
 * - Sistema busca denuncia por tracking ID
 * - Retorna estado público (nunca datos sensibles del denunciante)
 * 
 * Validación:
 * - Formato UUID (36 caracteres con guiones)
 * - Patrón: 8-4-4-4-12 caracteres hexadecimales
 * - Rate limiting por IP (máx 10 intentos/hora)
 * 
 * Privacidad:
 * - No revela si tracking ID existe o no (mensaje genérico)
 * - No expone información del denunciante
 * - Solo muestra estado general (PENDING, IN_PROGRESS, RESOLVED, etc.)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public class TrackingForm {

    @NotBlank(message = "El código de seguimiento es requerido")
    @Size(min = 36, max = 36, message = "El código debe tener 36 caracteres")
    @Pattern(regexp = "^[a-fA-F0-9\\-]{36}$", message = "Formato de código inválido")
    private String trackingId;

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }
}
