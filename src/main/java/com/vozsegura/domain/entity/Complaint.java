package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad JPA que representa una denuncia anónima almacenada en {@code denuncias.denuncia}.
 *
 * <p>Principios del modelo:</p>
 * <ul>
 *   <li>El seguimiento público se realiza únicamente mediante {@code trackingId}.</li>
 *   <li>El contenido de la denuncia y los datos de empresa/contacto se almacenan cifrados.</li>
 *   <li>La identidad del denunciante (si existe) se guarda separada en {@code registro_civil.identity_vault}
 *       y se referencia con {@code identityVaultId}.</li>
 * </ul>
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>No almacenar texto plano con información sensible (PII o datos de empresa).</li>
 *   <li>Las columnas {@code *_encrypted} deben contener texto cifrado (por ejemplo Base64 de AES-GCM).</li>
 * </ul>
 */
@Entity
@Table(name = "denuncia", schema = "denuncias")
public class Complaint {

    /** Identificador interno de la denuncia (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador público para seguimiento anónimo.
     * Corresponde a la columna {@code tracking_id}.
     */
    @Column(name = "tracking_id", nullable = false, unique = true, length = 64)
    private String trackingId;

    /**
     * Referencia opcional a la bóveda de identidad del denunciante.
     * La identidad real vive en el esquema {@code registro_civil} y no se almacena en esta tabla.
     */
    @Column(name = "identity_vault_id")
    private Long identityVaultId;

    /** Estado del caso (texto controlado por la aplicación). */
    @Column(name = "status", nullable = false, length = 40)
    private String status;

    /** Severidad (texto controlado por la aplicación). */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    /** Tipo de denuncia (corrupción, acoso, etc.). */
    @Column(name = "complaint_type", nullable = false, length = 100)
    private String complaintType;

    /** Prioridad (texto controlado por la aplicación). */
    @Column(name = "priority", length = 20)
    private String priority;

    /** Nombre de institución a la que se derivó (si aplica). */
    @Column(name = "derived_to", length = 120)
    private String derivedTo;

    /** Timestamp de derivación (si aplica). */
    @Column(name = "derived_at")
    private OffsetDateTime derivedAt;

    /** Indica si se solicitó información adicional. */
    @Column(name = "requires_more_info", nullable = false)
    private boolean requiresMoreInfo = false;

    /**
     * Texto principal de la denuncia cifrado.
     * Corresponde a la columna {@code encrypted_text}.
     */
    @Column(name = "encrypted_text", nullable = false, columnDefinition = "text")
    private String encryptedText;

    /** Campos de empresa/contacto: solo cifrado. */
    @Column(name = "company_name_encrypted", columnDefinition = "text")
    private String companyNameEncrypted;

    @Column(name = "company_email_encrypted", columnDefinition = "text")
    private String companyEmailEncrypted;

    @Column(name = "company_phone_encrypted", columnDefinition = "text")
    private String companyPhoneEncrypted;

    @Column(name = "company_contact_encrypted", columnDefinition = "text")
    private String companyContactEncrypted;

    @Column(name = "company_address_encrypted", columnDefinition = "text")
    private String companyAddressEncrypted;

    /** Notas del analista: solo cifrado. */
    @Column(name = "analyst_notes_encrypted", columnDefinition = "text")
    private String analystNotesEncrypted;

    /** Analista asignado (staff). */
    @Column(name = "assigned_staff_id")
    private Long assignedStaffId;

    /** Timestamp de creación (set por la app o por BD). */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** Timestamp de actualización (set por la app o trigger). */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public Long getIdentityVaultId() { return identityVaultId; }
    public void setIdentityVaultId(Long identityVaultId) { this.identityVaultId = identityVaultId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getComplaintType() { return complaintType; }
    public void setComplaintType(String complaintType) { this.complaintType = complaintType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getDerivedTo() { return derivedTo; }
    public void setDerivedTo(String derivedTo) { this.derivedTo = derivedTo; }

    public OffsetDateTime getDerivedAt() { return derivedAt; }
    public void setDerivedAt(OffsetDateTime derivedAt) { this.derivedAt = derivedAt; }

    public boolean isRequiresMoreInfo() { return requiresMoreInfo; }
    public void setRequiresMoreInfo(boolean requiresMoreInfo) { this.requiresMoreInfo = requiresMoreInfo; }

    public String getEncryptedText() { return encryptedText; }
    public void setEncryptedText(String encryptedText) { this.encryptedText = encryptedText; }

    public String getCompanyNameEncrypted() { return companyNameEncrypted; }
    public void setCompanyNameEncrypted(String companyNameEncrypted) { this.companyNameEncrypted = companyNameEncrypted; }

    public String getCompanyEmailEncrypted() { return companyEmailEncrypted; }
    public void setCompanyEmailEncrypted(String companyEmailEncrypted) { this.companyEmailEncrypted = companyEmailEncrypted; }

    public String getCompanyPhoneEncrypted() { return companyPhoneEncrypted; }
    public void setCompanyPhoneEncrypted(String companyPhoneEncrypted) { this.companyPhoneEncrypted = companyPhoneEncrypted; }

    public String getCompanyContactEncrypted() { return companyContactEncrypted; }
    public void setCompanyContactEncrypted(String companyContactEncrypted) { this.companyContactEncrypted = companyContactEncrypted; }

    public String getCompanyAddressEncrypted() { return companyAddressEncrypted; }
    public void setCompanyAddressEncrypted(String companyAddressEncrypted) { this.companyAddressEncrypted = companyAddressEncrypted; }

    public String getAnalystNotesEncrypted() { return analystNotesEncrypted; }
    public void setAnalystNotesEncrypted(String analystNotesEncrypted) { this.analystNotesEncrypted = analystNotesEncrypted; }

    public Long getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(Long assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
