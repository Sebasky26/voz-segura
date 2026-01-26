package com.vozsegura.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vozsegura.domain.entity.Complaint;
import com.vozsegura.domain.entity.DerivationPolicy;
import com.vozsegura.domain.entity.DerivationRule;
import com.vozsegura.domain.entity.DestinationEntity;
import com.vozsegura.repo.ComplaintRepository;
import com.vozsegura.repo.DerivationPolicyRepository;
import com.vozsegura.repo.DerivationRuleRepository;
import com.vozsegura.repo.DestinationEntityRepository;

@Slf4j
@Service
public class DerivationService {

    private final DerivationRuleRepository ruleRepository;
    private final DerivationPolicyRepository policyRepository;
    private final ComplaintRepository complaintRepository;
    private final DestinationEntityRepository destinationEntityRepository;
    private final AuditService auditService;

    public DerivationService(DerivationRuleRepository ruleRepository,
                             DerivationPolicyRepository policyRepository,
                             ComplaintRepository complaintRepository,
                             DestinationEntityRepository destinationEntityRepository,
                             AuditService auditService) {
        this.ruleRepository = ruleRepository;
        this.policyRepository = policyRepository;
        this.complaintRepository = complaintRepository;
        this.destinationEntityRepository = destinationEntityRepository;
        this.auditService = auditService;
    }

    // =========================
    // LISTADOS
    // =========================

    /**
     * IMPORTANTE:
     * Para la vista Thymeleaf /admin/reglas, necesitamos que destinationEntity venga cargado,
     * caso contrario puede salir N/A por lazy loading.
     */
    public List<DerivationRule> findAllRules() {
        return ruleRepository.findAllWithDestinationOrderByNameAsc();
    }

    public List<DerivationRule> findActiveRules() {
        return ruleRepository.findByActiveTrue();
    }

    public List<DerivationPolicy> findActivePolicies() {
        return policyRepository.findByActiveTrueOrderByEffectiveFromDesc();
    }

    public List<DerivationPolicy> findAllPolicies() {
        return policyRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<DerivationRule> findById(Long id) {
        return ruleRepository.findById(id);
    }

    // =========================
    // CRUD REGLAS
    // =========================

    @Transactional
    public DerivationRule createRule(DerivationRule rule, String adminUsername) {
        if (rule.getPolicyId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una política.");
        }
        if (rule.getDestinationId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una entidad destino.");
        }

        policyRepository.findById(rule.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("La política seleccionada no existe."));

        destinationEntityRepository.findById(rule.getDestinationId())
                .orElseThrow(() -> new IllegalArgumentException("La entidad destino seleccionada no existe."));

        if (rule.getPriorityOrder() == null) rule.setPriorityOrder(100);
        if (rule.getConditions() == null || rule.getConditions().isBlank()) rule.setConditions("{}");

        rule.setCreatedAt(OffsetDateTime.now());
        rule.setUpdatedAt(OffsetDateTime.now());

        DerivationRule saved = ruleRepository.save(rule);

        auditService.logEvent("ADMIN", adminUsername, "RULE_CREATED", null,
                "Regla creada: " + safe(rule.getName()));

        return saved;
    }

    @Transactional
    public DerivationRule updateRule(Long id, DerivationRule updated, String adminUsername) {
        DerivationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));

        if (updated.getPolicyId() != null && !updated.getPolicyId().equals(rule.getPolicyId())) {
            policyRepository.findById(updated.getPolicyId())
                    .orElseThrow(() -> new IllegalArgumentException("La política seleccionada no existe."));
            rule.setPolicyId(updated.getPolicyId());
        }

        if (updated.getDestinationId() != null && !updated.getDestinationId().equals(rule.getDestinationId())) {
            destinationEntityRepository.findById(updated.getDestinationId())
                    .orElseThrow(() -> new IllegalArgumentException("La entidad destino seleccionada no existe."));
            rule.setDestinationId(updated.getDestinationId());
        }

        rule.setName(updated.getName());
        rule.setDescription(updated.getDescription());
        rule.setActive(updated.isActive());
        rule.setComplaintTypeMatch(updated.getComplaintTypeMatch());
        rule.setSeverityMatch(updated.getSeverityMatch());

        rule.setPriorityOrder(updated.getPriorityOrder() != null ? updated.getPriorityOrder() : rule.getPriorityOrder());
        rule.setRequiresManualReview(updated.isRequiresManualReview());
        rule.setSlaHours(updated.getSlaHours());
        rule.setNormativeReference(updated.getNormativeReference());
        rule.setConditions((updated.getConditions() == null || updated.getConditions().isBlank()) ? "{}" : updated.getConditions());

        rule.setUpdatedAt(OffsetDateTime.now());

        DerivationRule saved = ruleRepository.save(rule);

        auditService.logEvent("ADMIN", adminUsername, "RULE_UPDATED", null,
                "Regla actualizada: " + safe(rule.getName()));

        return saved;
    }

    @Transactional
    public void deleteRule(Long id, String adminUsername) {
        DerivationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));

        rule.setActive(false);
        rule.setUpdatedAt(OffsetDateTime.now());
        ruleRepository.save(rule);

        auditService.logEvent("ADMIN", adminUsername, "RULE_DELETED", null,
                "Regla desactivada: " + safe(rule.getName()));
    }

    // =========================
    // ACTIVAR / DESACTIVAR
    // =========================

    @Transactional
    public void activateRule(Long id) {
        activateRule(id, "ADMIN");
    }

    @Transactional
    public void deactivateRule(Long id) {
        deactivateRule(id, "ADMIN");
    }

    @Transactional
    public void activateRule(Long id, String adminUsername) {
        DerivationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));

        rule.setActive(true);
        rule.setUpdatedAt(OffsetDateTime.now());
        ruleRepository.save(rule);

        auditService.logEvent("ADMIN", adminUsername, "RULE_ACTIVATED", null,
                "Regla activada: " + safe(rule.getName()));
    }

    @Transactional
    public void deactivateRule(Long id, String adminUsername) {
        DerivationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));

        rule.setActive(false);
        rule.setUpdatedAt(OffsetDateTime.now());
        ruleRepository.save(rule);

        auditService.logEvent("ADMIN", adminUsername, "RULE_DEACTIVATED", null,
                "Regla desactivada: " + safe(rule.getName()));
    }

    // =========================
    // MATCHING / DERIVACIÓN
    // =========================

    public Long findDestinationIdForComplaint(Complaint complaint) {
        // Validar que la denuncia tenga severity y complaintType configurados
        if (complaint.getSeverity() == null || complaint.getComplaintType() == null) {
            log.warn("Complaint [{}] missing severity or type, using fallback", complaint.getTrackingId());
            return fallbackDestinationId();
        }

        log.info("=== MATCHING RULES FOR COMPLAINT [{}] ===", complaint.getTrackingId());
        log.info("Complaint severity: {}", complaint.getSeverity());
        log.info("Complaint type: {}", complaint.getComplaintType());

        // Buscar reglas que coincidan con severity y complaintType
        List<DerivationRule> candidates = ruleRepository.findMatchingRules(
                complaint.getSeverity(),      // CRÍTICO: Usar severity (campo que contiene LOW/MEDIUM/HIGH/CRITICAL)
                complaint.getComplaintType()
        );

        log.info("Found {} matching rules", candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            DerivationRule rule = candidates.get(i);
            log.info("  Rule {}: id={}, name='{}', severityMatch='{}', typeMatch='{}', destinationId={}",
                    i + 1, rule.getId(), rule.getName(),
                    rule.getSeverityMatch(), rule.getComplaintTypeMatch(),
                    rule.getDestinationId());
        }

        // Si encontramos reglas, usar la primera (más específica)
        if (!candidates.isEmpty()) {
            DerivationRule selectedRule = candidates.get(0);
            if (selectedRule.isRequiresManualReview()) {
                log.info("Rule [{}] requires manual review for complaint [{}]",
                        selectedRule.getName(), complaint.getTrackingId());
                return null;
            }
            log.info("SELECTED rule [{}] for complaint [{}] -> destination [{}]",
                    selectedRule.getName(), complaint.getTrackingId(), selectedRule.getDestinationId());
            return selectedRule.getDestinationId();
        }

        // Si no hay coincidencias, usar fallback
        log.warn("No matching rules for complaint [{}] (priority={}, type={}), using fallback",
                complaint.getTrackingId(), complaint.getPriority(), complaint.getComplaintType());
        return fallbackDestinationId();
    }

    private DerivationPolicy findEffectivePolicy(LocalDate today) {
        return policyRepository.findByActiveTrueOrderByEffectiveFromDesc()
                .stream()
                .filter(p -> p.getEffectiveFrom() != null)
                .filter(p -> !p.getEffectiveFrom().isAfter(today))
                .filter(p -> p.getEffectiveTo() == null || !p.getEffectiveTo().isBefore(today))
                .findFirst()
                .orElse(null);
    }

    private Long fallbackDestinationId() {
        // Primero intentar encontrar la entidad DEFAULT
        Optional<DestinationEntity> defaultEntity = destinationEntityRepository.findByCode("DEFAULT")
                .filter(DestinationEntity::isActive);

        if (defaultEntity.isPresent()) {
            return defaultEntity.get().getId();
        }

        // Si no hay DEFAULT, usar la primera entidad activa disponible
        return destinationEntityRepository.findAll()
                .stream()
                .filter(DestinationEntity::isActive)
                .findFirst()
                .map(DestinationEntity::getId)
                .orElse(null);
    }

    @Transactional
    public String deriveComplaint(String trackingId, String analystUsername) {
        Complaint complaint = complaintRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new IllegalArgumentException("Denuncia no encontrada"));

        Long destinationId = findDestinationIdForComplaint(complaint);

        if (destinationId == null) {
            complaint.setStatus("REQUIRES_REVIEW");
            complaint.setPriority("MANUAL_REVIEW");
            complaint.setUpdatedAt(OffsetDateTime.now());
            complaintRepository.save(complaint);

            auditService.logEvent("ANALYST", analystUsername, "COMPLAINT_DERIVATION_SKIPPED",
                    trackingId, "No se derivó automáticamente (sin destino o requiere revisión)");

            return null;
        }

        DestinationEntity dest = destinationEntityRepository.findById(destinationId)
                .orElseThrow(() -> new IllegalArgumentException("Entidad destino no encontrada"));

        complaint.setStatus("DERIVED");
        complaint.setDerivedTo(dest.getName());
        complaint.setDerivedAt(OffsetDateTime.now());
        complaint.setUpdatedAt(OffsetDateTime.now());
        complaintRepository.save(complaint);

        auditService.logEvent("ANALYST", analystUsername, "COMPLAINT_DERIVED", trackingId,
                "Derivado a: " + safe(dest.getName()));

        return dest.getName();
    }

    @Transactional
    public void deriveComplaint(Complaint complaint) {
        try {
            Long destinationId = findDestinationIdForComplaint(complaint);
            if (destinationId == null) {
                complaint.setStatus("REQUIRES_REVIEW");
                complaint.setPriority("MANUAL_REVIEW_PENDING");
                complaintRepository.save(complaint);
                return;
            }

            DestinationEntity dest = destinationEntityRepository.findById(destinationId)
                    .orElseThrow(() -> new RuntimeException("Destination not found"));

            complaint.setDerivedTo(dest.getName());
            complaint.setDerivedAt(OffsetDateTime.now());
            complaint.setStatus("DERIVED");
            complaintRepository.save(complaint);

        } catch (Exception e) {
            log.error("Error deriving complaint {}", complaint.getTrackingId(), e);
        }
    }

    // =========================
    // Compat
    // =========================

    public Optional<DerivationRule> findRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    @Transactional
    public void updateRule(DerivationRule rule) {
        rule.setUpdatedAt(OffsetDateTime.now());
        ruleRepository.save(rule);
    }

    private String safe(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > 120 ? t.substring(0, 120) + "..." : t;
    }
}
