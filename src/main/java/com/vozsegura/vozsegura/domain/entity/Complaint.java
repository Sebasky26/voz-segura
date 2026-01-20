package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Denuncia anónima cifrada almacenada en schema denuncias.denuncia
 * 
 * Cada denuncia es completamente anónima:
 * - Identificación únicamente por trackingId (UUID público)
 * - Contenido cifrado con AES-256-GCM
 * - Vinculada a bóveda de identidad (IdentityVault) sin datos personales
 * 
 * Ciclo de vida:
 * PENDING → IN_REVIEW → (RESOLVED|DERIVED|REJECTED|NEEDS_INFO) → ARCHIVED
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */

@Entity
@Table(name = "denuncia", schema = "denuncias")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** ID único de la denuncia (clave primaria) */
    private Long id;

    @Column(name = "id_registro")
    /** ID de registro en IdentityVault (referencia anónima) */
    private Long idRegistro;

    @Column(nullable = false, unique = true, length = 40)
    /** Tracking ID único (UUID hexadecimal) para seguimiento anónimo público */
    private String trackingId;
/** Referencia al bóveda de identidad (hash anónimo del denunciante) */
    private IdentityVault identityVault;

    @Column(nullable = false, length = 32)
    /** Estado actual: PENDING, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED, INFO_REQUESTED */
    private String status;

    @Column(nullable = false, length = 16)
    /** Severidad: HIGH (crítico), MEDIUM (moderado), LOW (leve) */
    private String severity;

    @Column(name = "complaint_type", length = 64)
    /** Tipo de denuncia: corrupción, acoso, discriminación, etc. */
    private String complaintType;

    @Column(name = "priority", length = 16)
    /** Prioridad: URGENT (atender YA), NORMAL (normal), LOW (puede esperar) */
    private String priority = "MEDIUM";

    @Column(name = "analyst_notes", columnDefinition = "text")
    /** Notas privadas del analista (NO visible al denunciante) */
    private String analystNotes;

    @Column(name = "derived_to", length = 255)
    /** Nombre de institución a la que se derivó (ej: Ministerio Público) */
    private String derivedTo;

    @Column(name = "derived_at")
    /** Timestamp de cuándo se derivó */
    private OffsetDateTime derivedAt;

    @Column(name = "requires_more_info")
    /** Flag: analista solicitó información adicional */
    private boolean requiresMoreInfo = false;

    @Column(name = "encrypted_text", nullable = false, columnDefinition = "text")
    /** Contenido cifrado (AES-256-GCM Base64) - NUNCA descifrar para denunciante */
    private String encryptedText;

    @Column(name = "company_name", nullable = false, length = 255)
    /** Nombre empresa/institución afectada (público) */
    private String companyName;

    @Column(name = "company_address", nullable = false, length = 512)
    /** Dirección empresa/institución (público) */
    private String companyAddress;

    @Column(name = "company_contact", nullable = false, length = 255)
    /** Persona de contacto en empresa (público) */
    private String companyContact;

    @Column(nullable = false)
    /** Timestamp de creación (UTC, generado por BD) */
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    /** Timestamp de última modificación (UTC, actualizado por BD) */
    private OffsetDateTime updatedAt;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Long idRegistro) { this.idRegistro = idRegistro; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public IdentityVault getIdentityVault() { return identityVault; }
    public void setIdentityVault(IdentityVault identityVault) { this.identityVault = identityVault; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getComplaintType() { return complaintType; }
    public void setComplaintType(String complaintType) { this.complaintType = complaintType; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAnalystNotes() { return analystNotes; }
    public void setAnalystNotes(String analystNotes) { this.analystNotes = analystNotes; }

    public String getDerivedTo() { return derivedTo; }
    public void setDerivedTo(String derivedTo) { this.derivedTo = derivedTo; }

    public OffsetDateTime getDerivedAt() { return derivedAt; }
    public void setDerivedAt(OffsetDateTime derivedAt) { this.derivedAt = derivedAt; }

    public boolean isRequiresMoreInfo() { return requiresMoreInfo; }
    public void setRequiresMoreInfo(boolean requiresMoreInfo) { this.requiresMoreInfo = requiresMoreInfo; }

    public String getEncryptedText() { return encryptedText; }
    public void setEncryptedText(String encryptedText) { this.encryptedText = encryptedText; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyAddress() { return companyAddress; }
    public void setCompanyAddress(String companyAddress) { this.companyAddress = companyAddress; }

    public String getCompanyContact() { return companyContact; }
    public void setCompanyContact(String companyContact) { this.companyContact = companyContact; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
