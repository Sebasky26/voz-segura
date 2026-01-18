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
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private int evidenceCount;

    public ComplaintStatusDto() {}

    public ComplaintStatusDto(String trackingId, String status, String severity,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt, int evidenceCount) {
        this.trackingId = trackingId;
        this.status = status;
        this.statusLabel = translateStatus(status);
        this.severity = severity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.evidenceCount = evidenceCount;
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

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.statusLabel = translateStatus(status); }
    public String getStatusLabel() { return statusLabel; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getEvidenceCount() { return evidenceCount; }
    public void setEvidenceCount(int evidenceCount) { this.evidenceCount = evidenceCount; }
}
