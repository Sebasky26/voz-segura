package com.vozsegura.vozsegura.dto.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO para el formulario de creación de denuncias públicas.
 * 
 * Recopila información de la denuncia:
 * - Detalle (texto cifrado después con AES-256-GCM)
 * - Evidencias (archivos, máximo 5 por denuncia, 25MB cada uno)
 * - Información de la empresa afectada
 * 
 * Validaciones:
 * - detail: 50-4000 caracteres (obligatorio)
 * - evidences: máximo 5 archivos, cada uno máximo 25MB
 *   Formatos permitidos: PDF, DOCX, JPG, PNG, MP4 (dentro de whitelist)
 * - companyName, companyAddress, companyContact: obligatorios
 * - companyEmail: formato válido (opcional pero si se proporciona, debe ser válido)
 * - companyPhone: máximo 20 caracteres (opcional)
 * 
 * Seguridad:
 * - Todos los campos de texto se cifran con AES-256-GCM antes de almacenar
 * - Evidencias se almacenan en S3/blob storage (no en base de datos)
 * - Contenido se valida contra whitelist de tipos MIME
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
public class ComplaintForm {

    @NotBlank(message = "El detalle de la denuncia es requerido")
    @Size(min = 50, max = 4000, message = "El detalle debe tener entre 50 y 4000 caracteres")
    private String detail;

    private MultipartFile[] evidences;

    @NotBlank
    @Size(max = 255)
    private String companyName;

    @NotBlank
    @Size(max = 512)
    private String companyAddress;

    @NotBlank
    @Size(max = 255)
    private String companyContact;

    @Email
    @Size(max = 255)
    private String companyEmail;

    @Size(max = 20)
    private String companyPhone;

    @Size(min = 50, max = 4000, message = "La información adicional debe tener entre 50 y 4000 caracteres")
    private String additionalInfo;

    private MultipartFile[] newEvidences;

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public MultipartFile[] getEvidences() { return evidences; }
    public void setEvidences(MultipartFile[] evidences) { this.evidences = evidences; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }

    public String getCompanyContact() { return companyContact; }
    public void setCompanyContact(String companyContact) { this.companyContact = companyContact; }

    public String getCompanyEmail() { return companyEmail; }
    public void setCompanyEmail(String companyEmail) { this.companyEmail = companyEmail; }

    public String getCompanyPhone() { return companyPhone; }
    public void setCompanyPhone(String companyPhone) { this.companyPhone = companyPhone; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public MultipartFile[] getNewEvidences() { return newEvidences; }
    public void setNewEvidences(MultipartFile[] newEvidences) { this.newEvidences = newEvidences; }
}
