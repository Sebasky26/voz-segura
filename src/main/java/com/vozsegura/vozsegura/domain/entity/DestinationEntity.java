package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;

/**
 * Entidad receptora de denuncias derivadas (schema reglas_derivacion.entidad_destino)
 * 
 * Instituciones públicas o privadas que reciben denuncias clasificadas:
 * - Ministerio del Trabajo
 * - Defensoría del Pueblo
 * - Organismos de control
 * - etc.
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Entity
@Table(name = "entidad_destino", schema = "reglas_derivacion")
public class DestinationEntity {

    /**
     * ID unico de entidad destino.
     * 
     * Tipo: AUTO_INCREMENT (generado por BD)
     * Proposito: Clave primaria para identificar la entidad
     * 
     * Ejemplo: 1, 2, 3, ...
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre completo de la entidad en español.
     */
    @Column(nullable = false, length = 255, unique = true)
    private String name;


    /**
     * Email de contacto de la entidad.
     */
    @Column(length = 255)
    private String email;

    /**
     * Teléfono de contacto.
     */
    @Column(length = 20)
    private String phone;

    /**
     * Dirección física de la entidad.
     */
    @Column(columnDefinition = "TEXT")
    private String address;

    /**
     * Soft-delete: true = activa (recibe derivaciones), false = inactiva.
     * 
     * Proposito:
     * - Inactivar entidades sin borrar registros historicos
     * - No se borra NUNCA (mantiene auditoria)
     * - Si una entidad no acepta mas derivaciones: active=false
     * 
     * Default:
     * - true: Nueva entidad agregada se asume activa
     * 
     * Flujo:
     * 1. Admin agrega entidad: active=true (por defecto)
     * 2. Entidad empieza a recibir derivaciones
     * 3. Entidad comunica que no acepta mas casos: Admin pone active=false
     * 4. DerivationRule ya no la considera en matching
     * 5. Registro NUNCA se borra (auditoria)
     * 
     * Restricciones:
     * - nullable=false: Siempre debe haber valor (true o false)
     */
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Timestamp de creacion (auditoria).
     * 
     * Proposito:
     * - Registrar cuando se agrego la entidad al sistema
     * - Auditar cambios de configuracion
     * 
     * Tipo: OffsetDateTime (incluye timezone)
     * Timezone: UTC (se asume que BD es UTC)
     * 
     * Nota:
     * - Se establece ANTES de guardar (ver @PrePersist si es necesario)
     * - NUNCA se modifica tras creacion
     * - Es read-only para usuarios
     */
    @Column(name = "created_at")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
