package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.domain.entity.AuditEvent;
import com.vozsegura.vozsegura.repo.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * Servicio de auditoría.
 * Registra eventos sin incluir datos personales (cédula, biometría, etc).
 * Utiliza IDs hasheados para identificar usuarios de forma segura.
 */
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Registra un evento de auditoría.
     * IMPORTANTE: nunca pasar datos personales en los parámetros.
     * Este método NUNCA debe lanzar excepciones para no afectar el flujo principal.
     *
     * @param actorRole rol del actor (ADMIN, ANALYST, SYSTEM, PUBLIC)
     * @param actorUsername username del actor (null si es público/sistema)
     * @param eventType tipo de evento
     * @param trackingId tracking de la denuncia (si aplica)
     * @param details detalles adicionales (sin PII)
     */
    @Transactional
    public void logEvent(String actorRole, String actorUsername, String eventType, String trackingId, String details) {
        try {
            AuditEvent event = new AuditEvent();
            event.setEventTime(OffsetDateTime.now());
            event.setActorRole(actorRole != null ? actorRole : "UNKNOWN");
            // Usar hash corto del username si existe, para no exponer datos sensibles
            event.setActorUsername(actorUsername != null ? hashShort(actorUsername) : null);
            event.setEventType(eventType != null ? eventType : "UNKNOWN");
            event.setTrackingId(trackingId);
            event.setDetails(truncate(details, 500));
            auditEventRepository.save(event);
        } catch (Exception e) {
            // Log silencioso - nunca propagar error de auditoría
            System.err.println("[AUDIT] Error registrando evento: " + e.getMessage());
        }
    }

    /**
     * Registra un evento de auditoría con ID de sesión.
     * Este método NUNCA debe lanzar excepciones para no afectar el flujo principal.
     */
    @Transactional
    public void logEventWithSession(String actorRole, String sessionId, String cedula,
                                     String eventType, String trackingId, String details) {
        try {
            AuditEvent event = new AuditEvent();
            event.setEventTime(OffsetDateTime.now());
            event.setActorRole(actorRole != null ? actorRole : "UNKNOWN");
            // Generar identificador único hasheado que combina sesión y cédula
            String hashedId = generateSecureUserId(sessionId, cedula);
            event.setActorUsername(hashedId);
            event.setEventType(eventType != null ? eventType : "UNKNOWN");
            event.setTrackingId(trackingId);
            event.setDetails(truncate(details, 500));
            auditEventRepository.save(event);
        } catch (Exception e) {
            // Log silencioso - nunca propagar error de auditoría
            System.err.println("[AUDIT] Error registrando evento: " + e.getMessage());
        }
    }

    /**
     * Genera un ID de usuario seguro (hash corto) para los logs.
     * Combina sessionId y cédula para crear un identificador único no reversible.
     */
    public String generateSecureUserId(String sessionId, String cedula) {
        if (sessionId == null && cedula == null) {
            return "ANON-" + System.currentTimeMillis() % 100000;
        }

        String combined = (sessionId != null ? sessionId : "") + ":" + (cedula != null ? cedula : "");
        return hashShort(combined);
    }

    /**
     * Genera un hash corto (8 caracteres) de un string.
     * Suficiente para identificación en logs sin exponer datos.
     */
    private String hashShort(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String b64 = Base64.getEncoder().encodeToString(hash);
            // Tomar solo los primeros 8 caracteres del hash
            return "USR-" + b64.substring(0, 8).replaceAll("[+/=]", "X");
        } catch (Exception e) {
            return "USR-" + input.hashCode();
        }
    }

    /**
     * Lista eventos de auditoría paginados.
     */
    public Page<AuditEvent> findAll(Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}

