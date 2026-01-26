package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Regla de derivación automática almacenada en {@code reglas_derivacion.regla_derivacion}.
 *
 * <p>Una regla pertenece a una política ({@code politica_derivacion}) para mantener trazabilidad
 * por normativa (versión, vigencia, aprobación).</p>
 *
 * <p>La regla puede filtrar por criterios básicos (tipo y severidad) y criterios avanzados
 * mediante {@code conditions} (JSONB). Este campo permite incorporar requisitos adicionales
 * sin romper el modelo (por ejemplo: palabras clave, evidencia obligatoria, región, etc.).</p>
 */
@Entity
@Table(name = "regla_derivacion", schema = "reglas_derivacion")
public class DerivationRule {

    /** Identificador interno de la regla. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Política a la que pertenece la regla (NOT NULL en BD). */
    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    /** Nombre descriptivo de la regla. */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /** Descripción administrativa de la regla. */
    @Column(name = "description")
    private String description;

    /** Regla activa/inactiva. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Filtro simple por tipo de denuncia (puede ser null para cualquier tipo). */
    @Column(name = "complaint_type_match", length = 100)
    private String complaintTypeMatch;

    /** Filtro simple por severidad (puede ser null para cualquier severidad). */
    @Column(name = "severity_match", length = 20)
    private String severityMatch;

    /**
     * Condiciones avanzadas en formato JSON.
     *
     * <p>Recomendación: mantener un JSON pequeño con claves definidas por la aplicación.</p>
     * <p>Nota: Se almacena como TEXT en lugar de JSONB para evitar problemas de conversión.</p>
     */
    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions = "{}";

    /** Entidad destino a la que se deriva si la regla aplica (FK). */
    @Column(name = "destination_id", nullable = false)
    private Long destinationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", insertable = false, updatable = false)
    private DestinationEntity destinationEntity;

    /** Orden de prioridad: menor valor significa mayor prioridad. */
    @Column(name = "priority_order", nullable = false)
    private Integer priorityOrder = 100;

    /** Indica si la regla requiere revisión manual antes de derivar. */
    @Column(name = "requires_manual_review", nullable = false)
    private boolean requiresManualReview = false;

    /** SLA sugerido para atención/derivación (en horas). */
    @Column(name = "sla_hours")
    private Integer slaHours;

    /** Referencia normativa (artículo, resolución, etc.). */
    @Column(name = "normative_reference")
    private String normativeReference;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = (this.createdAt == null) ? now : this.createdAt;
        this.updatedAt = (this.updatedAt == null) ? now : this.updatedAt;

        if (this.conditions == null || this.conditions.isBlank()) {
            this.conditions = "{}";
        }
        if (this.priorityOrder == null) {
            this.priorityOrder = 100;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
        if (this.conditions == null || this.conditions.isBlank()) {
            this.conditions = "{}";
        }
    }

    // Getters y setters
    public Long getId() { return id; }

    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long policyId) { this.policyId = policyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getComplaintTypeMatch() { return complaintTypeMatch; }
    public void setComplaintTypeMatch(String complaintTypeMatch) { this.complaintTypeMatch = complaintTypeMatch; }

    public String getSeverityMatch() { return severityMatch; }
    public void setSeverityMatch(String severityMatch) { this.severityMatch = severityMatch; }

    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }

    public Long getDestinationId() { return destinationId; }
    public void setDestinationId(Long destinationId) { this.destinationId = destinationId; }

    public DestinationEntity getDestinationEntity() { return destinationEntity; }
    public void setDestinationEntity(DestinationEntity destinationEntity) { this.destinationEntity = destinationEntity; }

    public Integer getPriorityOrder() { return priorityOrder; }
    public void setPriorityOrder(Integer priorityOrder) { this.priorityOrder = priorityOrder; }

    public boolean isRequiresManualReview() { return requiresManualReview; }
    public void setRequiresManualReview(boolean requiresManualReview) { this.requiresManualReview = requiresManualReview; }

    public Integer getSlaHours() { return slaHours; }
    public void setSlaHours(Integer slaHours) { this.slaHours = slaHours; }

    public String getNormativeReference() { return normativeReference; }
    public void setNormativeReference(String normativeReference) { this.normativeReference = normativeReference; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
