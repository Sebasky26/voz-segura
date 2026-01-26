package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Evidencia asociada a una denuncia almacenada en {@code evidencias.evidencia}.
 *
 * <p>La evidencia puede guardarse de dos formas:</p>
 * <ul>
 *   <li>Contenido cifrado en base de datos ({@code encrypted_content}).</li>
 *   <li>Referencia a Supabase Storage ({@code storage_object_key}) y el archivo se guarda fuera de la BD.</li>
 * </ul>
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>El nombre del archivo se almacena cifrado en {@code file_name_encrypted}.</li>
 *   <li>No se debe almacenar el archivo en texto plano.</li>
 * </ul>
 */
@Entity
@Table(name = "evidencia", schema = "evidencias")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_denuncia", nullable = false)
    private Long idDenuncia;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_denuncia", insertable = false, updatable = false)
    private Complaint complaint;

    /** Nombre de archivo cifrado (por ejemplo AES-GCM en Base64). */
    @Column(name = "file_name_encrypted", nullable = false, columnDefinition = "text")
    private String fileNameEncrypted;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /**
     * Contenido cifrado del archivo.
     * Puede ser null si se utiliza {@code storageObjectKey}.
     */
    @Column(name = "encrypted_content", columnDefinition = "bytea")
    private byte[] encryptedContent;

    /**
     * Llave/ruta del objeto en Storage (si aplica).
     * Puede ser null si el archivo está embebido en {@code encryptedContent}.
     */
    @Column(name = "storage_object_key", columnDefinition = "text")
    private String storageObjectKey;

    /** Checksum para integridad del archivo (recomendado SHA-256 en hex). */
    @Column(name = "checksum", nullable = false, length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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

    public Long getIdDenuncia() { return idDenuncia; }
    public void setIdDenuncia(Long idDenuncia) { this.idDenuncia = idDenuncia; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public String getFileNameEncrypted() { return fileNameEncrypted; }
    public void setFileNameEncrypted(String fileNameEncrypted) { this.fileNameEncrypted = fileNameEncrypted; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public byte[] getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(byte[] encryptedContent) { this.encryptedContent = encryptedContent; }

    public String getStorageObjectKey() { return storageObjectKey; }
    public void setStorageObjectKey(String storageObjectKey) { this.storageObjectKey = storageObjectKey; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
