package com.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad JPA para auditoría de eventos del sistema.
 *
 * <p>Esta tabla registra acciones relevantes y trazabilidad operacional, sin almacenar información
 * sensible en texto plano. Su objetivo es permitir diagnóstico, cumplimiento y análisis forense.</p>
 *
 * <h2>Principios de seguridad</h2>
 * <ul>
 *   <li>No almacenar PII (cédula, nombres, correo, teléfono) en texto plano.</li>
 *   <li>No almacenar credenciales, tokens, JWT, ni secretos de sesión.</li>
 *   <li>En campos de texto libre se debe escribir información genérica, sin datos identificables.</li>
 *   <li>El campo {@code details} es JSONB para información estructurada y controlada.</li>
 * </ul>
 *
 * <h2>Modelo de trazabilidad</h2>
 * <ul>
 *   <li>{@code requestId} y {@code correlationId} permiten agrupar eventos de una misma petición o flujo.</li>
 *   <li>{@code actorStaffId} se usa cuando el actor es personal interno (staff).</li>
 *   <li>{@code trackingId} referencia una denuncia sin revelar identidad.</li>
 * </ul>
 */
@Entity
@Table(name = "evento_auditoria", schema = "logs")
public class AuditEvent {

    /** Identificador del evento */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Momento exacto del evento */
    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    /** Identificador único de la petición (ideal para logging distribuido) */
    @Column(name = "request_id")
    private UUID requestId;

    /** Identificador para correlacionar múltiples peticiones dentro de un mismo flujo */
    @Column(name = "correlation_id")
    private UUID correlationId;

    /**
     * Rol del actor.
     * Ejemplos: ANON, STAFF, ADMIN, SYSTEM.
     */
    @Column(name = "actor_role", length = 50)
    private String actorRole;

    /**
     * Identificador del actor interno (staff), cuando aplica.
     * No debe usarse para identidad de denunciante.
     */
    @Column(name = "actor_staff_id")
    private Long actorStaffId;

    /**
     * Identificador no sensible del actor (por ejemplo hash o username interno).
     * No almacenar correos ni nombres reales.
     */
    @Column(name = "actor_username", length = 200)
    private String actorUsername;

    /** Dirección IP de origen (almacenada en DB como inet) */
    @Column(name = "ip_address", columnDefinition = "inet")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.INET)
    private String ipAddress;

    /** User-Agent del cliente */
    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    /** Método HTTP (GET/POST/PUT/DELETE) si aplica */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /** Ruta del endpoint, sin incluir datos sensibles en query strings */
    @Column(name = "path", columnDefinition = "text")
    private String path;

    /** Código de respuesta HTTP si aplica */
    @Column(name = "status_code")
    private Integer statusCode;

    /** Tiempo de respuesta (ms) si aplica */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * Tipo de evento.
     * Ejemplos: LOGIN_SUCCESS, LOGIN_FAILED, COMPLAINT_SUBMITTED, EVIDENCE_UPLOADED, STAFF_CREATED.
     */
    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    /** Resultado del evento. Ejemplos: SUCCESS, FAIL. */
    @Column(name = "outcome", length = 50)
    private String outcome;

    /** TrackingId de la denuncia asociada (sin exponer identidad) */
    @Column(name = "tracking_id", length = 64)
    private String trackingId;

    /** Tipo de entidad afectada (complaint, evidence, staff_user, rule, etc.) */
    @Column(name = "entity_type", length = 100)
    private String entityType;

    /** Id de la entidad afectada (si aplica) */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Detalles estructurados del evento (JSON).
     *
     * <p>Recomendación: guardar un JSON pequeño con claves controladas.
     * No incluir PII ni secretos.</p>
     * <p>Nota: Se almacena como TEXT para evitar problemas de conversión.</p>
     */
    @Column(name = "details", nullable = false, columnDefinition = "TEXT")
    private String details = "{}";

    public Long getId() { return id; }

    public OffsetDateTime getEventTime() { return eventTime; }
    public void setEventTime(OffsetDateTime eventTime) { this.eventTime = eventTime; }

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public Long getActorStaffId() { return actorStaffId; }
    public void setActorStaffId(Long actorStaffId) { this.actorStaffId = actorStaffId; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getTrackingId() { return trackingId; }
    public void setTrackingId(String trackingId) { this.trackingId = trackingId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getDetails() { return details; }
    public void setDetails(String details) {
        this.details = (details == null || details.isBlank()) ? "{}" : details;
    }
}
