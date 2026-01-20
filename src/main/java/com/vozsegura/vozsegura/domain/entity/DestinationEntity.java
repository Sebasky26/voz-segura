package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
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
     * Codigo unico (ej: CPCCS, CPC, CIDH) - usado en DerivationRule.
     * 
     * Proposito:
     * - Identificador legible para humanos (no numerico)
     * - Usado en DerivationRule.destinationCode para routing
     * - Usado en reportes y logs
     * 
     * Restricciones:
     * - unique=true: Debe ser unico en la BD
     * - nullable=false: No puede ser nulo
     * - length=32: Maximo 32 caracteres
     * 
     * Ejemplos:
     * - "CPCCS" para Corte Penal
     * - "FISCALIA" para Fiscal General
     * - "POLICIA" para Policia Nacional
     * - "DEFENSORIA" para Defensoria Publica
     * 
     * Nota: Usado como clave en DerivationRule para evitar buscar por ID
     */
    @Column(nullable = false, unique = true, length = 32)
    private String code;

    /**
     * Nombre completo de la entidad en espanol.
     * 
     * Proposito:
     * - Mostrar en UI (dropdowns, reportes)
     * - Auditar derivaciones (registrar nombre de entidad en logs)
     * 
     * Restricciones:
     * - nullable=false: Siempre debe haber nombre
     * - length=255: Maximo 255 caracteres
     * 
     * Ejemplos:
     * - "Corte Penal y de Crimenes contra la Seguridad del Estado"
     * - "Fiscal General del Estado"
     * - "Policia Nacional del Ecuador"
     * - "Defensoria Publica"
     * - "Comision Interamericana de Derechos Humanos"
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Descripcion: areas de competencia, criterios de derivacion.
     * 
     * Proposito:
     * - Documentar areas en que la entidad acepta denuncias
     * - Mostrar en UI para ayudar al usuario a elegir entidad
     * - Registrar en reportes administrativos
     * 
     * Restricciones:
     * - nullable=true: Opcional (puede no haber descripcion)
     * - length=512: Maximo 512 caracteres
     * 
     * Ejemplos de contenido:
     * - "Recibe denuncias sobre crimenes de lesa humanidad, genocidio"
     * - "Investiga delitos de corrupcion y peculado"
     * - "Protege derechos humanos; casos internacionales"
     * - "Asiste a victimas de violaciones de derechos"
     */
    @Column(length = 512)
    private String description;

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
    private OffsetDateTime createdAt;

    // Getters y Setters

    /**
     * Obtiene el ID unico de la entidad destino.
     * @return Long: ID (ej: 1, 2, 3)
     */
    public Long getId() { return id; }

    /**
     * Establece el ID unico.
     * @param id ID a establecer (usualmente generado por BD)
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Obtiene el codigo unico de la entidad.
     * @return String: Codigo (ej: CPCCS, FISCALIA)
     */
    public String getCode() { return code; }

    /**
     * Establece el codigo unico.
     * @param code Codigo a establecer (ej: CPCCS)
     */
    public void setCode(String code) { this.code = code; }

    /**
     * Obtiene el nombre completo de la entidad.
     * @return String: Nombre completo
     */
    public String getName() { return name; }

    /**
     * Establece el nombre completo.
     * @param name Nombre a establecer
     */
    public void setName(String name) { this.name = name; }

    /**
     * Obtiene la descripcion de la entidad.
     * @return String: Descripcion (puede ser nulo)
     */
    public String getDescription() { return description; }

    /**
     * Establece la descripcion.
     * @param description Descripcion a establecer
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Obtiene el estado activo de la entidad.
     * @return boolean: true si activa, false si inactiva
     */
    public boolean isActive() { return active; }

    /**
     * Establece el estado activo.
     * @param active true si activa, false si inactiva
     */
    public void setActive(boolean active) { this.active = active; }

    /**
     * Obtiene el timestamp de creacion.
     * @return OffsetDateTime: Cuando se creo la entidad (UTC)
     */
    public OffsetDateTime getCreatedAt() { return createdAt; }

    /**
     * Establece el timestamp de creacion.
     * @param createdAt Timestamp a establecer (usualmente System.now())
     */
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
