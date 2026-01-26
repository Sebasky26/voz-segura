package com.vozsegura.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vozsegura.domain.entity.AuditEvent;
import com.vozsegura.repo.AuditEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate txNew;

    public AuditService(AuditEventRepository auditEventRepository, PlatformTransactionManager txManager) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = new ObjectMapper();

        this.txNew = new TransactionTemplate(txManager);
        this.txNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void logEvent(String actorRole, String actorUsername, String eventType, String trackingId, String details) {
        try {
            txNew.execute(status -> {
                try {
                    AuditEvent event = baseEvent();
                    event.setActorRole(actorRole != null ? actorRole : "UNKNOWN");
                    event.setActorUsername(actorUsername != null ? hashShort(actorUsername) : null);
                    event.setEventType(eventType != null ? eventType : "UNKNOWN");
                    event.setTrackingId(trackingId);

                    // JSON válido siempre
                    event.setDetails(toJsonOrEmpty(Map.of("message", truncate(details, 500))));

                    auditEventRepository.saveAndFlush(event);
                } catch (Exception inner) {
                    log.warn("Audit logEvent failed: {}", inner.getMessage());
                }
                return null;
            });
        } catch (Exception ignored) { }
    }

    public void logEventWithSession(String actorRole, String sessionId, String cedula,
                                    String eventType, String trackingId, String details) {
        try {
            txNew.execute(status -> {
                try {
                    AuditEvent event = baseEvent();
                    event.setActorRole(actorRole != null ? actorRole : "UNKNOWN");
                    event.setActorUsername(generateSecureUserId(sessionId, cedula));
                    event.setEventType(eventType != null ? eventType : "UNKNOWN");
                    event.setTrackingId(trackingId);

                    event.setDetails(toJsonOrEmpty(Map.of("message", truncate(details, 500))));

                    auditEventRepository.saveAndFlush(event);
                } catch (Exception inner) {
                    log.warn("Audit logEventWithSession failed: {}", inner.getMessage());
                }
                return null;
            });
        } catch (Exception ignored) { }
    }

    public void logSecurityEvent(
            String eventType,
            String outcome,
            Long actorStaffId,
            String actorUsername,
            String ipAddress,
            String userAgent,
            Map<String, Object> details
    ) {
        try {
            txNew.execute(status -> {
                try {
                    AuditEvent event = baseEvent();

                    event.setActorStaffId(actorStaffId);
                    event.setActorRole(actorStaffId != null ? "STAFF" : "ANON");

                    // Si quieres 0-PII estricto, hashea; si no, déjalo como está.
                    event.setActorUsername(truncate(actorUsername, 200));

                    event.setIpAddress(sanitizeIp(ipAddress));
                    event.setUserAgent(truncate(userAgent, 500));

                    event.setEventType(eventType != null ? eventType : "UNKNOWN");
                    event.setOutcome(outcome != null ? outcome : "UNKNOWN");

                    event.setDetails(toJsonOrEmpty(details));

                    auditEventRepository.saveAndFlush(event);
                } catch (Exception inner) {
                    log.warn("Audit logSecurityEvent failed: {}", inner.getMessage());
                }
                return null;
            });
        } catch (Exception ignored) { }
    }

    public void logComplaintCreated(String trackingId) {
        try {
            txNew.execute(status -> {
                try {
                    AuditEvent event = baseEvent();
                    event.setActorRole("USER");
                    event.setEventType("COMPLAINT_CREATED");
                    event.setOutcome("SUCCESS");
                    event.setTrackingId(trackingId);
                    event.setEntityType("Complaint");
                    event.setDetails("{}");
                    auditEventRepository.saveAndFlush(event);
                } catch (Exception inner) {
                    log.warn("Audit logComplaintCreated failed: {}", inner.getMessage());
                }
                return null;
            });
        } catch (Exception ignored) { }
    }

    public void logComplaintAccess(String trackingId) {
        try {
            txNew.execute(status -> {
                try {
                    AuditEvent event = baseEvent();
                    event.setActorRole("USER");
                    event.setEventType("COMPLAINT_ACCESSED");
                    event.setOutcome("SUCCESS");
                    event.setTrackingId(trackingId);
                    event.setDetails("{}");
                    auditEventRepository.saveAndFlush(event);
                } catch (Exception inner) {
                    log.warn("Audit logComplaintAccess failed: {}", inner.getMessage());
                }
                return null;
            });
        } catch (Exception ignored) { }
    }

    public void logLogout(String actorRole, String username) {
        try {
            txNew.execute(status -> {
                try {
                    AuditEvent event = baseEvent();
                    event.setActorRole(actorRole != null ? actorRole : "UNKNOWN");
                    event.setActorUsername(username != null ? hashShort(username) : null);
                    event.setEventType("LOGOUT");
                    event.setOutcome("SUCCESS");
                    event.setDetails("{}");
                    auditEventRepository.saveAndFlush(event);
                } catch (Exception inner) {
                    log.warn("Audit logLogout failed: {}", inner.getMessage());
                }
                return null;
            });
        } catch (Exception ignored) { }
    }

    public Page<AuditEvent> findAll(Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }

    // ---------------- helpers ----------------

    private AuditEvent baseEvent() {
        AuditEvent event = new AuditEvent();
        event.setEventTime(OffsetDateTime.now());
        event.setRequestId(UUID.randomUUID());
        event.setDetails("{}");
        return event;
    }

    public String generateSecureUserId(String sessionId, String cedula) {
        if (sessionId == null && cedula == null) {
            return "ANON-" + (System.currentTimeMillis() % 100000);
        }
        String combined = (sessionId != null ? sessionId : "") + ":" + (cedula != null ? cedula : "");
        return hashShort(combined);
    }

    private String hashShort(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            String b64 = Base64.getEncoder().encodeToString(hash);
            return "USR-" + b64.substring(0, 8).replaceAll("[+/=]", "X");
        } catch (Exception e) {
            return "USR-" + input.hashCode();
        }
    }

    private String sanitizeIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) return null;

        String sanitized = ip.trim();
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (sanitized.matches(ipv4Pattern)) return sanitized;

        if (sanitized.contains(":") && sanitized.length() <= 45) return sanitized;

        return null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private String toJsonOrEmpty(Map<String, Object> details) {
        try {
            if (details == null || details.isEmpty()) return "{}";
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            return "{}";
        }
    }
}
