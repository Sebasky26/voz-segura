package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad para almacenar datos de verificación biométrica con Didit.
 * Estructura simplificada: id, id_registro, didit_session_id, document_number,
 * verification_status, verified_at, created_at, updated_at.
 * El document_number debe coincidir con cedula de la persona referenciada.
 */
@Entity
@Table(name = "didit_verification", schema = "registro_civil")
public class DiditVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia a registro_civil.personas
     */
    @Column(name = "id_registro", nullable = false)
    private Long idRegistro;

    /**
     * Session ID de Didit - identifica la sesión de verificación
     * Permite trazabilidad y auditoría de cada verificación
     */
    @Column(name = "didit_session_id", nullable = false, length = 255, unique = true)
    private String diditSessionId;

    /**
     * Número de cédula del documento escaneado.
     * Debe coincidir con el cedula de registro_civil.personas del id_registro
     * Cifrado a nivel aplicación (AES-256)
     */
    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    /**
     * Resultado de la verificación (VERIFIED, FAILED, PENDING)
     */
    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus;

    /**
     * Timestamp cuando se completó la verificación biométrica
     */
    @Column(name = "verified_at", nullable = false)
    private OffsetDateTime verifiedAt;

    /**
     * Timestamp de creación del registro
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp de última actualización del registro
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructores
    public DiditVerification() {
    }

    public DiditVerification(Long idRegistro, String diditSessionId, String documentNumber,
                           String verificationStatus, OffsetDateTime verifiedAt) {
        this.idRegistro = idRegistro;
        this.diditSessionId = diditSessionId;
        this.documentNumber = documentNumber;
        this.verificationStatus = verificationStatus;
        this.verifiedAt = verifiedAt;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Long idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getDiditSessionId() {
        return diditSessionId;
    }

    public void setDiditSessionId(String diditSessionId) {
        this.diditSessionId = diditSessionId;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
