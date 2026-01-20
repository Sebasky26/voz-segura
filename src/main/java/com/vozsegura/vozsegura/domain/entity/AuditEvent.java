package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "evento_auditoria", schema = "logs")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_registro")
    private Long idRegistro;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "tracking_id", length = 40)
    private String trackingId;

    @Column(name = "details", length = 512)
    private String details;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Long idRegistro) { this.idRegistro = idRegistro; }

    public OffsetDateTime getEventTime() { return eventTime; }
    public void setEventTime(OffsetDateTime eventTime) { this.eventTime = eventTime; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
