package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "denuncia", schema = "denuncias")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_registro")
    private Long idRegistro;

    @Column(nullable = false, unique = true, length = 40)
    private String trackingId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "identity_vault_id")
    private IdentityVault identityVault;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(name = "complaint_type", length = 64)
    private String complaintType;

    @Column(name = "priority", length = 16)
    private String priority = "MEDIUM";

    @Column(name = "analyst_notes", columnDefinition = "text")
    private String analystNotes;

    @Column(name = "derived_to", length = 255)
    private String derivedTo;

    @Column(name = "derived_at")
    private OffsetDateTime derivedAt;

    @Column(name = "requires_more_info")
    private boolean requiresMoreInfo = false;

    @Column(name = "encrypted_text", nullable = false, columnDefinition = "text")
    private String encryptedText;

    @Column(name = "company_name", nullable = false, length = 255)
    private String companyName;

    @Column(name = "company_address", nullable = false, length = 512)
    private String companyAddress;

    @Column(name = "company_contact", nullable = false, length = 255)
    private String companyContact;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
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
