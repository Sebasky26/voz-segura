package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Identidad verificada almacenada en {@code registro_civil.personas}.
 *
 * <p>Esta tabla guarda atributos de identidad cifrados para un registro verificado, y se vincula
 * de forma estricta con {@code registro_civil.identity_vault} mediante {@code identityVaultId}.</p>
 *
 * <h2>Seguridad</h2>
 * <ul>
 *   <li>Los datos PII se almacenan cifrados en columnas {@code *_encrypted}.</li>
 *   <li>Los hashes permiten búsqueda/correlación sin descifrar.</li>
 *   <li>Los campos transient son solo para uso temporal en memoria y nunca deben persistirse ni loguearse.</li>
 * </ul>
 */
@Entity
@Table(name = "personas", schema = "registro_civil")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relación 1:1 con {@code identity_vault}. En la base está marcado como UNIQUE.
     * Cada bóveda de identidad puede tener, como máximo, un registro en personas.
     */
    @Column(name = "identity_vault_id", nullable = false, unique = true)
    private Long identityVaultId;

    /** Hash de cédula (u otro documento) para búsquedas sin descifrar. */
    @Column(name = "cedula_hash", length = 128)
    private String cedulaHash;

    /** Cédula cifrada (Base64), para recuperación controlada bajo autorización. */
    @Column(name = "cedula_encrypted", columnDefinition = "text")
    private String cedulaEncrypted;

    @Column(name = "primer_nombre_encrypted", columnDefinition = "text")
    private String primerNombreEncrypted;

    @Column(name = "segundo_nombre_encrypted", columnDefinition = "text")
    private String segundoNombreEncrypted;

    @Column(name = "primer_apellido_encrypted", columnDefinition = "text")
    private String primerApellidoEncrypted;

    @Column(name = "segundo_apellido_encrypted", columnDefinition = "text")
    private String segundoApellidoEncrypted;

    /** Hash del nombre completo para búsquedas sin exponer texto plano. */
    @Column(name = "nombre_completo_hash", length = 128)
    private String nombreCompletoHash;

    /** Sexo (dato no sensible por sí mismo, pero igual puede tratarse con cuidado). */
    @Column(name = "sexo", length = 20)
    private String sexo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Campos transient para uso temporal en memoria (no persistidos)

    @Transient
    private String cedula;

    @Transient
    private String primerNombre;

    @Transient
    private String segundoNombre;

    @Transient
    private String primerApellido;

    @Transient
    private String segundoApellido;

    public Persona() {}

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

    public String getCedulaHash() { return cedulaHash; }
    public void setCedulaHash(String cedulaHash) { this.cedulaHash = cedulaHash; }

    public String getCedulaEncrypted() { return cedulaEncrypted; }
    public void setCedulaEncrypted(String cedulaEncrypted) { this.cedulaEncrypted = cedulaEncrypted; }

    public String getPrimerNombreEncrypted() { return primerNombreEncrypted; }
    public void setPrimerNombreEncrypted(String primerNombreEncrypted) { this.primerNombreEncrypted = primerNombreEncrypted; }

    public String getSegundoNombreEncrypted() { return segundoNombreEncrypted; }
    public void setSegundoNombreEncrypted(String segundoNombreEncrypted) { this.segundoNombreEncrypted = segundoNombreEncrypted; }

    public String getPrimerApellidoEncrypted() { return primerApellidoEncrypted; }
    public void setPrimerApellidoEncrypted(String primerApellidoEncrypted) { this.primerApellidoEncrypted = primerApellidoEncrypted; }

    public String getSegundoApellidoEncrypted() { return segundoApellidoEncrypted; }
    public void setSegundoApellidoEncrypted(String segundoApellidoEncrypted) { this.segundoApellidoEncrypted = segundoApellidoEncrypted; }

    public String getNombreCompletoHash() { return nombreCompletoHash; }
    public void setNombreCompletoHash(String nombreCompletoHash) { this.nombreCompletoHash = nombreCompletoHash; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getPrimerNombre() { return primerNombre; }
    public void setPrimerNombre(String primerNombre) { this.primerNombre = primerNombre; }

    public String getSegundoNombre() { return segundoNombre; }
    public void setSegundoNombre(String segundoNombre) { this.segundoNombre = segundoNombre; }

    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }

    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }
}
