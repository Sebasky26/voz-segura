package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad receptora de denuncias derivadas almacenada en {@code reglas_derivacion.entidad_destino}.
 *
 * <p>Representa instituciones (públicas o privadas) que pueden recibir casos derivados.
 * La información de contacto se almacena cifrada para reducir exposición en caso de fuga de datos.</p>
 *
 * <h2>Reglas de seguridad</h2>
 * <ul>
 *   <li>Los campos de contacto se guardan únicamente en columnas {@code *_encrypted}.</li>
 *   <li>La aplicación debe cifrar antes de persistir y descifrar solo cuando sea necesario.</li>
 * </ul>
 *
 * <h2>Identificación</h2>
 * <ul>
 *   <li>{@code code} es el identificador único estable (útil para integraciones y configuración).</li>
 *   <li>{@code name} es el nombre visible y puede cambiar con el tiempo.</li>
 * </ul>
 */
@Entity
@Table(name = "entidad_destino", schema = "reglas_derivacion")
public class DestinationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único y estable de la entidad destino.
     * Ejemplos: "MIN_TRABAJO", "DEFENSORIA", "FISCALIA".
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /** Nombre visible de la entidad. */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Descripción administrativa (opcional). */
    @Column(name = "description", length = 255)
    private String description;

    /** Entidad activa/inactiva para nuevas derivaciones. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Correo de contacto cifrado (texto cifrado, por ejemplo AES-GCM en Base64). */
    @Column(name = "email_encrypted", columnDefinition = "text")
    private String emailEncrypted;

    /** Teléfono de contacto cifrado (texto cifrado, por ejemplo AES-GCM en Base64). */
    @Column(name = "phone_encrypted", columnDefinition = "text")
    private String phoneEncrypted;

    /** Dirección cifrada (texto cifrado, por ejemplo AES-GCM en Base64). */
    @Column(name = "address_encrypted", columnDefinition = "text")
    private String addressEncrypted;

    /**
     * Endpoint para notificación/integración (si aplica).
     * Puede ser null si la derivación se gestiona por otro canal.
     */
    @Column(name = "endpoint_url", columnDefinition = "text")
    private String endpointUrl;

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

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getEmailEncrypted() { return emailEncrypted; }
    public void setEmailEncrypted(String emailEncrypted) { this.emailEncrypted = emailEncrypted; }

    public String getPhoneEncrypted() { return phoneEncrypted; }
    public void setPhoneEncrypted(String phoneEncrypted) { this.phoneEncrypted = phoneEncrypted; }

    public String getAddressEncrypted() { return addressEncrypted; }
    public void setAddressEncrypted(String addressEncrypted) { this.addressEncrypted = addressEncrypted; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
