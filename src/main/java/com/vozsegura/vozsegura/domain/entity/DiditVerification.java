package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad para almacenar datos de verificación biométrica con Didit.
 * Guarda: nombre completo, cédula, y datos de la sesión de verificación.
 */
@Entity
@Table(name = "didit_verification", schema = "secure_identities")
public class DiditVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Session ID de Didit - identifica la sesión de verificación
     * Nota: Puede haber múltiples sesiones por usuario (permite reintentos)
     */
    @Column(name = "didit_session_id", nullable = false, length = 255)
    private String diditSessionId;

    /**
     * Número de cédula del documento escaneado
     * Nota: Se actualiza si el usuario se verifica de nuevo (permite múltiples verificaciones)
     */
    @Column(name = "document_number", nullable = false, length = 20)
    private String documentNumber;

    /**
     * Nombre completo del documento escaneado (first_name + last_name)
     */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /**
     * Primer nombre del documento
     */
    @Column(name = "first_name", length = 255)
    private String firstName;

    /**
     * Apellido del documento
     */
    @Column(name = "last_name", length = 255)
    private String lastName;

    /**
     * Resultado de la verificación (VERIFIED, FAILED, PENDING, etc.)
     */
    @Column(name = "verification_status", nullable = false, length = 50)
    private String verificationStatus;

    /**
     * Hash SHA-256 del ciudadano para vincular con denuncia
     */
    @Column(name = "citizen_hash", length = 128)
    private String citizenHash;

    /**
     * Timestamp de creación del webhook
     */
    @Column(name = "verified_at", nullable = false)
    private OffsetDateTime verifiedAt;

    /**
     * IP desde la que se realizó la verificación (del webhook)
     */
    @Column(name = "webhook_ip", length = 45)
    private String webhookIp;

    /**
     * Raw payload del webhook de Didit (para auditoría)
     */
    @Column(name = "webhook_payload", columnDefinition = "TEXT")
    private String webhookPayload;

    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public String getCitizenHash() {
        return citizenHash;
    }

    public void setCitizenHash(String citizenHash) {
        this.citizenHash = citizenHash;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getWebhookIp() {
        return webhookIp;
    }

    public void setWebhookIp(String webhookIp) {
        this.webhookIp = webhookIp;
    }

    public String getWebhookPayload() {
        return webhookPayload;
    }

    public void setWebhookPayload(String webhookPayload) {
        this.webhookPayload = webhookPayload;
    }
}
