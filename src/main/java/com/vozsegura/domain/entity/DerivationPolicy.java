package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Politica de derivacion almacenada en reglas_derivacion.politica_derivacion.
 *
 * Tabla: reglas_derivacion.politica_derivacion
 *
 * Una politica agrupa reglas de derivacion bajo un marco normativo y temporal especifico.
 * Permite versionar las reglas segun cambios en la legislacion o procedimientos internos.
 *
 * Campos clave:
 * - version: Identificador unico de version (ej: "v1.0", "2026-Q1")
 * - effective_from/to: Rango de vigencia temporal
 * - active: Flag para activar/desactivar rapidamente sin eliminar
 * - approved_by: Auditoría de quien aprobo esta politica
 *
 * Seguridad:
 * - Solo ADMIN puede crear, editar o aprobar politicas
 * - Las politicas inactivas no se usan para matching de reglas
 *
 * Auditoria:
 * - created_by: ID del staff que creo la politica
 * - approved_by: ID del staff que aprobo la politica
 * - approved_at: Timestamp de aprobacion
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Entity
@Table(name = "politica_derivacion", schema = "reglas_derivacion")
public class DerivationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "legal_framework", columnDefinition = "text")
    private String legalFramework;

    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by")
    private Long createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private StaffUser creator;

    @Column(name = "approved_by")
    private Long approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", insertable = false, updatable = false)
    private StaffUser approver;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLegalFramework() { return legalFramework; }
    public void setLegalFramework(String legalFramework) { this.legalFramework = legalFramework; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public StaffUser getCreator() { return creator; }
    public void setCreator(StaffUser creator) { this.creator = creator; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public StaffUser getApprover() { return approver; }
    public void setApprover(StaffUser approver) { this.approver = approver; }

    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
