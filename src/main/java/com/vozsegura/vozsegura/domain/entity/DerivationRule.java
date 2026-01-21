package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Regla de derivación automática (schema reglas_derivacion.regla_derivacion)
 * 
 * Define cómo se distribuyen las denuncias entre entidades:
 * - Coincide por severidad (LOW, MEDIUM, HIGH, CRITICAL o null = cualquiera)
 * - Especifica entidad_destino a la que se derive
 * 
 * Algoritmo: Busca regla más específica que coincida con la severidad
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Entity
@Table(name = "regla_derivacion", schema = "reglas_derivacion")
public class DerivationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /** ID único de la regla */
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    /** Nombre único descriptivo de la regla (ej: "AcosoLaboralCritico") */
    private String name;

    @Column(name = "severity_match", length = 32)
    /** Severidad a coincidir: LOW, MEDIUM, HIGH, CRITICAL (NULL = cualquier severidad) */
    private String severityMatch;

    @Column(name = "complaint_type_match", length = 64)
    /** Tipo de denuncia a coincidir: LABOR_RIGHTS, HARASSMENT, DISCRIMINATION, SAFETY, FRAUD, OTHER (NULL = cualquier tipo) */
    private String complaintTypeMatch;

    @Column(name = "destination_id")
    /** ID de la institución destino (DestinationEntity) */
    private Long destinationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", insertable = false, updatable = false)
    /** Referencia a la institución destino (relación lazy) */
    private DestinationEntity destinationEntity;

    @Column(name = "description", length = 512)
    /** Descripción legible de la regla (para administración) */
    private String description;

    @Column(name = "active", nullable = false)
    /** Flag: regla activa (true) o inactiva/soft-deleted (false) */
    private boolean active = true;

    @Column(name = "created_at")
    /** Timestamp de creación (UTC) */
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    /** Timestamp de última modificación (UTC) */
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSeverityMatch() { return severityMatch; }
    public void setSeverityMatch(String severityMatch) { this.severityMatch = severityMatch; }

    public String getComplaintTypeMatch() { return complaintTypeMatch; }
    public void setComplaintTypeMatch(String complaintTypeMatch) { this.complaintTypeMatch = complaintTypeMatch; }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }

    public DestinationEntity getDestinationEntity() { return destinationEntity; }
    public void setDestinationEntity(DestinationEntity destinationEntity) { this.destinationEntity = destinationEntity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
