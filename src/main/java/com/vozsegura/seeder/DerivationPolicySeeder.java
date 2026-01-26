package com.vozsegura.seeder;

import com.vozsegura.domain.entity.DerivationPolicy;
import com.vozsegura.repo.DerivationPolicyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Seeder para crear una política de derivación por defecto si no existe ninguna.
 *
 * Responsabilidades:
 * - Verificar si existe al menos una política activa
 * - Crear una política por defecto "Política General" si no existe ninguna
 * - Permitir que el sistema funcione sin necesidad de configuración manual inicial
 *
 * Seguridad:
 * - Solo se ejecuta en perfiles dev y default
 * - La política creada tiene vigencia indefinida (effectiveTo = null)
 * - Se marca como activa por defecto
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Component
@Profile({"dev", "default"})
@Order(10) // Ejecutar después de DataSeeder
public class DerivationPolicySeeder implements CommandLineRunner {

    private final DerivationPolicyRepository policyRepository;

    public DerivationPolicySeeder(DerivationPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            List<DerivationPolicy> activePolicies = policyRepository.findByActiveTrueOrderByEffectiveFromDesc();

            if (activePolicies.isEmpty()) {
                log.info("[POLICY_SEEDER] No active policies found. Creating default policy...");

                DerivationPolicy defaultPolicy = new DerivationPolicy();
                defaultPolicy.setName("Política General de Derivación");
                defaultPolicy.setVersion("1.0");
                defaultPolicy.setLegalFramework("Marco normativo general establecido por la institución para la derivación de denuncias.");
                defaultPolicy.setEffectiveFrom(LocalDate.now());
                defaultPolicy.setEffectiveTo(null); // Sin fecha de finalización
                defaultPolicy.setActive(true);
                defaultPolicy.setCreatedBy(null); // Sistema
                defaultPolicy.setApprovedBy(null); // Auto-aprobada
                defaultPolicy.setApprovedAt(OffsetDateTime.now());

                policyRepository.save(defaultPolicy);

                log.info("[POLICY_SEEDER] Default policy created successfully (version: 1.0)");
            } else {
                log.info("[POLICY_SEEDER] Active policies already exist ({} found). Skipping default creation.", activePolicies.size());
            }

        } catch (Exception e) {
            log.error("[POLICY_SEEDER] Error creating default policy", e);
        }
    }
}
