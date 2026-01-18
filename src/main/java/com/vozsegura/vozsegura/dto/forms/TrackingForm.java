package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario de consulta de seguimiento.
 * El ciudadano ingresa su código de seguimiento para ver el estado de su denuncia.
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
