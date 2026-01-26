package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Bóveda de identidad almacenada en {@code registro_civil.identity_vault}.
 *
 * <p>Este registro separa la identidad del denunciante de la denuncia para mantener anonimato.
 * La denuncia solo referencia {@code identityVaultId} cuando corresponde.</p>
 *
 * <h2>Seguridad</h2>
 * <ul>
 *   <li>{@code documentHash} permite correlación/búsqueda sin descifrar el documento.</li>
 *   <li>{@code identityBlobEncrypted} contiene datos de identidad cifrados (por ejemplo JSON cifrado en Base64).</li>
 *   <li>La aplicación debe descifrar estos datos únicamente bajo un flujo autorizado (por ejemplo,
 *       una solicitud aprobada por la entidad receptora y el administrador del sistema).</li>
 * </ul>
 */
@Entity
@Table(name = "identity_vault", schema = "registro_civil")
public class IdentityVault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hash del documento (cédula u otro identificador) para búsquedas sin revelar el valor real.
     * Corresponde a la columna {@code document_hash}.
     */
    @Column(name = "document_hash", unique = true, length = 128)
    private String documentHash;

    /**
     * Identidad cifrada completa.
     * Recomendación: JSON cifrado con AES-GCM y codificado en Base64.
     */
    @Column(name = "identity_blob_encrypted", nullable = false, columnDefinition = "text")
    private String identityBlobEncrypted;

    /**
     * Versión de llave/estrategia criptográfica.
     * Permite rotación de llaves sin romper datos antiguos.
     */
    @Column(name = "key_version", nullable = false)
    private Integer keyVersion = 1;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = (this.createdAt == null) ? now : this.createdAt;
        this.updatedAt = (this.updatedAt == null) ? now : this.updatedAt;
        if (this.keyVersion == null) {
            this.keyVersion = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocumentHash() { return documentHash; }
    public void setDocumentHash(String documentHash) { this.documentHash = documentHash; }

    public String getIdentityBlobEncrypted() { return identityBlobEncrypted; }
    public void setIdentityBlobEncrypted(String identityBlobEncrypted) { this.identityBlobEncrypted = identityBlobEncrypted; }

    public Integer getKeyVersion() { return keyVersion; }
    public void setKeyVersion(Integer keyVersion) { this.keyVersion = keyVersion; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
