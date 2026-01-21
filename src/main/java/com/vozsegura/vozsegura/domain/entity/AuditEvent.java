package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entidad JPA para auditoría de eventos del sistema.
 * 
 * Propósito:
 * - Registrar TODOS los eventos (login, logout, create, update, delete, access, error)
 * - Compliance legal y forensics
 * - Debugging de errores
 * - Detección de abuso/ataques
 * 
 * Estructura:
 * - eventTime: Timestamp exacto del evento (offset datetime con zona)
 * - actorRole: Rol del actor (ADMIN, ANALYST, CITIZEN, SYSTEM, UNKNOWN)
 * - actorUsername: Username hasheado o sessionHash (NO plain text)
 * - eventType: Tipo de evento (LOGIN, LOGOUT, CREATE, UPDATE, DELETE, ACCESS, ERROR, REVEAL)
 * - trackingId: ID de denuncia asociada (NULL si no aplica)
 * - details: Detalles adicionales (max 512 chars, sin PII)
 * 
 * Seguridad CERO Trust:
 * - NUNCA guardar cédula en plain text
 * - NUNCA guardar JWT/tokens
 * - NUNCA guardar biometría
 * - NUNCA guardar contraseñas
 * - SÍ: userHash (SHA-256), eventType, trackingId (anónimo), timestamp, details (generic)
 * 
 * Eventos registrados:
 * - LOGIN: Usuario hace login (actor, userAgent, IP)
 * - LOGOUT: Usuario hace logout
 * - CREATE: Crear recurso (denuncia, usuario, etc.)
 * - UPDATE: Actualizar recurso
 * - DELETE: Eliminar recurso (soft-delete)
 * - ACCESS: Acceso a recurso (read-only, no modificación)
 * - REVEAL: Solicitud de revelación de identidad
 * - ERROR: Error del sistema (exception message, sin stack trace)
 * 
 * Tabla:
 * - schema: logs (separado de datos de negocio)
 * - name: evento_auditoria
 * - index: (eventTime DESC) para queries rápidas
 * - index: (trackingId) para búsquedas por caso
 * - index: (actorRole, eventType) para filtros
 * 
 * Notas:
 * - idRegistro puede ser null (algunos eventos no tienen registro asociado)
 * - details truncado a 512 chars (evitar desbordamiento)
 * - AuditService maneja errores silenciosamente (no interrumpe flujo)
 * - En producción: exportar a syslog o AWS CloudWatch
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Entity
@Table(name = "evento_auditoria", schema = "logs")
public class AuditEvent {

    /** ID único del evento */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia de registro (puede ser null) */
    @Column(name = "id_registro")
    private Long idRegistro;

    /** Timestamp exacto del evento (con offset de zona horaria) */
    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    /** Rol del actor: ADMIN, ANALYST, CITIZEN, SYSTEM, UNKNOWN */
    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    /** Username hasheado del actor (SHA-256, nunca plain text) */
    @Column(name = "actor_username", length = 64)
    private String actorUsername;

    /** Tipo de evento: LOGIN, LOGOUT, CREATE, UPDATE, DELETE, ACCESS, ERROR, REVEAL */
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    /** ID de denuncia asociada (NULL si evento no está vinculado a caso) */
    @Column(name = "tracking_id", length = 40)
    private String trackingId;

    /** Detalles adicionales sin PII (max 512 chars, truncado automáticamente) */
    @Column(name = "details", length = 512)
    private String details;

    @Column(name = "ip_address", length = 255)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "status", length = 255)
    private String status;

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

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
