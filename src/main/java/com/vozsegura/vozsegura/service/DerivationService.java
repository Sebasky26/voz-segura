package com.vozsegura.vozsegura.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.domain.entity.DerivationRule;
import com.vozsegura.vozsegura.repo.ComplaintRepository;
import com.vozsegura.vozsegura.repo.DerivationRuleRepository;

/**
 * Servicio para gestionar reglas de derivación y derivar denuncias automáticamente.
 */
@Service
public class DerivationService {

    private final DerivationRuleRepository ruleRepository;
    private final ComplaintRepository complaintRepository;
    private final AuditService auditService;

    public DerivationService(DerivationRuleRepository ruleRepository,
                             ComplaintRepository complaintRepository,
                             AuditService auditService) {
        this.ruleRepository = ruleRepository;
        this.complaintRepository = complaintRepository;
        this.auditService = auditService;
    }

    public List<DerivationRule> findAllRules() {
        return ruleRepository.findAllByOrderByNameAsc();
    }

    public List<DerivationRule> findActiveRules() {
        return ruleRepository.findByActiveTrue();
    }

    public Optional<DerivationRule> findById(Long id) {
        return ruleRepository.findById(id);
    }

    @Transactional
    public DerivationRule createRule(DerivationRule rule, String adminUsername) {
        rule.setCreatedAt(OffsetDateTime.now());
        rule.setUpdatedAt(OffsetDateTime.now());
        DerivationRule saved = ruleRepository.save(rule);
        auditService.logEvent("ADMIN", adminUsername, "RULE_CREATED", null,
                "Regla creada: " + rule.getName());
        return saved;
    }

    @Transactional
    public DerivationRule updateRule(Long id, DerivationRule updated, String adminUsername) {
        return ruleRepository.findById(id).map(rule -> {
            rule.setName(updated.getName());
            rule.setComplaintTypeMatch(updated.getComplaintTypeMatch());
            rule.setSeverityMatch(updated.getSeverityMatch());
            rule.setPriorityMatch(updated.getPriorityMatch());
            rule.setDestination(updated.getDestination());
            rule.setDescription(updated.getDescription());
            rule.setActive(updated.isActive());
            rule.setUpdatedAt(OffsetDateTime.now());
            DerivationRule saved = ruleRepository.save(rule);
            auditService.logEvent("ADMIN", adminUsername, "RULE_UPDATED", null,
                    "Regla actualizada: " + rule.getName());
            return saved;
        }).orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));
    }

    @Transactional
    public void deleteRule(Long id, String adminUsername) {
        ruleRepository.findById(id).ifPresent(rule -> {
            rule.setActive(false);
            rule.setUpdatedAt(OffsetDateTime.now());
            ruleRepository.save(rule);
            auditService.logEvent("ADMIN", adminUsername, "RULE_DELETED", null,
                    "Regla desactivada: " + rule.getName());
        });
    }

    /**
     * Encuentra la entidad de destino para una denuncia basándose en las reglas activas.
     */
    public String findDestinationForComplaint(Complaint complaint) {
        List<DerivationRule> matchingRules = ruleRepository.findMatchingRules(
                complaint.getComplaintType(),
                complaint.getSeverity(),
                complaint.getPriority()
        );

        if (matchingRules.isEmpty()) {
            return "Ministerio del Trabajo del Ecuador";
        }

        return matchingRules.get(0).getDestination();
    }

    /**
     * Deriva una denuncia automáticamente basándose en las reglas configuradas.
     */
    @Transactional
    public String deriveComplaint(String trackingId, String analystUsername) {
        Optional<Complaint> complaintOpt = complaintRepository.findByTrackingId(trackingId);

        if (complaintOpt.isEmpty()) {
            throw new IllegalArgumentException("Denuncia no encontrada");
        }

        Complaint complaint = complaintOpt.get();
        String destination = findDestinationForComplaint(complaint);

        complaint.setStatus("DERIVED");
        complaint.setDerivedTo(destination);
        complaint.setDerivedAt(OffsetDateTime.now());
        complaint.setUpdatedAt(OffsetDateTime.now());
        complaintRepository.save(complaint);

        auditService.logEvent("ANALYST", analystUsername, "COMPLAINT_DERIVED", trackingId,
                "Derivado a: " + destination);

        return destination;
    }
}

