package com.vozsegura.vozsegura.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vozsegura.vozsegura.domain.entity.DerivationRule;

@Repository
public interface DerivationRuleRepository extends JpaRepository<DerivationRule, Long> {

    List<DerivationRule> findByActiveTrue();

    List<DerivationRule> findAllByOrderByNameAsc();

    /**
     * Busca la regla de derivación más específica que coincida con los criterios.
     */
    @Query("SELECT r FROM DerivationRule r WHERE r.active = true " +
           "AND (r.severityMatch = :severity OR r.severityMatch IS NULL) " +
           "AND (r.priorityMatch = :priority OR r.priorityMatch IS NULL) " +
           "ORDER BY " +
           "CASE WHEN r.severityMatch IS NOT NULL THEN 1 ELSE 0 END + " +
           "CASE WHEN r.priorityMatch IS NOT NULL THEN 1 ELSE 0 END DESC")
    List<DerivationRule> findMatchingRules(
            @Param("severity") String severity,
            @Param("priority") String priority);

    Optional<DerivationRule> findByName(String name);
}
