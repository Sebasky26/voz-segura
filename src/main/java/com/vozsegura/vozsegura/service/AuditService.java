package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.domain.entity.AuditEvent;
import com.vozsegura.vozsegura.repo.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Servicio de auditoría.
 * Registra eventos sin incluir datos personales (cédula, biometría, etc).
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
     *
     * @param actorRole rol del actor (ADMIN, ANALYST, SYSTEM, PUBLIC)
     * @param actorUsername username del actor (null si es público/sistema)
     * @param eventType tipo de evento
     * @param trackingId tracking de la denuncia (si aplica)
     * @param details detalles adicionales (sin PII)
     */
    @Transactional
    public void logEvent(String actorRole, String actorUsername, String eventType, String trackingId, String details) {
        AuditEvent event = new AuditEvent();
        event.setEventTime(OffsetDateTime.now());
        event.setActorRole(actorRole);
        event.setActorUsername(actorUsername);
        event.setEventType(eventType);
        event.setTrackingId(trackingId);
        event.setDetails(truncate(details, 500));
        auditEventRepository.save(event);
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

