package com.vozsegura.vozsegura.dto;

import java.time.OffsetDateTime;

/**
 * DTO para mostrar el estado de una denuncia al denunciante.
 * 
 * Proposito:
 * - Retornar informacion sobre el estado de una denuncia
 * - SOLO datos no sensibles (SIN cifrado, SIN PII)
 * - Usado en endpoint publico para que ciudadano verifique su denuncia
 * 
 * Seguridad:
 * - NO contiene: cedula, email, telefono, nombre completo, texto de denuncia
 * - SI contiene: trackingId (unico), estado, severidad, cantidad de evidencias
 * - Traduccion: Estados y severidades en espanol para UI
 * 
 * Flujo:
 * 1. Ciudadano accede a /tracking?trackingId=ABC123
 * 2. TrackingController consulta BD por trackingId
 * 3. Si encontrada, retorna ComplaintStatusDto (no la denuncia completa)
 * 4. Cliente la renderiza en HTML con informacion traducida
 * 
 * Campos:
 * - trackingId: Codigo unico (40 caracteres) para buscar denuncia
 * - status: PENDING, IN_REVIEW, NEEDS_INFO, APPROVED, REJECTED, DERIVED, RESOLVED, ARCHIVED
 * - statusLabel: Traduccion del status al espanol
 * - severity: LOW, MEDIUM, HIGH, CRITICAL
 * - severityLabel: Traduccion de severidad
 * - createdAt/updatedAt: Timestamps
 * - evidenceCount: Numero de archivos adjuntos
 * - derivedTo: Nombre de entidad a la que fue derivada (si aplica)
 * - analystNotes: Notas publicas del analista (por que se rechazo, etc.)
 * - requiresMoreInfo: Si necesita informacion adicional
 * - complaintType: Codigo del tipo (LABOR_RIGHTS, HARASSMENT, etc.)
 * - complaintTypeLabel: Etiqueta en espanol
 * 
 * @see com.vozsegura.vozsegura.controller.publicview.TrackingController
 */
public class ComplaintStatusDto {

    /**
     * Identificador unico para consultar la denuncia.
     * 40 caracteres aleatorios (sin sensibilidad a mayusculas).
     * Ejemplo: "ABCD1234EFGH5678IJKL9012MNOP3456QRST7890"
     */
    private String trackingId;

    /**
     * Estado actual de la denuncia (codigo).
     * Valores: PENDING, IN_REVIEW, NEEDS_INFO, APPROVED, REJECTED, DERIVED, RESOLVED, ARCHIVED
     */
    private String status;

    /**
     * Traduccion del estado al espanol (para mostrar en UI).
     * Ejemplo: "PENDING" -> "Pendiente de revision"
     */
    private String statusLabel;

    /**
     * Severidad de la denuncia (codigo).
     * Valores: LOW, MEDIUM, HIGH, CRITICAL
     */
    private String severity;

    /**
     * Traduccion de severidad al espanol.
     * Ejemplo: "CRITICAL" -> "Critica"
     */
    private String severityLabel;

    /**
     * Timestamp de creacion de la denuncia (UTC).
     */
    private OffsetDateTime createdAt;

    /**
     * Timestamp de ultima modificacion (UTC).
     */
    private OffsetDateTime updatedAt;

    /**
     * Numero de archivos de evidencia adjuntos a la denuncia.
     */
    private int evidenceCount;

    /**
     * Nombre de la entidad a la que fue derivada la denuncia.
     * Ejemplo: "Fiscal General del Estado", "Policia Nacional"
     * null si NO fue derivada
     */
    private String derivedTo;

    /**
     * Notas publicas del analista.
     * Ejemplo: "Se requiere informacion adicional sobre hechos"
     * Visible para el ciudadano.
     */
    private String analystNotes;

    /**
     * Indica si la denuncia requiere informacion adicional.
     * Si true: ciudadano puede cargar mas evidencias
     */
    private boolean requiresMoreInfo;

    /**
     * Tipo de denuncia (codigo tecnico).
     * Ejemplo: "LABOR_RIGHTS", "HARASSMENT"
     */
    private String complaintType;

    /**
     * Etiqueta del tipo de denuncia en espanol.
     * Ejemplo: "Derechos Laborales"
     */
    private String complaintTypeLabel;

    /**
     * Constructor vacio (requerido para deserializacion).
     */
    public ComplaintStatusDto() {}

    /**
     * Constructor minimo con datos basicos.
     * 
     * @param trackingId Codigo de seguimiento
     * @param status Estado de la denuncia
     * @param severity Severidad
     * @param createdAt Timestamp de creacion
     * @param updatedAt Timestamp de actualizacion
     * @param evidenceCount Numero de evidencias
     */
    public ComplaintStatusDto(String trackingId, String status, String severity,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt, int evidenceCount) {
        this.trackingId = trackingId;
        this.status = status;
        this.statusLabel = translateStatus(status);
        this.severity = severity;
        this.severityLabel = translateSeverity(severity);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.evidenceCount = evidenceCount;
    }

    /**
     * Constructor completo con todos los campos.
     * 
     * @param trackingId Codigo de seguimiento
     * @param status Estado
     * @param severity Severidad
     * @param createdAt Creacion
     * @param updatedAt Actualizacion
     * @param evidenceCount Evidencias
     * @param derivedTo Entidad derivada
     * @param analystNotes Notas del analista
     * @param requiresMoreInfo Si requiere mas info
     * @param complaintType Tipo de denuncia
     */
    public ComplaintStatusDto(String trackingId, String status, String severity,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt, int evidenceCount,
                               String derivedTo, String analystNotes, boolean requiresMoreInfo, String complaintType) {
        this(trackingId, status, severity, createdAt, updatedAt, evidenceCount);
        this.derivedTo = derivedTo;
        this.analystNotes = analystNotes;
        this.requiresMoreInfo = requiresMoreInfo;
        this.complaintType = complaintType;
        this.complaintTypeLabel = translateComplaintType(complaintType);
    }

    /**
     * Traduce codigo de estado a etiqueta en espanol.
     * 
     * @param status Codigo de estado (ej: "PENDING")
     * @return Etiqueta traducida (ej: "Pendiente de revision")
     */
    private String translateStatus(String status) {
        if (status == null) return "Desconocido";
        return switch (status) {
            case "PENDING" -> "Pendiente de revision";
            case "IN_REVIEW" -> "En revision";
            case "NEEDS_INFO" -> "Requiere informacion adicional";
            case "APPROVED" -> "Aprobado";
            case "REJECTED" -> "No procede";
            case "DERIVED" -> "Derivado a autoridad competente";
            case "RESOLVED" -> "Resuelto";
            case "ARCHIVED" -> "Archivado";
            default -> status;
        };
    }

    /**
     * Traduce codigo de severidad a etiqueta en espanol.
     * 
     * @param severity Codigo (ej: "CRITICAL")
     * @return Etiqueta traducida (ej: "Critica")
     */
    private String translateSeverity(String severity) {
        if (severity == null) return "No definida";
        return switch (severity) {
            case "LOW" -> "Baja";
            case "MEDIUM" -> "Media";
            case "HIGH" -> "Alta";
            case "CRITICAL" -> "Critica";
            default -> severity;
        };
    }

    /**
     * Traduce codigo de tipo de denuncia a etiqueta en espanol.
     * 
     * @param type Codigo (ej: "LABOR_RIGHTS")
     * @return Etiqueta traducida (ej: "Derechos Laborales")
     */
    private String translateComplaintType(String type) {
        if (type == null) return "No clasificado";
        return switch (type) {
            case "LABOR_RIGHTS" -> "Derechos Laborales";
            case "HARASSMENT" -> "Acoso Laboral";
            case "DISCRIMINATION" -> "Discriminacion";
            case "SAFETY" -> "Seguridad Laboral";
            case "FRAUD" -> "Fraude";
            case "OTHER" -> "Otro";
            default -> type;
        };
    }

    // Getters y Setters

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.statusLabel = translateStatus(status); }

    public String getStatusLabel() { return statusLabel; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; this.severityLabel = translateSeverity(severity); }

    public String getSeverityLabel() { return severityLabel; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getEvidenceCount() { return evidenceCount; }
    public void setEvidenceCount(int evidenceCount) { this.evidenceCount = evidenceCount; }

    public String getDerivedTo() { return derivedTo; }
    public void setDerivedTo(String derivedTo) { this.derivedTo = derivedTo; }

    public String getAnalystNotes() { return analystNotes; }
    public void setAnalystNotes(String analystNotes) { this.analystNotes = analystNotes; }

    public boolean isRequiresMoreInfo() { return requiresMoreInfo; }
    public void setRequiresMoreInfo(boolean requiresMoreInfo) { this.requiresMoreInfo = requiresMoreInfo; }

    public String getComplaintType() { return complaintType; }
    public void setComplaintType(String complaintType) { 
        this.complaintType = complaintType; 
        this.complaintTypeLabel = translateComplaintType(complaintType);
    }

    public String getComplaintTypeLabel() { return complaintTypeLabel; }
}
