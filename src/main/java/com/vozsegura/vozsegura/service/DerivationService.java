package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.domain.entity.DerivationRule;
import com.vozsegura.vozsegura.repo.DerivationRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio para gestión de reglas de derivación.
 */
@Service
public class DerivationService {

    private final DerivationRuleRepository derivationRuleRepository;

    public DerivationService(DerivationRuleRepository derivationRuleRepository) {
        this.derivationRuleRepository = derivationRuleRepository;
    }

    /**
     * Lista todas las reglas de derivación.
     */
    public List<DerivationRule> findAllRules() {
        return derivationRuleRepository.findAll();
    }

    /**
     * Lista reglas activas.
     */
    public List<DerivationRule> findActiveRules() {
        return derivationRuleRepository.findByActiveTrue();
    }

    /**
     * Busca una regla por ID.
     */
    public Optional<DerivationRule> findById(Long id) {
        return derivationRuleRepository.findById(id);
    }

    /**
     * Crea o actualiza una regla.
     */
    @Transactional
    public DerivationRule save(DerivationRule rule) {
        return derivationRuleRepository.save(rule);
    }

    /**
     * Desactiva una regla (no se elimina por trazabilidad).
     */
    @Transactional
    public void deactivate(Long id) {
        derivationRuleRepository.findById(id).ifPresent(rule -> {
            rule.setActive(false);
            derivationRuleRepository.save(rule);
        });
    }

    /**
     * Busca destino de derivación según severidad.
     */
    public Optional<String> findDestinationBySeverity(String severity) {
        return derivationRuleRepository.findByActiveTrue().stream()
                .filter(r -> r.getSeverityMatch() == null || r.getSeverityMatch().equalsIgnoreCase(severity))
                .map(DerivationRule::getDestination)
                .findFirst();
    }
}

