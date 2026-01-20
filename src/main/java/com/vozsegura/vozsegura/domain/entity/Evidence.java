package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;

/**
 * Evidencia cifrada asociada a denuncia (schema evidencias.evidencia)
 * 
 * Archivos cargados por denunciantes como prueba:
 * - Fotos, videos, documentos
 * - Todo cifrado con AES-256-GCM
 * - Asociado a denuncia vía id_denuncia
 * 
 * NÚNCEA se almacena en plain text
 */
@Entity
@Table(name = "evidencia", schema = "evidencias")
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** ID único de la evidencia */
    private Long id;

    @Column(name = "id_denuncia", nullable = false)
    /** ID de la denuncia a la que pertenece */
    private Long idDenuncia;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_denuncia", insertable = false, updatable = false)
    /** Referencia a Complaint (lazy loading para performance) */
    private Complaint complaint;

    @Column(name = "file_name", nullable = false, length = 255)
    /** Nombre del archivo (ej: "documento.pdf") */
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    /** Tipo MIME (ej: "application/pdf", "image/jpeg") - validado contra whitelist */
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    /** Tamaño en bytes (máximo 25MB = 26214400 bytes) */
    private long sizeBytes;

    @Column(name = "encrypted_content", nullable = false, columnDefinition = "bytea")
    /** Contenido del archivo cifrado con AES-256-GCM (guardado como binario) */
    private byte[] encryptedContent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdDenuncia() { return idDenuncia; }
    public void setIdDenuncia(Long idDenuncia) { this.idDenuncia = idDenuncia; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public byte[] getEncryptedContent() { return encryptedContent; }
    public void setEncryptedContent(byte[] encryptedContent) { this.encryptedContent = encryptedContent; }
}
