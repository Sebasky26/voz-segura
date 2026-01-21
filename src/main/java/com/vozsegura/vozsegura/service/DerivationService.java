package com.vozsegura.vozsegura.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.domain.entity.DerivationRule;
import com.vozsegura.vozsegura.domain.entity.DestinationEntity;
import com.vozsegura.vozsegura.repo.ComplaintRepository;
import com.vozsegura.vozsegura.repo.DerivationRuleRepository;
import com.vozsegura.vozsegura.repo.DestinationEntityRepository;

/**
 * Servicio para gestionar reglas de derivación automática y derivar denuncias.
 * 
 * Responsabilidades:
 * - Mantener reglas de derivación configuradas por admin
 * - Buscar regla coincidente para cada denuncia basada en severidad+prioridad
 * - Derivar denuncias automáticamente a entidades externas (OIJ, CONAMUSIDA, etc.)
 * - Registrar todas las derivaciones en auditoría
 * - Permitir activación/desactivación de reglas sin eliminar histórico
 * 
 * Algoritmo de Matching (findDestinationIdForComplaint):
 * 1. Recibir denuncia con severidad (LOW/MEDIUM/HIGH/CRITICAL) y prioridad (1-5)
 * 2. Buscar DerivationRule activa donde:
 *    - severityMatch coincida (o sea wildcard "*")
 *    - priorityMatch coincida (o sea rango)
 * 3. Retornar ID de destino (DestinationEntity)
 * 4. Si no hay coincidencia → retornar destino por defecto (ID 1)
 * 
 * Entidades Relacionadas:
 * - DerivationRule: Pareja (severidad+prioridad) → entidad destino
 * - DestinationEntity: Entidades externas (OIJ, CONAMUSIDA, etc.)
 * - Complaint: Denuncia a derivar
 * 
 * Estados de Derivación:
 * - PENDING: Denuncia creada, sin derivar aún
 * - DERIVED: Derivada a entidad externa (cambio de estado + timestamp)
 * 
 * Auditoría:
 * - RULE_CREATED: Admin crea nueva regla
 * - RULE_UPDATED: Admin modifica regla existente
 * - RULE_DELETED: Admin desactiva regla (soft delete)
 * - COMPLAINT_DERIVED: Analyst o sistema derivan denuncia
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * @see DerivationRule
 * @see DestinationEntity
 * @see Complaint
 */
@Service
public class DerivationService {

    private final DerivationRuleRepository ruleRepository;
    private final ComplaintRepository complaintRepository;
    private final DestinationEntityRepository destinationEntityRepository;
    private final AuditService auditService;

    public DerivationService(DerivationRuleRepository ruleRepository,
                             ComplaintRepository complaintRepository,
                             DestinationEntityRepository destinationEntityRepository,
                             AuditService auditService) {
        this.ruleRepository = ruleRepository;
        this.complaintRepository = complaintRepository;
        this.destinationEntityRepository = destinationEntityRepository;
        this.auditService = auditService;
    }

    /**
     * Obtiene todas las reglas de derivación (activas e inactivas).
     */
    public List<DerivationRule> findAllRules() {
        return ruleRepository.findAllByOrderByNameAsc();
    }

    /**
     * Obtiene solo las reglas de derivación activas.
     */
    public List<DerivationRule> findActiveRules() {
        return ruleRepository.findByActiveTrue();
    }

    /**
     * Obtiene una regla de derivación por su ID.
     */
    public Optional<DerivationRule> findById(Long id) {
        return ruleRepository.findById(id);
    }

    /**
     * Crea una nueva regla de derivación.
     */
    @Transactional
    public DerivationRule createRule(DerivationRule rule, String adminUsername) {
        rule.setCreatedAt(OffsetDateTime.now());
        rule.setUpdatedAt(OffsetDateTime.now());
        DerivationRule saved = ruleRepository.save(rule);
        auditService.logEvent("ADMIN", adminUsername, "RULE_CREATED", null,
                "Regla creada: " + rule.getName());
        return saved;
    }

    /**
     * Modifica una regla de derivación existente.
     */
    @Transactional
    public DerivationRule updateRule(Long id, DerivationRule updated, String adminUsername) {
        return ruleRepository.findById(id).map(rule -> {
            rule.setName(updated.getName());
            rule.setSeverityMatch(updated.getSeverityMatch());
            rule.setComplaintTypeMatch(updated.getComplaintTypeMatch());
            rule.setDestinationId(updated.getDestinationId());
            rule.setDescription(updated.getDescription());
            rule.setActive(updated.isActive());
            rule.setUpdatedAt(OffsetDateTime.now());
            DerivationRule saved = ruleRepository.save(rule);
            auditService.logEvent("ADMIN", adminUsername, "RULE_UPDATED", null,
                    "Regla actualizada: " + rule.getName());
            return saved;
        }).orElseThrow(() -> new IllegalArgumentException("Regla no encontrada"));
    }

    /**
     * Desactiva una regla de derivación (soft delete).
     */
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
     * Reactiva una regla de derivación previamente desactivada.
     */
    @Transactional
    public void activateRule(Long id, String adminUsername) {
        ruleRepository.findById(id).ifPresent(rule -> {
            rule.setActive(true);
            rule.setUpdatedAt(OffsetDateTime.now());
            ruleRepository.save(rule);
            auditService.logEvent("ADMIN", adminUsername, "RULE_UPDATED", null,
                    "Regla activada: " + rule.getName());
        });
    }

    /**
     * Encuentra el ID de la entidad de destino para una denuncia basándose en las reglas activas.
     * CRÍTICO: Ahora matchea por severity Y complaintType para derivación precisa
     */
    public Long findDestinationIdForComplaint(Complaint complaint) {
        List<DerivationRule> matchingRules = ruleRepository.findMatchingRules(
                complaint.getSeverity(),
                complaint.getComplaintType() // Ahora incluye tipo de denuncia
        );

        if (matchingRules.isEmpty() || matchingRules.get(0).getDestinationId() == null) {
            // Retornar ID por defecto si no hay regla que coincida
            return 1L;
        }

        return matchingRules.get(0).getDestinationId();
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
        Long destinationId = findDestinationIdForComplaint(complaint);
        
        // Obtener el nombre de la entidad de destino para audit
        Optional<DestinationEntity> destEntity = destinationEntityRepository.findById(destinationId);
        String destinationName = destEntity.map(DestinationEntity::getName).orElse("Destino desconocido");

        complaint.setStatus("DERIVED");
        complaint.setDerivedTo(destinationName);
        complaint.setDerivedAt(OffsetDateTime.now());
        complaint.setUpdatedAt(OffsetDateTime.now());
        complaintRepository.save(complaint);

        auditService.logEvent("ANALYST", analystUsername, "COMPLAINT_DERIVED", trackingId,
                "Derivado a: " + destinationName);

        return destinationName;
    }
}

