package com.vozsegura.vozsegura.dto.forms;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario para agregar información adicional a una denuncia
 * cuando el analista solicita más información.
 */
public class AdditionalInfoForm {

    @NotBlank(message = "La información adicional es requerida")
    @Size(min = 20, max = 5000, message = "La información debe tener entre 20 y 5000 caracteres")
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
