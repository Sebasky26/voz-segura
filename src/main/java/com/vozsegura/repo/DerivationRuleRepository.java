package com.vozsegura.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vozsegura.domain.entity.DerivationRule;

@Repository
public interface DerivationRuleRepository extends JpaRepository<DerivationRule, Long> {

    // =========================
    // Básicos
    // =========================

    List<DerivationRule> findByActiveTrue();

    List<DerivationRule> findAllByOrderByNameAsc();

    Optional<DerivationRule> findByName(String name);

    List<DerivationRule> findByPolicyIdAndActiveTrueOrderByPriorityOrderAsc(Long policyId);

    // =========================
    // Para la pantalla /admin/reglas (evita N/A por LAZY)
    // =========================

    /**
     * Trae todas las reglas junto a su entidad destino (fetch join),
     * para que Thymeleaf pueda mostrar rule.destinationEntity.name sin LazyInitialization.
     */
    @Query("select r from DerivationRule r " +
            "left join fetch r.destinationEntity " +
            "order by r.name asc")
    List<DerivationRule> findAllWithDestinationOrderByNameAsc();

    /**
     * Trae reglas de una política con destino ya cargado (útil si filtras por policy).
     */
    @Query("select r from DerivationRule r " +
            "left join fetch r.destinationEntity " +
            "where r.policyId = :policyId " +
            "order by r.priorityOrder asc, r.id asc")
    List<DerivationRule> findByPolicyIdWithDestinationOrderByPriority(
            @Param("policyId") Long policyId
    );

    // =========================
    // Matching (mejorado)
    // =========================

    /**
     * Matching respetando policy y priority_order.
     * - Solo reglas activas de esa policy
     * - Match flexible por severidad y tipo (NULL = comodín)
     * - Ordena por:
     *   1) priorityOrder ASC (lo más importante)
     *   2) especificidad DESC (más específica gana si misma prioridad)
     *   3) id ASC (estable)
     */
    @Query("select r from DerivationRule r " +
            "where r.active = true and r.policyId = :policyId " +
            "and (r.severityMatch = :severity or r.severityMatch is null) " +
            "and (r.complaintTypeMatch = :complaintType or r.complaintTypeMatch is null) " +
            "order by r.priorityOrder asc, " +
            "case when r.severityMatch is not null and r.complaintTypeMatch is not null then 3 " +
            "     when r.severityMatch is not null then 2 " +
            "     when r.complaintTypeMatch is not null then 1 " +
            "     else 0 end desc, " +
            "r.id asc")
    List<DerivationRule> findMatchingRulesForPolicy(
            @Param("policyId") Long policyId,
            @Param("severity") String severity,
            @Param("complaintType") String complaintType
    );

    // =========================
    // Mantengo tu método original por compatibilidad (si lo usas en otro lado)
    // Pero OJO: no respeta policy ni priorityOrder.
    // =========================

    @Query("SELECT r FROM DerivationRule r WHERE r.active = true " +
            "AND (r.severityMatch = :severity OR r.severityMatch IS NULL) " +
            "AND (r.complaintTypeMatch = :complaintType OR r.complaintTypeMatch IS NULL) " +
            "ORDER BY " +
            "CASE WHEN r.severityMatch IS NOT NULL AND r.complaintTypeMatch IS NOT NULL THEN 3 " +
            "     WHEN r.severityMatch IS NOT NULL THEN 2 " +
            "     WHEN r.complaintTypeMatch IS NOT NULL THEN 1 " +
            "     ELSE 0 END DESC")
    List<DerivationRule> findMatchingRules(@Param("severity") String severity,
                                           @Param("complaintType") String complaintType);
}
