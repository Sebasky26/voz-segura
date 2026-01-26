package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Usuario interno del sistema almacenado en {@code staff.staff_user}.
 *
 * Roles válidos: ADMIN, ANALYST
 * Seguridad:
 * - password_hash: hash (BCrypt o similar)
 * - email/phone/mfa_secret: cifrados
 * - No se almacena cédula ni PII innecesaria
 */
@Entity
@Table(name = "staff_user", schema = "staff")
public class StaffUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username único (en BD es CITEXT: case-insensitive). */
    @Column(name = "username", nullable = false, unique = true, columnDefinition = "citext")
    private String username;

    /** Hash de contraseña (nunca texto plano). */
    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    /** Rol del usuario: ADMIN o ANALYST. */
    @Column(name = "role", nullable = false, length = 30)
    private String role;

    /** Indica si el usuario puede autenticarse. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Email cifrado (opcional). */
    @Column(name = "email_encrypted", columnDefinition = "text")
    private String emailEncrypted;

    /** Teléfono cifrado (opcional). */
    @Column(name = "phone_encrypted", columnDefinition = "text")
    private String phoneEncrypted;

    /** Secreto MFA cifrado (opcional). */
    @Column(name = "mfa_secret_encrypted", columnDefinition = "text")
    private String mfaSecretEncrypted;

    /**
     * Hash SHA-256 de la cédula para enlazar con verificación Didit.
     * Permite que staff que se verifica con Didit sea reconocido sin guardar cédula en texto plano.
     * Este campo es único y permite búsquedas anónimas.
     */
    @Column(name = "cedula_hash_idx", unique = true, length = 128)
    private String cedulaHashIdx;

    /** ID del staff que creó a este usuario (puede ser null). */
    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Último login exitoso. */
    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    // Campos transient (solo memoria)
    @Transient private String email;
    @Transient private String phone;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getEmailEncrypted() { return emailEncrypted; }
    public void setEmailEncrypted(String emailEncrypted) { this.emailEncrypted = emailEncrypted; }

    public String getPhoneEncrypted() { return phoneEncrypted; }
    public void setPhoneEncrypted(String phoneEncrypted) { this.phoneEncrypted = phoneEncrypted; }

    public String getMfaSecretEncrypted() { return mfaSecretEncrypted; }
    public void setMfaSecretEncrypted(String mfaSecretEncrypted) { this.mfaSecretEncrypted = mfaSecretEncrypted; }

    public String getCedulaHashIdx() { return cedulaHashIdx; }
    public void setCedulaHashIdx(String cedulaHashIdx) { this.cedulaHashIdx = cedulaHashIdx; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
