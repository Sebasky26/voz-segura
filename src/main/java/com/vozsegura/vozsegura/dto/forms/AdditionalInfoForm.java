package com.vozsegura.vozsegura.dto.forms;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para formulario de información adicional solicitada por analista.
 * 
 * Flujo:
 * 1. Analista revisa denuncia y requiere más información
 * 2. Sistema marca denuncia como "INFO_REQUESTED"
 * 3. Ciudadano recibe notificación (email/SMS)
 * 4. Ciudadano accede a /denuncia/adicional-info
 * 5. Carga información adicional (texto + evidencias)
 * 6. Se cifra y agrega a la denuncia
 * 
 * Validaciones:
 * - additionalInfo: 50-5000 caracteres (texto que se cifrará)
 * - evidences: máximo 5 archivos nuevos (cada uno máx 25MB)
 * - Total evidencias por denuncia: máximo 5 (incluyendo originales)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public class AdditionalInfoForm {

    @NotBlank(message = "La información adicional es requerida")
    @Size(min = 50, max = 5000, message = "La información debe tener entre 50 y 5000 caracteres")
    private String additionalInfo;

    private MultipartFile[] evidences;

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public MultipartFile[] getEvidences() {
        return evidences;
    }

    public void setEvidences(MultipartFile[] evidences) {
        this.evidences = evidences;
    }
}
