package com.vozsegura.repo;

import com.vozsegura.domain.entity.DerivationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DerivationPolicyRepository extends JpaRepository<DerivationPolicy, Long> {

    /**
     * Encuentra todas las politicas activas.
     */
    List<DerivationPolicy> findByActiveTrueOrderByEffectiveFromDesc();

    /**
     * Encuentra politica por version.
     */
    Optional<DerivationPolicy> findByVersion(String version);

    /**
     * Encuentra politicas vigentes en una fecha especifica.
     * OJO: agrego ORDER BY para que el resultado sea estable.
     */
    @Query("SELECT p FROM DerivationPolicy p WHERE p.active = true " +
            "AND p.effectiveFrom <= :date " +
            "AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date) " +
            "ORDER BY p.effectiveFrom DESC, p.id DESC")
    List<DerivationPolicy> findEffectiveOn(LocalDate date);

    /**
     * Encuentra la política vigente más reciente (la que debe usar el motor).
     * Esto evita depender de policies.get(0) sin orden garantizado.
     */
    @Query("SELECT p FROM DerivationPolicy p WHERE p.active = true " +
            "AND p.effectiveFrom <= :date " +
            "AND (p.effectiveTo IS NULL OR p.effectiveTo >= :date) " +
            "ORDER BY p.effectiveFrom DESC, p.id DESC")
    Optional<DerivationPolicy> findTopEffectiveOn(LocalDate date);

    /**
     * Encuentra todas las politicas ordenadas por fecha de creacion.
     */
    List<DerivationPolicy> findAllByOrderByCreatedAtDesc();
}
