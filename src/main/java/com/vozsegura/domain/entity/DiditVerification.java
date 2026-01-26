package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Registro de verificación con Didit almacenado en {@code registro_civil.didit_verification}.
 *
 * <p>Este registro guarda trazabilidad del proceso de verificación asociado a una identidad
 * alojada en {@code registro_civil.identity_vault}.</p>
 *
 * <h2>Seguridad</h2>
 * <ul>
 *   <li>No almacenar número de documento en texto plano.</li>
 *   <li>{@code documentNumberHash} permite búsquedas sin descifrar.</li>
 *   <li>{@code documentNumberEncrypted} permite recuperación controlada bajo autorización.</li>
 * </ul>
 */
@Entity
@Table(name = "didit_verification", schema = "registro_civil")
public class DiditVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia a la bóveda de identidad del denunciante.
     * Es el vínculo correcto en el esquema nuevo (no usar id_registro).
     */
    @Column(name = "identity_vault_id", nullable = false)
    private Long identityVaultId;

    /**
     * Identificador de sesión de Didit.
     * En BD es varchar(200). No se fuerza unique a nivel entidad para no desalinear con el SQL.
     */
    @Column(name = "didit_session_id", nullable = false, length = 200)
    private String diditSessionId;

    /** Hash del número de documento (para búsquedas sin descifrar). */
    @Column(name = "document_number_hash", length = 128)
    private String documentNumberHash;

    /** Número de documento cifrado (para auditoría / recuperación autorizada). */
    @Column(name = "document_number_encrypted", columnDefinition = "text")
    private String documentNumberEncrypted;

    /** Estado de la verificación (por ejemplo: VERIFIED, FAILED, PENDING). */
    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus;

    /**
     * Fecha/hora en que se completó la verificación.
     * Puede ser null si el estado sigue PENDING.
     */
    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * Número de documento en texto plano, solo para uso temporal en memoria.
     * No debe persistirse ni registrarse en logs.
     */
    @Transient
    private String documentNumber;

    public DiditVerification() {}

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = (this.createdAt == null) ? now : this.createdAt;
        this.updatedAt = (this.updatedAt == null) ? now : this.updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters y setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdentityVaultId() { return identityVaultId; }
    public void setIdentityVaultId(Long identityVaultId) { this.identityVaultId = identityVaultId; }

    public String getDiditSessionId() { return diditSessionId; }
    public void setDiditSessionId(String diditSessionId) { this.diditSessionId = diditSessionId; }

    public String getDocumentNumberHash() { return documentNumberHash; }
    public void setDocumentNumberHash(String documentNumberHash) { this.documentNumberHash = documentNumberHash; }

    public String getDocumentNumberEncrypted() { return documentNumberEncrypted; }
    public void setDocumentNumberEncrypted(String documentNumberEncrypted) { this.documentNumberEncrypted = documentNumberEncrypted; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
}
