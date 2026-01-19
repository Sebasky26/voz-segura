package com.vozsegura.vozsegura.dto;

import java.time.OffsetDateTime;

/**
 * DTO para mostrar el estado de una denuncia al denunciante.
 * Solo contiene información no sensible.
 */
public class ComplaintStatusDto {

    private String trackingId;
    private String status;
    private String statusLabel;
    private String severity;
    private String severityLabel;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private int evidenceCount;
    private String derivedTo;          // Entidad a la que fue derivado
    private String analystNotes;       // Notas del analista (para mostrar motivo)
    private boolean requiresMoreInfo;  // Si requiere más información
    private String complaintType;      // Tipo de denuncia (código)
    private String complaintTypeLabel; // Tipo de denuncia (etiqueta en español)

    public ComplaintStatusDto() {}

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

    private String translateStatus(String status) {
        if (status == null) return "Desconocido";
        return switch (status) {
            case "PENDING" -> "Pendiente de revisión";
            case "IN_REVIEW" -> "En revisión";
            case "NEEDS_INFO" -> "Requiere información adicional";
            case "APPROVED" -> "Aprobado";
            case "REJECTED" -> "No procede";
            case "DERIVED" -> "Derivado a autoridad competente";
            case "RESOLVED" -> "Resuelto";
            case "ARCHIVED" -> "Archivado";
            default -> status;
        };
    }

    private String translateSeverity(String severity) {
        if (severity == null) return "No definida";
        return switch (severity) {
            case "LOW" -> "Baja";
            case "MEDIUM" -> "Media";
            case "HIGH" -> "Alta";
            case "CRITICAL" -> "Crítica";
            default -> severity;
        };
    }

    private String translateComplaintType(String type) {
        if (type == null) return "No clasificado";
        return switch (type) {
            case "LABOR_RIGHTS" -> "Derechos Laborales";
            case "HARASSMENT" -> "Acoso Laboral";
            case "DISCRIMINATION" -> "Discriminación";
            case "SAFETY" -> "Seguridad Laboral";
            case "FRAUD" -> "Fraude";
            case "OTHER" -> "Otro";
            default -> type;
        };
    }

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
