package com.vozsegura.vozsegura.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vozsegura.vozsegura.domain.entity.DerivationRule;

/**
 * Spring Data JPA repository para entidad DerivationRule.
 * 
 * Responsabilidad:
 * - Gestionar las reglas de derivación automática de denuncias
 * - Buscar reglas activas basadas en criterios de severidad y tipo de denuncia
 * - Soporte para soft-delete (reglas se marcan como inactivas)
 * 
 * Lógica de Routing:
 * Las denuncias se derivan automáticamente a DestinationEntity (ministerios, etc.)
 * basada en una regla que coincida con:
 * 1. Severidad de la denuncia (HIGH, MEDIUM, LOW)
 * 2. Tipo de denuncia (ACOSO_LABORAL, DISCRIMINACION, etc.)
 *
 * La regla más específica (ambos criterios) se prefiere sobre genérica (uno solo).
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Repository
public interface DerivationRuleRepository extends JpaRepository<DerivationRule, Long> {

    /**
     * Busca todas las reglas de derivación activas.
     * Usado para obtener el conjunto completo de reglas en vigencia.
     * 
     * @return Lista de reglas con active=true
     */
    List<DerivationRule> findByActiveTrue();

    /**
     * Obtiene todas las reglas ordenadas alfabéticamente.
     * Útil para pantalla de administración de reglas.
     * 
     * @return Lista de todas las reglas ordenadas por nombre
     */
    List<DerivationRule> findAllByOrderByNameAsc();

    /**
     * Busca la regla de derivación más específica que coincida con la severidad.
     *
     * Algoritmo de Matching:
     * 1. Filtra reglas activas (active=true)
     * 2. Coincide severidad: exacta (if severityMatch presente) O ignora (if NULL)
     * 3. Ordena por especificidad: primero las que tienen severidad específica
     *
     * Ejemplos:
     * - Regla: severity=HIGH → Coincide con denuncias de severidad HIGH
     * - Regla: severity=NULL → Coincide con cualquier severidad (fallback)
     *
     * @param severity Severidad de la denuncia (ej: "HIGH", "CRITICAL")
     * @return Lista de reglas que coinciden, ordenadas por especificidad (MÁS específicas primero)
     */
    @Query("SELECT r FROM DerivationRule r WHERE r.active = true " +
           "AND (r.severityMatch = :severity OR r.severityMatch IS NULL) " +
           "ORDER BY " +
           "CASE WHEN r.severityMatch IS NOT NULL THEN 1 ELSE 0 END DESC")
    List<DerivationRule> findMatchingRules(@Param("severity") String severity);

    /**
     * Busca una regla por su nombre único.
     * 
     * @param name Nombre de la regla
     * @return Optional con regla si existe
     */
    Optional<DerivationRule> findByName(String name);
}
